/**
 * Copyright (c) 2019,2020 honintech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package cn.weforward.protocol.aio.netty.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.crypto.Hex;
import cn.weforward.common.util.Bytes;
import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.protocol.aio.ClientContext;
import cn.weforward.protocol.aio.ClientHandler;
import cn.weforward.protocol.aio.ConnectionListener;
import cn.weforward.protocol.aio.ServerHandler;
import cn.weforward.protocol.aio.ServerHandlerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.OutOfDirectMemoryError;

/**
 * 切换到WebSocket下的处理器
 * 
 * @author liangyi
 *
 */
public class WebSocketContext extends ChannelInboundHandlerAdapter implements WebSocketChannel {
	static final Logger _Logger = LoggerFactory.getLogger(WebSocketContext.class);

	/** 业务处理器工厂 */
	protected ServerHandlerFactory m_HandlerFactory = ServerHandlerFactory._unassigned;
	/** netty连接的上下文 */
	protected ChannelHandlerContext m_Ctx;
	/** 客户端地址 */
	protected String m_RemoteAddr;
	/** 多路复用的通道 */
	protected Map<String, WebSocketSession> m_Multiplex;
	/** 请求序号生成器 */
	protected AtomicLong m_Sequencer;
	protected ScheduledFuture<?> m_PingTask;
	/** 连接的事件监听器 */
	protected ConnectionListener m_ConnectionListener = ConnectionListener._unassigned;

	public WebSocketContext() {
		m_Multiplex = new HashMap<String, WebSocketSession>();
		m_Sequencer = new AtomicLong();
	}

	public void setServerHandlerFactory(ServerHandlerFactory factory) {
		m_HandlerFactory = factory;
	}

	public void setConnectionListener(ConnectionListener listener) {
		m_ConnectionListener = listener;
	}

	public ConnectionListener getConnectionListener() {
		return m_ConnectionListener;
	}

	public void lost(ChannelHandlerContext ctx) {
		if (null != ctx) {
			initRemoteAddr(ctx.channel());
		}
		m_ConnectionListener.lost(this);
	}

