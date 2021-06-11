/**
 * Copyright (c) 2019,2020 honintech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. * 
 * 
 */
package cn.weforward.protocol.aio.netty.websocket;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import cn.weforward.common.DictionaryExt;
import cn.weforward.common.util.NumberUtil;
import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.common.util.StringUtil;
import cn.weforward.protocol.aio.ClientChannel;
import cn.weforward.protocol.aio.ClientContext;
import cn.weforward.protocol.aio.ClientHandler;
import cn.weforward.protocol.aio.Headers;
import cn.weforward.protocol.aio.ServerBackwardChannel;
import cn.weforward.protocol.aio.ServerContext;
import cn.weforward.protocol.aio.ServerHandler;
import cn.weforward.protocol.aio.http.HttpConstants;
import cn.weforward.protocol.aio.http.QueryStringParser;
import cn.weforward.protocol.aio.netty.HeadersParser;
import cn.weforward.protocol.aio.netty.NettyHttpHeaders;
import cn.weforward.protocol.aio.netty.NettyOutputStream;
import cn.weforward.protocol.aio.netty.websocket.WebSocketMessage.Output;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.util.concurrent.ScheduledFuture;

/**
 * WebSocket多路复用的一次Request/Response模式调用
 * 
 * @author liangyi
 *
 */
public class WebSocketSession {
	final protected String m_Id;
	final protected WebSocketContext m_Websocket;
	protected WebSocketRequest m_Request;
	protected WebSocketResponse m_Response;
	protected ServerHandler m_ServerHandler = ServerHandler._init;
	protected ClientHandler m_ClientHandler = ClientHandler._init;
	protected long m_RequestTimepoint;

	public WebSocketSession(WebSocketContext ctx, String id) {
		m_Websocket = ctx;
		m_Id = id;
	}

	public String getId() {
		return m_Id;
	}

	protected int readable(ByteBuf payload, int packetState) throws IOException {
		int type = packetState & ~WebSocketMessage.PACKET_MARK_FINAL;
		int available = payload.readableBytes();
		if (WebSocketMessage.PACKET_REQUEST == type) {
			// 请求数据包
			if (null == m_Request) {
				m_Request = new WebSocketRequest(this, analyseHead(payload));
				packetState |= WebSocketMessage.PACKET_MARK_HEADER;
				// m_RequestTimepoint = System.currentTimeMillis();
				ServerHandler handler = null;
				try {
					handler = m_Websocket.m_HandlerFactory.handle(new ServerSide());
				} finally {
					if (null == handler) {
						if (isRespond()) {
							// 已经响应
							return WebSocketMessage.PACKET_MARK_ABORT;
						}
						// 没有业务处理
						responseAndClose(WebSocketMessage.STATUS_NOT_IMPLEMENTED);
						return WebSocketMessage.PACKET_MARK_ABORT;
					}
				}
				m_ServerHandler = handler;
				handler.requestHeader();
			}
			m_Request.readable(payload);
			m_ServerHandler.prepared(available);
			if (WebSocketMessage.PACKET_MARK_FINAL == (WebSocketMessage.PACKET_MARK_FINAL & packetState)) {
				m_RequestTimepoint = System.currentTimeMillis();
				m_Request.complete();
//				m_ServerHandler.requestCompleted();
				messageCompleted(m_Request);
			}
			return packetState;
		}
		if (WebSocketMessage.PACKET_RESPONSE == type) {
			// 响应数据包
			if (null == m_Response) {
				m_Response = new WebSocketResponse(this, analyseHead(payload));
				packetState |= WebSocketMessage.PACKET_MARK_HEADER;
				m_ClientHandler.responseHeader();
			}
			m_Response.readable(payload);
			m_ClientHandler.prepared(available);
			if (WebSocketMessage.PACKET_MARK_FINAL == (WebSocketMessage.PACKET_MARK_FINAL & packetState)) {
				m_Response.complete();
//				m_ClientHandler.responseCompleted();
				messageCompleted(m_Response);
			}
			return packetState;
		}
		throw new IOException("包类型异常:" + packetState);
	}

