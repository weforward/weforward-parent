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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import cn.weforward.protocol.aio.ClientContext;
import cn.weforward.protocol.aio.ClientHandler;
import cn.weforward.protocol.aio.Headers;
import cn.weforward.protocol.aio.ServerContext;
import cn.weforward.protocol.aio.ServerHandler;
import cn.weforward.protocol.aio.netty.HeadersParser;
import cn.weforward.protocol.aio.netty.NettyHttpHeaders;
import cn.weforward.protocol.aio.netty.NettyOutputStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.ScheduledFuture;

/**
 * WebSocket多路复用的一次Request/Response模式调用
 * 
 * @author liangyi
 *
 */
public class WebSocketSession {
	/** 调用请求的数据包 */
	public final static int PACKET_REQUEST = 0x01;
	/** 调用响应的数据包 */
	public final static int PACKET_RESPONSE = 0x02;
	/** 最后的数据包 */
	public final static int PACKET_MARK_FINAL = 0x10;
	/** 已初始化header */
	public final static int PACKET_MARK_HEADER = 0x20;

	/** 请求包前导码 */
	public final static int PACKET_PREAMBLE_REQUEST = 'P';
	/** 响应包前导码 */
	public final static int PACKET_PREAMBLE_RESPONSE = 'R';
	/** 消息包分隔符 */
	public final static char PACKET_LF = '\n';
	/** 消息包ID最大长度 */
	public final static int PACKET_ID_LENGTH = 128;

	public final static String HEADER_URI = "URI";
	public final static String HEADER_WS_RPC_ID = "WS-RPC-ID";

	protected String m_Id;
	protected WebSocketContext m_Context;
	protected WebSocketRequest m_Request;
	protected WebSocketResponse m_Response;
	protected HeadersParser m_HeadersParser;
	protected ServerHandler m_ServerHandler = ServerHandler._init;
	protected ClientHandler m_ClientHandler = ClientHandler._init;
	protected long m_RequestTimepoint;

	public WebSocketSession(WebSocketContext ctx, String id) {
		m_Context = ctx;
		m_Id = id;
	}

	public String getId() {
		return m_Id;
	}

	protected int readable(ByteBuf payload, int packetState) throws IOException {
		int type = packetState & ~PACKET_MARK_FINAL;
		int available = payload.readableBytes();
		if (PACKET_REQUEST == type) {
			if (null == m_Request) {
				m_Request = new WebSocketRequest(this, analyseHead(payload));
				packetState |= PACKET_MARK_HEADER;
				// m_RequestTimepoint = System.currentTimeMillis();
			}
			m_Request.readable(payload);
			m_ServerHandler.prepared(available);
			if (PACKET_MARK_FINAL == (PACKET_MARK_FINAL & packetState)) {
				m_RequestTimepoint = System.currentTimeMillis();
				m_Request.complete();
				m_ServerHandler.requestCompleted();
			}
			return packetState;
		}
		if (PACKET_RESPONSE == type) {
			if (null == m_Response) {
				m_Response = new WebSocketResponse(this, analyseHead(payload));
				packetState |= PACKET_MARK_HEADER;
			}
			m_Response.readable(payload);
			m_ClientHandler.prepared(available);
			if (PACKET_MARK_FINAL == (PACKET_MARK_FINAL & packetState)) {
				m_Response.complete();
				m_ClientHandler.responseCompleted();
			}
			return packetState;
		}
		throw new IOException("包类型异常:" + packetState);
	}

	/**
	 * 分析HTTP头格式
	 * 
	 * @param payload
	 *            数据
	 * @return 分析到结果
	 */
	private NettyHttpHeaders analyseHead(ByteBuf payload) throws IOException {
		if (null == m_HeadersParser) {
			m_HeadersParser = new HeadersParser();
		}
		io.netty.handler.codec.http.HttpHeaders headers = m_HeadersParser.parseRaw(payload);
		m_HeadersParser.reset();
		// 加入WS-RPC-ID
		headers = m_HeadersParser.openHeaders(headers);
		headers.add(HEADER_WS_RPC_ID, getId());
		return new NettyHttpHeaders(headers);
	}

	public void close() {
		WebSocketRequest req = m_Request;
		WebSocketResponse rsp = m_Response;
		if (null != req) {
			req.abort();
		}
		if (null != rsp) {
			rsp.abort();
		}
	}

