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
package cn.weforward.protocol.aio.netty;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.protocol.aio.ServerHandlerFactory;
import cn.weforward.protocol.aio.netty.websocket.WebSocketContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.OutOfDirectMemoryError;

/**
 * Netty Websocket 工厂
 * 
 * @author liangyi
 *
 */
public class NettyWebsocketFactory {
	static final Logger _Logger = LoggerFactory.getLogger(NettyWebsocketFactory.class);

	Bootstrap m_Bootstrap;
	volatile EventLoopGroup m_EventLoopGroup;
	/** SSL支持 */
	SslContext m_SslContext;
	/** 名称 */
	String m_Name;
	/** 工作线程数 */
	int m_Threads;
	/** 空闲超时值（毫秒） */
	protected int m_IdleMillis = 10 * 60 * 1000;
	/** 是否debug模式 */
	protected boolean m_DebugEnabled = false;

	public NettyWebsocketFactory() {
		m_Threads = NettyRuntime.availableProcessors() * 2;
		if (m_Threads > 8) {
			m_Threads = 8;
		}
	}

	public void setName(String name) {
		m_Name = name;
	}

	public String getName() {
		return m_Name;
	}

	public void setThreads(int threads) {
		m_Threads = threads;
	}

	/**
	 * 空闲超时值（秒），默认10分钟
	 * 
	 * @param secs 空闲秒数
	 */
	public void setIdle(int secs) {
		m_IdleMillis = secs * 1000;
	}

	public int getIdleMillis() {
		return m_IdleMillis;
	}

//	public WebSocketContext open(ServerHandlerFactory factory,String url) throws IOException {
	public void open(ServerHandlerFactory factory, String url) throws IOException {
		URL uri = new URL(url);
		int port = uri.getPort();
		String protocol = uri.getProtocol().toLowerCase();
		boolean ssl = false;
		if ("ws".equals(protocol)) {
			if (port < 1) {
				port = 80;
			}
		} else if ("wss".equals(protocol)) {
			if (port < 1) {
				port = 443;
				ssl = true;
			}
		} else {
			throw new MalformedURLException("不支持的协议" + uri.getProtocol());
		}
		connect(factory, uri.getHost(), port, ssl);
	}