	/**
	 * 分析HTTP头格式
	 * 
	 * @param payload 数据
	 * @return 分析到结果
	 */
	private NettyHttpHeaders analyseHead(ByteBuf payload) throws IOException {
		io.netty.handler.codec.http.HttpHeaders headers;
		HeadersParser parser = HeadersParser._Pool.poll();
		try {
			headers = parser.parseRaw(payload);
			headers = parser.openHeaders(headers);
		} finally {
			HeadersParser._Pool.offer(parser);
		}
		// 加入WS-RPC-ID
		headers.add(WebSocketMessage.HEADER_WS_RPC_ID, getId());
		return new NettyHttpHeaders(headers);
	}

	public void abort() {
		WebSocketRequest req = m_Request;
		WebSocketResponse rsp = m_Response;
		boolean abort = false;
		abort = (null != req && req.abort());
		if (null != rsp) {
			if (rsp.isCompleted()) {
				abort = false;
			}
			rsp.abort();
		}
		if (abort) {
			m_ClientHandler.requestAbort();
			m_ServerHandler.requestAbort();
		}
	}

	public ByteBufAllocator getAllocator() {
		return m_Websocket.getAllocator();
	}

	protected ClientContext openRequest(ClientHandler handler, String uri) throws IOException {
		synchronized (this) {
			if (null != m_Request) {
				throw new IOException("请求已打开");
			}
			m_ClientHandler = handler;
			io.netty.handler.codec.http.HttpHeaders headers = new DefaultHttpHeaders();
			headers.set(WebSocketMessage.HEADER_URI, uri);
//			headers.set(HEADER_WS_RPC_ID, getId());
			m_Request = new WebSocketRequest(this, new NettyHttpHeaders(headers));
		}
		m_RequestTimepoint = System.currentTimeMillis();
		ClientContext context = new ClientSide();
		handler.established(context);
		return context;
	}

	synchronized WebSocketResponse openResponse() throws IOException {
		if (null == m_Request || m_Request.isInvalid()) {
			throw new EOFException("invalid");
		}
		if (null == m_Response) {
			io.netty.handler.codec.http.HttpHeaders headers = new DefaultHttpHeaders();
//			headers.set(HEADER_WS_RPC_ID, getId());
			m_Response = new WebSocketResponse(this, new NettyHttpHeaders(headers));
		} else if (m_Response.isCompleted()) {
			throw new EOFException("已响应");
		}
		return m_Response;
	}

	public boolean isRespond() {
		WebSocketResponse rsp = m_Response;
		return null != rsp && rsp.isCompleted();
	}

	public void responseAndClose(String status) throws IOException {
		WebSocketResponse rsp = openResponse();
		if (null != status) {
			rsp.setHeader(WebSocketMessage.HEADER_STATUS, status);
		}
		rsp.flush(null);
		m_Websocket.close();
	}

	protected void errorTransferTo(WebSocketMessage message, IOException e, ByteBuf data, NettyOutputStream out) {
		if (message == m_Request) {
			m_ClientHandler.errorResponseTransferTo(e, data, out);
			return;
		}
		if (message == m_Response) {
			m_ServerHandler.errorRequestTransferTo(e, data, out);
		}
	}

	void messageCompleted(WebSocketMessage message) {
		if (message == m_Request) {
			m_ServerHandler.requestCompleted();
			return;
		}
		if (message == m_Response) {
			m_Websocket.removeSession(getId());
			m_ClientHandler.responseCompleted();
			return;
		}
	}

	void messageAbort(WebSocketMessage message) {
		if (message == m_Request) {
			// m_ServerHandler.requestAbort();
			m_ClientHandler.requestAbort();
			return;
		}
		if (message == m_Response) {
			m_Websocket.removeSession(getId());
			return;
		}
	}

