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
import cn.weforward.common.sys.ClockTick;
import cn.weforward.common.util.Bytes;
import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.common.util.StringUtil;
import cn.weforward.protocol.aio.ClientChannel;
import cn.weforward.protocol.aio.ClientContext;
import cn.weforward.protocol.aio.ClientHandler;
import cn.weforward.protocol.aio.ConnectionListener;
import cn.weforward.protocol.aio.ServerHandlerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.OutOfDirectMemoryError;

/**
 * 切换到WebSocket下的处理器
 * 
 * @author liangyi
 *
 */
public class WebSocketContext extends ChannelInboundHandlerAdapter implements ClientChannel {
	static final Logger _Logger = LoggerFactory.getLogger(WebSocketContext.class);

	static final ClockTick _Tick = ClockTick.getInstance(1);

	static final ByteBuf _PingData = Unpooled.wrappedBuffer("weforward".getBytes()).asReadOnly();

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
	/** 最后收到消息的时间点（单位秒） */
	protected long m_LastActivity;
	/** 定时PING */
	protected ScheduledFuture<?> m_PingTask;
	/** 定时检查空闲 */
	protected ScheduledFuture<?> m_IdleTask;
	/** 连接的事件监听器 */
	protected ConnectionListener m_ConnectionListener = ConnectionListener._unassigned;

	public WebSocketContext(ServerHandlerFactory factory) {
		m_Multiplex = new HashMap<String, WebSocketSession>();
		m_Sequencer = new AtomicLong();
		m_HandlerFactory = factory;
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
		if (_Logger.isDebugEnabled()) {
			_Logger.debug(formatMessage("handlerAdded"));
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
			if (_Logger.isDebugEnabled()) {
				_Logger.debug(formatMessage("channelInactive"));
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
			if (msg instanceof WebSocketFrame) {
				// 刷新活跃时间
				m_LastActivity = _Tick.getTickerLong();

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
					PongWebSocketFrame pong = new PongWebSocketFrame(wsframe.content().retain());
					ctx.channel().writeAndFlush(pong);
					if (_Logger.isDebugEnabled()) {
						_Logger.debug(formatMessage("pong"));
					}
					return;
				}
				if (_Logger.isDebugEnabled() && !(wsframe instanceof PongWebSocketFrame)) {
					_Logger.debug(formatMessage("未知帧类型？" + msg));
				}
			}
		} finally {
//			super.channelRead(ctx, msg);
			// 释放msg，不再传递到下一个，必须确保这是最后的处理器
			ReferenceCountUtil.release(msg);
		}
	}

	/**
	 * 启动PING->PONG心跳保持（一般由TCP服务端发送PING）
	 * 
	 * @param seconds 心跳间隔（秒），Firefox,Chrome等浏览器超过30秒会断开
	 */
	synchronized public void setKeepalive(int seconds) {
		if (null != m_PingTask) {
			m_PingTask.cancel(false);
			m_PingTask = null;
		}
		if (seconds < 1) {
			return;
		}
		Runnable worker = new Runnable() {
			@Override
			public void run() {
				PingWebSocketFrame ping = new PingWebSocketFrame(true, 0, _PingData.retainedDuplicate());
				ChannelFuture future = m_Ctx.channel().writeAndFlush(ping);
				if (_Logger.isDebugEnabled()) {
					_Logger.debug(formatMessage("ping"));
					future.addListener(new GenericFutureListener<Future<Void>>() {
						@Override
						public void operationComplete(Future<Void> future) throws Exception {
							if (!future.isSuccess()) {
								_Logger.debug(formatMessage("ping fail"), future.cause());
							}
						}
					});
				}
				return;
			}
		};
		m_PingTask = m_Ctx.executor().scheduleWithFixedDelay(worker, seconds, seconds, TimeUnit.SECONDS);
	}

	/**
	 * 启用空闲超时检测（秒）
	 * 
	 * @param seconds 空闲秒数
	 */
	synchronized public void setIdle(final int seconds) {
		if (null != m_IdleTask) {
			m_IdleTask.cancel(false);
			m_IdleTask = null;
		}
		if (seconds < 1) {
			return;
		}
		Runnable worker = new Runnable() {
			@Override
			public void run() {
				if (_Tick.getTickerLong() > m_LastActivity + seconds) {
					if (_Logger.isDebugEnabled()) {
						_Logger.debug(formatMessage("idle"));
					}
					m_IdleTask.cancel(false);
					m_IdleTask = null;
					close();
				}
				return;
			}
		};
		m_IdleTask = m_Ctx.executor().scheduleWithFixedDelay(worker, seconds, seconds, TimeUnit.SECONDS);
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
				if (WebSocketMessage.PACKET_LF == seqBuf[i]) {
					// 碰到换行符，序号已完成
					if (WebSocketMessage.PACKET_PREAMBLE_REQUEST == seqBuf[0]) {
						// 这是请求，那就按server处理
						packetState = WebSocketMessage.PACKET_REQUEST;
					} else if (WebSocketMessage.PACKET_PREAMBLE_RESPONSE == seqBuf[0]) {
						// 这是响应，那就是按client处理
						packetState = WebSocketMessage.PACKET_RESPONSE;
					} else {
						// 标识有误
						_Logger.error("帧格式异常，序号标识错误：" + Hex.encode(seqBuf, 0, i));
						close();
						return;
					}
					seq = new String(seqBuf, 1, i - 1, "UTF-8");
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
			packetState |= WebSocketMessage.PACKET_MARK_FINAL;
		}
		WebSocketSession session;
		if (WebSocketMessage.PACKET_REQUEST == (WebSocketMessage.PACKET_REQUEST & packetState)) {
			// 是请求消息
			session = openSession(seq);
//			packetState = session.readable(payload, packetState);
//			if (WebSocketMessage.PACKET_MARK_HEADER == (WebSocketMessage.PACKET_MARK_HEADER & packetState)) {
//				// 请求开始
//				requestHeader(session);
//			}
			session.readable(payload, packetState);
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
		if (WebSocketMessage.PACKET_MARK_FINAL == (WebSocketMessage.PACKET_MARK_FINAL & packetState)) {
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
		if (null != m_IdleTask) {
			m_IdleTask.cancel(false);
			m_IdleTask = null;
		}
		if (null != m_PingTask) {
			m_PingTask.cancel(false);
			m_PingTask = null;
		}
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

	protected ByteBuf allocBuffer(int len) {
		if (null != m_Ctx) {
			return m_Ctx.alloc().buffer(len);
		}
		return ByteBufAllocator.DEFAULT.buffer(len);
	}

	private String formatMessage(String caption) {
		StringBuilder builder = StringBuilderPool._128.poll();
		try {
			if (null != caption) {
				builder.append(caption);
			}
			toString(builder);
			return builder.toString();
		} finally {
			StringBuilderPool._128.offer(builder);
		}
	}

	public ClientContext request(ClientHandler handler, String uri, String verb) throws IOException {
		String seq = genSequence();
		WebSocketSession session = openSession(seq);
		ClientContext client = session.openRequest(handler, uri);
		if (!StringUtil.isEmpty(verb)) {
			client.setRequestHeader(WebSocketMessage.HEADER_VERB, verb);
		}
		return client;
	}

	public StringBuilder toString(StringBuilder builder) {
		builder.append("{ip:");
		if (null != m_RemoteAddr) {
			builder.append(m_RemoteAddr);
		}
		builder.append(",seq:").append(m_Sequencer.get());
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