	protected void initRemoteAddr(Channel channel) {
		if (null != m_RemoteAddr) {
			return;
		}
		// 获取调用端信息（IP+端口）
		InetSocketAddress ip = (InetSocketAddress) channel.remoteAddress();
		if (null != ip) {
			m_RemoteAddr = ip.getAddress().getHostAddress() + ':' + ip.getPort();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof OutOfDirectMemoryError || cause.getCause() instanceof OutOfDirectMemoryError) {
			ctx.close();
		}
		super.exceptionCaught(ctx, cause);
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		super.handlerAdded(ctx);
		m_Ctx = ctx;
		if (isDebugEnabled()) {
			_Logger.info(formatMessage("handlerAdded"));
		}
		initRemoteAddr(ctx.channel());
//		m_ConnectionListener.establish(this);
	}

//	@Override
//	public void channelActive(ChannelHandlerContext ctx) throws Exception {
//		super.channelActive(ctx);
//		if (isDebugEnabled()) {
//			_Logger.info(formatMessage("channelActive"));
//		}
//		m_Ctx = ctx;
//		initRemoteAddr(ctx.channel());
//		m_ConnectionListener.establish(this);
//	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		try {
			if (isDebugEnabled()) {
				_Logger.info(formatMessage("channelInactive"));
			}
			cleanup();
			lost(ctx);
		} finally {
			super.channelInactive(ctx);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			if (null != m_Ctx && ctx != m_Ctx) {
				_Logger.error("不一样的Context？" + m_Ctx + "!=" + ctx);
				ctx.close();
				return;
			}
			// if (_Logger.isTraceEnabled()) {
			// _Logger.trace("[" + ctx.hashCode() + "]" + msg);
			// }
			if (msg instanceof WebSocketFrame) {
				WebSocketFrame wsframe = (WebSocketFrame) msg;
				if (wsframe instanceof BinaryWebSocketFrame || wsframe instanceof TextWebSocketFrame) {
					if (_Logger.isTraceEnabled()) {
						_Logger.trace(msg.toString());
					}
					readable(wsframe);
					// 略过除了BinaryWebSocketFrame的其它类型的帧
					return;
				}
				if (wsframe instanceof PingWebSocketFrame) {
					// PING响应PONG
					if (_Logger.isTraceEnabled()) {
						_Logger.trace(formatMessage("ping " + msg));
					}
					PongWebSocketFrame pong = new PongWebSocketFrame(wsframe.content().retain());
					ctx.channel().writeAndFlush(pong);
					return;
				}
				if (isDebugEnabled()) {
					_Logger.warn(formatMessage("未知帧类型？" + msg));
				}
			}
		} finally {
			super.channelRead(ctx, msg);
		}
	}

	/**
	 * 启动PING->PONG心跳保持（一般由TCP服务端发送PING）
	 * 
	 * @param millis 心跳间隔（毫秒），Firefox,Chrome等浏览器超过30秒会断开
	 */
	synchronized public void setKeepalive(int millis) {
		if (null != m_PingTask) {
			m_PingTask.cancel(false);
			m_PingTask = null;
		}
		if (millis < 1) {
			return;
		}
		Runnable worker = new Runnable() {
			@Override
			public void run() {
				PingWebSocketFrame ping = new PingWebSocketFrame();
				m_Ctx.channel().writeAndFlush(ping);
				return;
			}
		};
		m_PingTask = m_Ctx.executor().scheduleWithFixedDelay(worker, millis, millis, TimeUnit.MILLISECONDS);
	}

	public String getRemoteAddr() {
		return m_RemoteAddr;
	}

	/**
	 * websocket服务端（w）或客户端（z）标识
	 */
	protected char getSideMarker() {
		// 服务端
		return 'w';
	}

	/**
	 * 生成序号
	 */
	public String genSequence() {
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			builder.append(getSideMarker());
			Hex.toHex(m_Sequencer.incrementAndGet(), builder);
			return builder.toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
	}

	private void readable(WebSocketFrame wsframe) throws IOException {
		/**
		 * <pre>
		 * 对数据包进行二次封装：
		 * 1. 加上请求/响应序号用于多路复用，序号由一个用于标识请求（P）或响应（R）的字符加流水号组成，在连接中必须保持唯一<br/>
		 *     流水号由请求生成，对应的响应匹配此序号，序号最大长度不超过128字节<br/>
		 *     websocket服务端发起的请求流水号首个字符为w，客户端为z，即：服务端为“Pw*”或“Rw*”，客户端为“Pz*”或“Rz*”<br/>
		 * 2. 类似HTTP协议，把数据包封装为head及body ，head使用HTTP head规范，同样使用两个换行符表示head结束<br/>
		 * 3. 把请求/响应序号放在head，方便业务层读取，标识为：WS-RPC-ID
		 * 4. 使用Binary或Text帧，每个帧Payload由序号部分与head或body部分组成，包含head的帧必须完整在一个帧内，不允许分帧，Payload格式如下：
		 * |不定长的UTF-8字符集字串并以换行符“\n”结束的请求/响应序号|head或body的各部分...
		 * </pre>
		 */
		ByteBuf payload = wsframe.content();
		// 先读取序号
		payload.markReaderIndex();
		String seq = null;
		int packetState = 0;
		byte seqBuf[] = Bytes.Pool._512.poll();
		try {
			for (int i = 0; i < seqBuf.length && payload.isReadable(); i++) {
				seqBuf[i] = payload.readByte();
				if (WebSocketSession.PACKET_LF == seqBuf[i]) {
					// 碰到换行符，序号已完成
					if (WebSocketSession.PACKET_PREAMBLE_REQUEST == seqBuf[0]) {
						// 这是请求，那就按server处理
						packetState = WebSocketSession.PACKET_REQUEST;
					} else if (WebSocketSession.PACKET_PREAMBLE_RESPONSE == seqBuf[0]) {
						// 这是响应，那就是按client处理
						packetState = WebSocketSession.PACKET_RESPONSE;
					} else {
						// 标识有误
						_Logger.error("帧格式异常，序号标识错误：" + Hex.encode(seqBuf, 0, i));
						close();
						return;
					}
					seq = new String(seqBuf, 1, i, "UTF-8");
					break;
				}
			}
			if (null == seq) {
				// 没有序号的帧？
				_Logger.error("帧格式异常，序号没有/不合格" + seqBuf);
				close();
				return;
			}
		} finally {
			Bytes.Pool._512.offer(seqBuf);
		}
		seqBuf = null;
		if (wsframe.isFinalFragment()) {
			// 是最后的帧
			packetState |= WebSocketSession.PACKET_MARK_FINAL;
		}
		WebSocketSession session;
		if (WebSocketSession.PACKET_REQUEST == (WebSocketSession.PACKET_REQUEST & packetState)) {
			// 是请求消息
			session = openSession(seq);
			packetState = session.readable(payload, packetState);
			if (WebSocketSession.PACKET_MARK_HEADER == (WebSocketSession.PACKET_MARK_HEADER & packetState)) {
				// 请求开始
				requestHeader(session);
			}
			return;
		}
		// 是响应消息
		session = getSession(seq);
		if (null == session) {
			// 没有对应的请求？
			_Logger.warn("miss request:" + seq + ",frame:" + wsframe);
			return;
		}
		session.readable(payload, packetState);
		if (WebSocketSession.PACKET_MARK_FINAL == (WebSocketSession.PACKET_MARK_FINAL & packetState)) {
			// 响应结束，移除session
			removeSession(seq);
		}
	}

	synchronized protected WebSocketSession openSession(String seq) {
		WebSocketSession session = m_Multiplex.get(seq);
		if (null == session) {
			session = new WebSocketSession(this, seq);
			m_Multiplex.put(seq, session);
		}
		return session;
	}

	synchronized protected WebSocketSession removeSession(String seq) {
		WebSocketSession session = m_Multiplex.remove(seq);
		return session;
	}

	synchronized protected WebSocketSession getSession(String seq) {
		return m_Multiplex.get(seq);
	}

	synchronized protected void cleanup() {
		Map<String, WebSocketSession> empty = Collections.emptyMap();
		Map<String, WebSocketSession> multiplex = m_Multiplex;
		if (empty == multiplex) {
			return;
		}
		m_Multiplex = empty;
		WebSocketSession session;
		for (Map.Entry<String, WebSocketSession> e : multiplex.entrySet()) {
			session = e.getValue();
			if (null != session) {
				session.close();
			}
		}
	}

	public void close() {
		ChannelHandlerContext ctx = m_Ctx;
		if (null != ctx) {
			m_Ctx = null;
			ctx.close();
			cleanup();
		}
	}

	protected CompositeByteBuf compositeBuffer() {
		ChannelHandlerContext ctx = m_Ctx;
		if (null != ctx) {
			return ctx.alloc().compositeBuffer();
		}
		return ByteBufAllocator.DEFAULT.compositeBuffer();
	}

	public boolean isDebugEnabled() {
		// TODO
		return true;
	}

	private String formatMessage(String caption) {
		StringBuilder sb = new StringBuilder(128);
		if (null != caption) {
			sb.append(caption);
		}
		toString(sb);
		return sb.toString();
	}

	public ClientContext request(ClientHandler handler, String uri) throws IOException {
		String seq = genSequence();
		WebSocketSession session = openSession(seq);
		session.openRequest(handler, uri);
		return session.getClientContext();
	}

	/**
	 * 接收到调用请求（请求头已接收完）
	 * 
	 * @throws IOException
	 */
	private void requestHeader(WebSocketSession session) throws IOException {
		ServerHandler handler = null;
		try {
			handler = m_HandlerFactory.handle(session.getServerContext());
		} finally {
			if (null == handler) {
				if (session.isRespond()) {
					// 已经响应
					return;
				}
				// 没有业务处理，直接返回501，且主动关闭
				session.response(WebSocketSession.STATUS_NOT_IMPLEMENTED);
				return;
			}
		}
		session.onRequest(handler);
	}

	public StringBuilder toString(StringBuilder builder) {
		builder.append("{ip:");
		if (null != m_RemoteAddr) {
			builder.append(m_RemoteAddr);
		}
		builder.append(",mul:").append(m_Multiplex.size());
		builder.append("}");
		return builder;
	}

	@Override
	public String toString() {
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			return toString(builder).toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
	}
}