	public ChannelFuture writeAndFlush(Object content) throws IOException {
		ChannelHandlerContext ctx = m_Websocket.getChannelContext();
		if (null == ctx) {
			throw new EOFException("closed");
		}
		return ctx.writeAndFlush(content);
	}

	public void disconnect() {
//		if (null != m_Context) {
		m_Websocket.close();
//		}
	}

	@Override
	public String toString() {
		return toString(null);
	}

	protected String toString(String msg) {
		StringBuilder builder = StringBuilderPool._8k.poll();
		try {
			if (null != msg) {
				builder.append(msg);
			}
			builder.append("{id:").append(m_Id).append(",ctx:");
			m_Websocket.toString(builder);
			if (null != m_Request) {
				builder.append(",req:");
				m_Request.toString(builder);
			}
			if (null != m_Response) {
				builder.append(",rsp:");
				m_Response.toString(builder);
			}
			builder.append("}");
			return builder.toString();
		} finally {
			StringBuilderPool._8k.offer(builder);
		}
	}

	/**
	 * 响应超时检查
	 */
	abstract class ResponseChecker implements Runnable {
		int m_Timeout;
		ScheduledFuture<?> m_Task;

		protected abstract void onTimeout();

		public void setTimeout(int millis) {
			ScheduledFuture<?> task = m_Task;
			if (null != task) {
				// 先取消上个任务
				m_Task = null;
				task.cancel(false);
			}
			ChannelHandlerContext ctx = m_Websocket.getChannelContext();
			if (null == ctx || millis < 1) {
				return;
			}
			m_Timeout = millis;
			checkTimeout();
		}

		/**
		 * 响应超时检查
		 */
		protected void checkTimeout() {
			long remaind;
			try {
				int timeout = m_Timeout;
				if (timeout <= 0) {
					// 没指定超时值，略过
					return;
				}
				remaind = timeout - (System.currentTimeMillis() - m_RequestTimepoint);
				if (remaind <= 0) {
					// 超时了
					if (WebSocketContext._Logger.isDebugEnabled()) {
						WebSocketContext._Logger.debug(WebSocketSession.this.toString("timeout"));
					}
					onTimeout();
					return;
				}
				// 还没到时间，要再等等
			} finally {
				m_Task = null;
			}
			// N毫秒后再检查
			if (remaind > 0) {
				ChannelHandlerContext ctx = m_Websocket.getChannelContext();
				if (null != ctx) {
					m_Task = ctx.executor().schedule(this, remaind, TimeUnit.MILLISECONDS);
				}
			}
		}

		@Override
		public void run() {
			checkTimeout();
		}
	}

	/**
	 * 对ServerContext支持
	 */
	class ServerSide extends ResponseChecker implements ServerContext, ServerBackwardChannel, Runnable {
		String m_Uri;
		String m_QueryString;
		DictionaryExt<String, String> m_Params;

		@Override
		public String getUri() {
			String uri = m_Uri;
			if (null == uri) {
				uri = m_Request.getHeaders().get(WebSocketMessage.HEADER_URI);
				if (null == uri) {
					uri = "";
				}
				int idx = uri.indexOf('?');
				if (idx >= 0) {
					m_QueryString = uri.substring(idx + 1);
					uri = uri.substring(0, idx);
				} else {
					m_QueryString = "";
				}
				m_Uri = uri;
			}
			return uri;
		}

		@Override
		public String getRemoteAddr() {
			return m_Websocket.getRemoteAddr();
		}

		@Override
		public String getVerb() {
			String verb = m_Request.getHeaders().get(WebSocketMessage.HEADER_VERB);
			if (null == verb) {
				verb = HttpConstants.METHOD_POST;
			}
			return verb;
		}

