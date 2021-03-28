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

import cn.weforward.protocol.aio.ClientContext;
import cn.weforward.protocol.aio.ClientHandler;
import cn.weforward.protocol.aio.Headers;
import cn.weforward.protocol.aio.ServerContext;
import cn.weforward.protocol.aio.ServerHandler;
import cn.weforward.protocol.aio.http.HttpHeaders;
import cn.weforward.protocol.aio.netty.HeadersParser;
import cn.weforward.protocol.aio.netty.NettyHttpHeaders;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

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

	public final static String HEADER_URI = "URI";
	public final static String HEADER_WS_RPC_ID = "WS-RPC-ID";

	protected String m_Id;
	protected WebSocketContext m_Context;
	protected WebSocketRequest m_Request;
	protected WebSocketResponse m_Response;
	protected HeadersParser m_HeadersParser;

	ServerHandler m_ServerHandler = ServerHandler._init;
	ClientHandler m_ClientHandler = ClientHandler._init;

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
			}
			m_Request.readable(payload);
			m_ServerHandler.prepared(available);
			if (PACKET_MARK_FINAL == (PACKET_MARK_FINAL & packetState)) {
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
	private HttpHeaders analyseHead(ByteBuf payload) throws IOException {
		if (null == m_HeadersParser) {
			m_HeadersParser = new HeadersParser(8192);
		}
		io.netty.handler.codec.http.HttpHeaders headers = m_HeadersParser.parseRaw(payload);
		m_HeadersParser.reset();
		// 加入WS-RPC-ID
		headers = m_HeadersParser.openHeaders(headers);
		headers.add(HEADER_WS_RPC_ID, getId());
		return NettyHttpHeaders.valueOf(headers);
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
		// TODO Auto-generated method stub
		return null;
	}

	public ClientContext getClientContext() {
		// TODO Auto-generated method stub
		return null;
	}

	protected void openRequest(ClientHandler handler, String uri) {
		// TODO Auto-generated method stub
		m_ClientHandler = handler;
		io.netty.handler.codec.http.HttpHeaders headers = new DefaultHttpHeaders();
		headers.set(HEADER_URI, uri);
		headers.set(HEADER_WS_RPC_ID, getId());
		// headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		// headers.set(HttpHeaderNames.USER_AGENT, "netty/4.1");
		// headers.set(HttpHeaderNames.ACCEPT, "*/*");
		// headers.set(HttpHeaderNames.ACCEPT_CHARSET, "UTF-8");
		m_Request = new WebSocketRequest(this, new NettyHttpHeaders(headers));
		handler.established();
	}

	public boolean isRespond() {
		// TODO Auto-generated method stub
		return false;
	}

	public void response(HttpResponseStatus status) {
		// TODO Auto-generated method stub

	}

	protected void onRequest(ServerHandler handler) {
		m_ServerHandler = handler;
		handler.requestHeader();
	}

	class ServerSide implements ServerContext {

		@Override
		public String getUri() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getRemoteAddr() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Headers getRequestHeaders() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void requestTransferTo(OutputStream writer, int skipBytes) throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		public InputStream getRequestStream() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setResponseTimeout(int millis) {
			// TODO Auto-generated method stub

		}

		@Override
		public void setResponseHeader(String name, String value) throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		public OutputStream openResponseWriter() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

	}

	class ClientSide implements ClientContext {

		@Override
		public void setRequestHeader(String name, String value) throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		public OutputStream openRequestWriter() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Headers getResponseHeaders() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public InputStream getResponseStream() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void responseTransferTo(OutputStream writer, int skipBytes) throws IOException {
			// TODO Auto-generated method stub

		}

	}
}