	public void connect(final ServerHandlerFactory factory, String host, int port, final boolean ssl)
			throws IOException {
		if (ssl && null == m_SslContext) {
			throw new SSLException("不支持");
		}
		ChannelFuture future = null;

		future = open().connect(host, port);
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Channel channel = future.channel();
				if (future.isSuccess()) {
					ChannelPipeline pipeline = channel.pipeline();
					if (ssl) {
						pipeline.addFirst("ssl", m_SslContext.newHandler(channel.alloc()));
					}
					pipeline.addLast(new HttpClientCodec(), new HttpObjectAggregator(8 * 1024),
							new WebSocketClientHandler());
					WebSocketContext handler = new WebSocketContext();
					handler.setServerHandlerFactory(factory);
					pipeline.addLast("ws-ctx", handler);
//						service.trace("已连接", channel);
				} else {
					// 连接失败？
//						service.fin();
					// _Logger.error(future.toString(), future.cause());
//					client.connectFail(future.cause());
					channel.close();
				}
			}
		});

	}

	/**
	 * 指定连接超时值，默认值是5秒
	 * 
	 * @param millis 超时值（毫秒）
	 */
	synchronized public void setConnectTimeout(int millis) {
		if (null == m_Bootstrap) {
			m_Bootstrap = new Bootstrap();
		}
		m_Bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, millis);
	}

	public int getConnectTimeout() {
		Integer v = (Integer) m_Bootstrap.config().options().get(ChannelOption.CONNECT_TIMEOUT_MILLIS);
		return (null == v) ? 0 : v;
	}

	public void setSsl(boolean enabled) throws SSLException {
		if (enabled) {
			SslContextBuilder builder = SslContextBuilder.forClient();
			m_SslContext = builder.build();
		} else {
			m_SslContext = null;
		}
	}

	public void setDebugEnabled(boolean enabled) {
		m_DebugEnabled = enabled;
	}

	public boolean isDebugEnabled() {
		return m_DebugEnabled;
	}

	synchronized public void close() {
		if (null != m_EventLoopGroup) {
			m_EventLoopGroup.shutdownGracefully();
		}
		m_EventLoopGroup = null;
		m_Bootstrap = null;
	}

	private Bootstrap open() {
		if (null != m_EventLoopGroup) {
			return m_Bootstrap;
		}
		synchronized (this) {
			if (null != m_EventLoopGroup) {
				return m_Bootstrap;
			}
			String name = getName();
			if (null == name || 0 == name.length()) {
				name = "hc";
			} else {
				name = name + "-hc";
			}
			ThreadFactory threadFactory = new DefaultThreadFactory(name);
			NioEventLoopGroup eventLoop = new NioEventLoopGroup(m_Threads, threadFactory);
			if (null == m_Bootstrap) {
				m_Bootstrap = new Bootstrap();
			}
			m_Bootstrap.group(eventLoop);
			m_Bootstrap.channel(NioSocketChannel.class);
			m_Bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
			m_Bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
//			m_Bootstrap.option(ChannelOption.SO_LINGER, 1);
			m_Bootstrap.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast("c-encoder", new HttpRequestEncoder());
					pipeline.addLast("c-decoder", new HttpResponseDecoder());
				}
			});
			m_EventLoopGroup = eventLoop;
		}
		NettyMemMonitor.getInstance().log();
		return m_Bootstrap;
	}

	/**
	 * 由host+port标识的一组连接
	 * 
	 * @author liangyi
	 *
	 */
	@Sharable
	class WebSocketClientHandler extends ChannelInboundHandlerAdapter {

		// @Override
		// public void channelActive(ChannelHandlerContext ctx) throws Exception
		// {
		// super.channelActive(ctx);
		// }

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			Channel channel = ctx.channel();
			super.channelInactive(ctx);
			trace("断开", channel);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			if (cause instanceof OutOfDirectMemoryError) {
				ctx.close();
			}
			super.exceptionCaught(ctx, cause);
		}

		public void trace(String msg, Channel channel) {
			if (!_Logger.isTraceEnabled()) {
				return;
			}
			StringBuilder builder = StringBuilderPool._128.poll();
			String content;
			try {
				if (null != msg) {
					builder.append(msg);
					builder.append(",");
				}
				// builder.append(m_Name);
//				builder.append("{idle:").append(size());
//				if (m_Reuses > 0) {
//					builder.append(",reuses:").append(m_Reuses);
//				}
//				if (m_Pending > 0) {
//					builder.append(",pending:").append(m_Pending);
//				}
//				builder.append("}");
				if (null != channel) {
					builder.append(channel.toString());
				}
				content = builder.toString();
			} finally {
				StringBuilderPool._128.offer(builder);
			}
			_Logger.trace(content);
		}

		ScheduledFuture<?> m_IdleTask;

		private void startIdleTask() {
//			int timeout = m_Service.getIdleMillis();
//			if (timeout > 0 && null != m_Channel && m_Channel.isActive()) {
//				// 启动空闲检查任务
//				m_IdleTask = m_Channel.eventLoop().schedule(new IdleChecker(), timeout, TimeUnit.MILLISECONDS);
//			}
		}

		/**
		 * 空闲超时检查
		 * 
		 * @author liangyi
		 *
		 */
		class IdleChecker implements Runnable {
			@Override
			public void run() {
//				if (m_Service.remove(ServiceChannel.this)) {
//					// 关闭
//					m_Channel.close();
//					if (_Logger.isTraceEnabled()) {
//						m_Service.trace("Idle[" + getRequests() + "]", m_Channel);
//					}
//				} else if (_Logger.isTraceEnabled()) {
//					m_Service.trace("not free[" + getRequests() + "]", m_Channel);
//				}
			}
		}
	}
}