		@SuppressWarnings("unchecked")
		@Override
		public DictionaryExt<String, String> getParams() {
			if (null != m_Params) {
				return m_Params;
			}
			getUri();
			String queryString = m_QueryString;
			if (null == queryString || 0 == queryString.length()) {
				m_Params = (DictionaryExt<String, String>) DictionaryExt._Empty;
			} else {
				QueryStringParser parser = QueryStringParser._Pool.poll();
				try {
					m_Params = new DictionaryExt.WrapMap<String, String>(
							parser.parse(queryString, 0, QueryStringParser.UTF_8, 50));
				} finally {
					QueryStringParser._Pool.offer(parser);
				}
			}
			return m_Params;
		}

		@Override
		public Headers getRequestHeaders() {
			return m_Request.getHeaders();
		}

		@Override
		public void requestTransferTo(OutputStream writer, int skipBytes) throws IOException {
			m_Request.transferTo(writer, skipBytes);
		}

		@Override
		public InputStream getRequestStream() throws IOException {
			return m_Request.getStream();
		}

		@Override
		public boolean isRequestCompleted() {
			return null != m_Request && m_Request.isCompleted();
		}

		@Override
		public void setResponseTimeout(int millis) {
			setTimeout(millis);
		}

		@Override
		public void setResponseHeader(String name, String value) throws IOException {
			openResponse().setHeader(name, value);
		}

		@Override
		public OutputStream openResponseWriter(int statusCode, String reasonPhrase) throws IOException {
			WebSocketResponse rsp = openResponse();
			if (statusCode > 0) {
				String status;
				StringBuilder builder = StringBuilderPool._128.poll();
				try {
					builder.append(statusCode);
					if (!StringUtil.isEmpty(reasonPhrase)) {
						builder.append(' ').append(reasonPhrase);
					}
					status = builder.toString();
				} finally {
					StringBuilderPool._128.offer(builder);
				}
				rsp.setHeader(WebSocketMessage.HEADER_STATUS, status);
			}
			return rsp.openWriter();
		}

		@Override
		public void response(int statusCode, byte[] content) throws IOException {
			if (ServerContext.RESPONSE_AND_CLOSE == content) {
				responseAndClose(String.valueOf(statusCode));
				return;
			}
			WebSocketResponse rsp = openResponse();
			rsp.setHeader(WebSocketMessage.HEADER_STATUS, String.valueOf(statusCode));
			Output out = rsp.openWriter();
			if (null != content) {
				out.write(content);
			}
			out.close();
		}

		@Override
		public boolean isRespond() {
			return WebSocketSession.this.isRespond();
		}

		@Override
		public void disconnect() {
			WebSocketSession.this.disconnect();
		}

		@Override
		public String toString() {
			return WebSocketSession.this.toString();
		}

		@Override
		protected void onTimeout() {
			m_ServerHandler.responseTimeout();
		}

		@Override
		public ClientChannel getClientChannel() {
			return m_Websocket;
		}
	}

	/**
	 * 对ClientContext支持
	 */
	class ClientSide extends ResponseChecker implements ClientContext, Runnable {
		@Override
		public void setRequestHeader(String name, String value) throws IOException {
			m_Request.setHeader(name, value);
		}

		@Override
		public OutputStream openRequestWriter() throws IOException {
			return m_Request.openWriter();
		}

		@Override
		public Headers getResponseHeaders() throws IOException {
			return m_Response.getHeaders();
		}

		@Override
		public InputStream getResponseStream() throws IOException {
			return m_Response.getStream();
		}

		@Override
		public void responseTransferTo(OutputStream writer, int skipBytes) throws IOException {
			m_Response.transferTo(writer, skipBytes);
		}

		@Override
		public boolean isResponseCompleted() {
			return null != m_Response && m_Response.isCompleted();
		}

		@Override
		public void disconnect() {
			WebSocketSession.this.disconnect();
		}

		@Override
		public String toString() {
			return WebSocketSession.this.toString();
		}

		@Override
		protected void onTimeout() {
			m_ClientHandler.responseTimeout();
		}

		@Override
		public int getResponseCode() throws IOException {
			return NumberUtil.toInt(getResponseHeaders().get(WebSocketMessage.HEADER_STATUS), 0);
		}
	}
}