	protected CompositeByteBuf compositeBuffer() {
		return m_Context.compositeBuffer();
	}

	public ServerContext getServerContext() {
		return new ServerSide();
	}

	public ClientContext getClientContext() {
		return new ClientSide();
	}

	protected void openRequest(ClientHandler handler, String uri) throws IOException {
		synchronized (this) {
			if (null != m_Request) {
				throw new IOException("请求已打开");
			}
			m_ClientHandler = handler;
			io.netty.handler.codec.http.HttpHeaders headers = new DefaultHttpHeaders();
			headers.set(HEADER_URI, uri);
			headers.set(HEADER_WS_RPC_ID, getId());
			m_Request = new WebSocketRequest(this, new NettyHttpHeaders(headers));
		}
		handler.established();
	}

	synchronized WebSocketResponse openResponse() {
		if (null == m_Response) {
			io.netty.handler.codec.http.HttpHeaders headers = new DefaultHttpHeaders();
			// headers.set(HEADER_WS_RPC_ID, getId());
			m_Response = new WebSocketResponse(this, new NettyHttpHeaders(headers));
		}
		return m_Response;
	}

	public boolean isRespond() {
		WebSocketResponse rsp = m_Response;
		return null != rsp && rsp.isCompleted();
	}

	public void response(HttpResponseStatus status) {
		// TODO Auto-generated method stub

	}

	protected void onRequest(ServerHandler handler) {
		m_ServerHandler = handler;
		handler.requestHeader();
	}

	class ServerSide implements ServerContext, Runnable {
		int m_ResponseTimeout;
		ScheduledFuture<?> m_ResponseTimeoutTask;

		@Override
		public String getUri() {
			return m_Request.getHeaders().get(HEADER_URI);
		}

		@Override
		public String getRemoteAddr() {
			return m_Context.getRemoteAddr();
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
		public void setResponseTimeout(int millis) {
			ScheduledFuture<?> task = m_ResponseTimeoutTask;
			if (null != task) {
				// 先取消上个任务
				m_ResponseTimeoutTask = null;
				task.cancel(false);
			}
			// if (Integer.MAX_VALUE == millis) {
			// // 使用空闲超时值
			// millis = m_Context.getIdleMillis();
			// }
			ChannelHandlerContext ctx = m_Context.m_Ctx;
			if (null == ctx || millis < 1) {
				return;
			}
			m_ResponseTimeout = millis;
			m_ResponseTimeoutTask = ctx.executor().schedule(this, millis, TimeUnit.MILLISECONDS);
		}

		@Override
		public void setResponseHeader(String name, String value) throws IOException {
			openResponse().setHeader(name, value);
		}

		@Override
		public OutputStream openResponseWriter() throws IOException {
			return openResponse().openWriter();
		}

		@Override
		public void run() {
			checkTimeout();
		}

		/**
		 * 响应超时检查
		 */
		private void checkTimeout() {
			long remaind;
			try {
				int timeout = m_ResponseTimeout;
				if (timeout <= 0) {
					// 超时值为，略过
					return;
				}
				remaind = timeout - System.currentTimeMillis() - m_RequestTimepoint;
				if (remaind <= 0) {
					// 超时了
					m_ServerHandler.responseTimeout();
					return;
				}
				// 还没到时间，要再等等
			} finally {
				m_ResponseTimeoutTask = null;
			}
			// N毫秒后再检查
			if (remaind > 0) {
				ChannelHandlerContext ctx = m_Context.m_Ctx;
				if (null != ctx) {
					m_ResponseTimeoutTask = ctx.executor().schedule(this, remaind,
							TimeUnit.MILLISECONDS);
				}
			}
		}
	}

	class ClientSide implements ClientContext {

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
	}

	protected void errorTransferTo(WebSocketMessage message, IOException e, ByteBuf data,
			NettyOutputStream out) {
		if (message == m_Request) {
			m_ClientHandler.errorResponseTransferTo(e, data, out);
			return;
		}
		if (message == m_Response) {
			m_ServerHandler.errorRequestTransferTo(e, data, out);
		}
	}

	public ChannelFuture writeAndFlush(Object content) {
		return m_Context.m_Ctx.writeAndFlush(content);
	}

	public void disconnect() {
		if (null != m_Context) {
			m_Context.close();
		}
	}
}
