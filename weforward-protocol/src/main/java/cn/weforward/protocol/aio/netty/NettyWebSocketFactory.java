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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.util.StringUtil;
import cn.weforward.protocol.aio.ClientChannel;
import cn.weforward.protocol.aio.ConnectionListener;
import cn.weforward.protocol.aio.ServerHandlerFactory;
import cn.weforward.protocol.aio.netty.websocket.WebSocketContext;
import cn.weforward.protocol.aio.netty.websocket.WebSocketContextClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.CharsetUtil;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Netty Websocket 工厂
 * 
 * @author liangyi
 *
 */
public class NettyWebSocketFactory {
	static final Logger _Logger = LoggerFactory.getLogger(NettyWebSocketFactory.class);

	Bootstrap m_Bootstrap;
	volatile EventLoopGroup m_EventLoopGroup;
	/** SSL支持 */
	SslContext m_SslContext;
	/** 名称 */
	String m_Name;
	/** 工作线程数 */
	int m_Threads;
	/** 空闲超时值（秒） */
	protected int m_IdleSeconds;

	public NettyWebSocketFactory() {
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
	 * 空闲超时值（秒）
	 * 
	 * @param secs 空闲秒数
	 */
	public void setIdle(int secs) {
		m_IdleSeconds = secs;
	}

	/**
	 * 是否websocket的URL
	 * 
	 * @param url URL
	 */
	public static boolean isWebSocket(String url) {
		if (null == url || url.length() < 6) {
			return false;
		}
		String protocol = url.substring(0, 6).toLowerCase();
		return protocol.startsWith("ws://") || protocol.startsWith("wss://");
	}

	public ClientChannel connect(ServerHandlerFactory factory, final String url, final ConnectionListener listener)
			throws IOException {
		final URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
		int port = uri.getPort();
		String protocol = uri.getScheme().toLowerCase();
		final boolean ssl;
		if ("ws".equals(protocol)) {
			if (port < 1) {
				port = 80;
			}
			ssl = false;
		} else if ("wss".equals(protocol)) {
			if (port < 1) {
				port = 443;
			}
			ssl = true;
			if (ssl && null == m_SslContext) {
				throw new SSLException("不支持");
			}
		} else {
			throw new MalformedURLException("不支持的协议：" + protocol);
		}
		ChannelFuture future = null;
		final WebSocketContextClient handler = new WebSocketContextClient(factory);
		handler.setConnectionListener(listener);
		if (listener instanceof Keepalive) {
			((Keepalive) listener).init(this, factory, url);
		}
		if (_Logger.isDebugEnabled()) {
			_Logger.debug("connecting " + url);
		}
		try {
			future = open().connect(uri.getHost(), port);
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					Channel channel = future.channel();
					if (future.isSuccess()) {
						ChannelPipeline pipeline = channel.pipeline();
						if (ssl) {
							pipeline.addFirst("ssl", m_SslContext.newHandler(channel.alloc()));
						}
						WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri,
								WebSocketVersion.V13, null, false, new DefaultHttpHeaders());
						pipeline.addLast(new HttpClientCodec(), new HttpObjectAggregator(8 * 1024),
								new Handshaker(handshaker, handler));
						if (_Logger.isDebugEnabled()) {
							_Logger.debug("已连接 " + channel);
						}
						handshaker.handshake(channel);
					} else {
						// 连接失败？
						channel.close();
						if (null != listener) {
							listener.fail(url, future.cause());
						}
					}
				}
			});
			future = null;
		} finally {
			if (null != future) {
				if (null != listener) {
					listener.fail(url, null);
				}
			}
		}
		return handler;
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
				}
			});
			m_EventLoopGroup = eventLoop;
		}
		NettyMemMonitor.getInstance().log();
		return m_Bootstrap;
	}

	/**
	 * 完成握手处理过程
	 */
	class Handshaker extends ChannelInboundHandlerAdapter {
		WebSocketClientHandshaker m_Handshaker;
		WebSocketContext m_Context;

		Handshaker(WebSocketClientHandshaker handshaker, WebSocketContext context) {
			m_Handshaker = handshaker;
			m_Context = context;
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//			trace("断开", channel);
			try {
				m_Context.lost(ctx);
			} finally {
				super.channelInactive(ctx);
			}
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			try {
				if (msg instanceof FullHttpResponse) {
					FullHttpResponse response = (FullHttpResponse) msg;
					if (!m_Handshaker.isHandshakeComplete()) {
						// 握手协议返回，设置结束握手
						try {
							m_Handshaker.finishHandshake(ctx.channel(), response);
							ChannelPipeline pipeline = ctx.channel().pipeline();
							pipeline.addLast("ws-ctx", m_Context);
							pipeline.remove(this);
							if (_Logger.isDebugEnabled()) {
								_Logger.debug("握手成功 " + ctx.channel());
							}
							if (m_IdleSeconds > 0) {
								m_Context.setIdle(m_IdleSeconds);
							}
							m_Context.getConnectionListener().establish(m_Context);
						} catch (Exception e) {
							_Logger.error("websock handshake error " + ctx.channel(), e);
							m_Context.getConnectionListener().fail(m_Handshaker.uri().toString(), e);
							ctx.close();
						}
						return;
					}
					throw new IllegalStateException("Unexpected FullHttpResponse {status:" + response.status()
							+ ",body:" + StringUtil.limit(response.content().toString(CharsetUtil.UTF_8), 200) + '}');
				}
			} finally {
				super.channelRead(ctx, msg);
			}
		}
	}

	/**
	 * 监听连接事件保持WebSocket连接
	 */
	public static class Keepalive implements ConnectionListener, Runnable {
		protected NettyWebSocketFactory m_WsFactory;
		protected int m_TryInterval;
		protected String m_Url;
		protected ServerHandlerFactory m_SvrFactory;

		public Keepalive() {
			m_TryInterval = 60;
		}

		/**
		 * 重试间隔
		 * 
		 * @param tryInterval 间隔时间（秒）
		 */
		public Keepalive(int tryInterval) {
			setInterval(tryInterval);
		}

		/**
		 * 配置重试间隔
		 * 
		 * @param secs 间隔时间（秒）
		 */
		public void setInterval(int secs) {
			if (secs < 1) {
				throw new IllegalArgumentException("tryInterval<1");
			}
			m_TryInterval = secs;
		}

		protected void init(NettyWebSocketFactory wsFactory, ServerHandlerFactory svrFactory, String url) {
			m_WsFactory = wsFactory;
			m_SvrFactory = svrFactory;
			m_Url = url;
		}

		@Override
		public void establish(ClientChannel channel) {
			if (_Logger.isDebugEnabled()) {
				_Logger.debug("establish " + channel);
			}
		}

		@Override
		public void fail(String url, Throwable cause) {
			if (_Logger.isDebugEnabled()) {
				_Logger.debug("fail,retry " + m_Url);
			}
			retry();
		}

		@Override
		public void lost(ClientChannel channel) {
			if (_Logger.isDebugEnabled()) {
				_Logger.debug("lost,retry " + m_Url);
			}
			retry();
		}

		protected void retry() {
			EventLoopGroup el = m_WsFactory.m_EventLoopGroup;
			if (null != el) {
				el.schedule(this, m_TryInterval, TimeUnit.SECONDS);
			}
		}

		@Override
		public void run() {
			try {
				m_WsFactory.connect(m_SvrFactory, m_Url, this);
			} catch (IOException e) {
//				retry();
				_Logger.error(m_Url, e);
			}
		}

		public String toString() {
			return "[" + m_TryInterval + "]" + m_Url;
		}
	}
}
