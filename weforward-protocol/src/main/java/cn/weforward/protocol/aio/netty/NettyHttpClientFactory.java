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
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.util.NumberUtil;
import cn.weforward.common.util.StringBuilderPool;
import cn.weforward.protocol.aio.ClientChannel;
import cn.weforward.protocol.aio.ClientContext;
import cn.weforward.protocol.aio.ClientHandler;
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
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.OutOfDirectMemoryError;

/**
 * NettyHttpClient工厂
 * 
 * @author liangyi
 *
 */
public class NettyHttpClientFactory implements ClientChannel {
	static final Logger _Logger = LoggerFactory.getLogger(NettyHttpClientFactory.class);

	Bootstrap m_Bootstrap;
	volatile EventLoopGroup m_EventLoopGroup;
	/** 按主机+端口组织的连接池 */
	Map<String, Service> m_Services;
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
	/** 尽量控制的每组并发连接数（若指定） */
	protected int m_FineConnections = NumberUtil
			.toInt(System.getProperty("cn.weforward.protocol.aio.netty.FINE_CONNECTIONS"), 100);
	/** 控制长连接重用次数（若指定） */
	protected int m_KeepaliveRequests = NumberUtil
			.toInt(System.getProperty("cn.weforward.protocol.aio.netty.KEEPALIVE_REQUESTS"), 0);

	private static final AttributeKey<ServiceChannel> KEEPALIVE_REQUESTS_KEY = AttributeKey
			.newInstance("WF.KEEPALIVE_REQUESTS");

	public NettyHttpClientFactory() {
		m_Services = new HashMap<String, Service>();
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

	public void setFineConnections(int max) {
		m_FineConnections = max;
	}

	public void setKeepaliveRequests(int max) {
		m_KeepaliveRequests = max;
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

	private String genKey(String host, int port) {
		return host + ":" + port;
	}

	private Service openService(String host, int port) {
		String key = genKey(host, port);
		Service service;
		synchronized (m_Services) {
			service = m_Services.get(key);
			if (null == service) {
				service = new Service(key);
				m_Services.put(key, service);
			}
		}
		return service;
	}

	/**
	 * 创建异步HTTP客户端
	 * 
	 * @param handler 客户端处理器，若指定ClientHandler.SYNC则可工作在同步模式
	 * @return HTTP客户端
	 */
	public NettyHttpClient open(ClientHandler handler) {
		NettyHttpClient client = new NettyHttpClient(this, handler);
		return client;
	}

	@Override
	public ClientContext request(ClientHandler handler, String uri, String verb) throws IOException {
		NettyHttpClient client = new NettyHttpClient(this, handler);
		client.request(uri, verb);
		return client;
	}

	public void connect(final NettyHttpClient client, String host, int port, final boolean ssl) throws IOException {
		if (ssl && null == m_SslContext) {
			throw new SSLException("不支持");
		}

		// Channel channel = getIdelChannel(host, port);
		final Service service = openService(host, port);
		Channel channel = service.get();
		if (null == channel) {
			try {
				// 没有空闲连接，准备创建新连接
				channel = service.pending(m_FineConnections);
			} catch (InterruptedException e) {
				throw new InterruptedIOException(e.getMessage());
			}
		}
		if (null != channel) {
			// 有空闲连接，直接关联
			channel.pipeline().addLast("client", client);
			return;
		}
		ChannelFuture future = null;
		try {
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
						pipeline.addLast("service", service);
						pipeline.addLast("client", client);
						service.establish(channel);
						service.trace("已连接", channel);
					} else {
						// 连接失败？
						service.fin();
						// _Logger.error(future.toString(), future.cause());
						client.connectFail(future.cause());
						channel.close();
					}
				}
			});
		} finally {
			if (null == future) {
				service.fin();
			}
		}
	}

	public void free(Channel channel) {
		if (null == channel || !channel.isActive()) {
			return;
		}
		InetSocketAddress ia = (InetSocketAddress) channel.remoteAddress();
		String host = ia.getHostString();
		int port = ia.getPort();
		Service service = openService(host, port);
		service.free(channel);
		service.trace("free", channel);
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
	 * 包装Channel及相关任务等（如：空闲检查）
	 * 
	 * @author liangyi
	 *
	 */
	static class ServiceChannel {
		final Service m_Service;
		final Channel m_Channel;
		ScheduledFuture<?> m_IdleTask;
		/** 请求计数 */
		int m_Requests;

		ServiceChannel(Service service, Channel channel) {
			m_Service = service;
			m_Channel = channel;
			m_Requests = 1;
			startIdleTask();
		}

		Channel take() {
			if (null != m_IdleTask) {
				m_IdleTask.cancel(true);
				m_IdleTask = null;
			}
			if (m_Channel.isActive() && m_Channel.isOpen()) {
				++m_Requests;
				return m_Channel;
			}
			return null;
		}

		public int getRequests() {
			return m_Requests;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof Channel && m_Channel == obj) {
				return true;
			}
			return false;
		}

		private void startIdleTask() {
			int timeout = m_Service.getIdleMillis();
			if (timeout > 0 && null != m_Channel && m_Channel.isActive()) {
				// 启动空闲检查任务
				m_IdleTask = m_Channel.eventLoop().schedule(new IdleChecker(), timeout, TimeUnit.MILLISECONDS);
			}
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
				if (m_Service.remove(ServiceChannel.this)) {
					// 关闭
					m_Channel.close();
					if (_Logger.isTraceEnabled()) {
						m_Service.trace("Idle[" + getRequests() + "]", m_Channel);
					}
				} else if (_Logger.isTraceEnabled()) {
					m_Service.trace("not free[" + getRequests() + "]", m_Channel);
				}
			}
		}
	}

	/**
	 * 由host+port标识的一组连接
	 * 
	 * @author liangyi
	 *
	 */
	@Sharable
	class Service extends ChannelInboundHandlerAdapter {
		/** 空闲连接 */
		List<ServiceChannel> m_Channels;
		/** 连接中或处理中的连接数 */
		int m_Pending;
		/** 重用计数 */
		long m_Reuses;
		/** 总请求计数 */
		long m_Requests;
		// final String m_Name;

		Service(String name) {
			// m_Name = name;
			m_Channels = new LinkedList<ServiceChannel>();
		}

		synchronized public void establish(Channel channel) {
			++m_Requests;
		}

		synchronized void fin() {
			--m_Pending;
			this.notifyAll();
		}

		synchronized Channel pending(int max) throws InterruptedException {
			if (m_Pending + m_Channels.size() >= max) {
				// 并发连接超额，等一会看看是否有空闲，或再创建新连接
				this.wait(100);
				Channel ret = get();
				if (null != ret) {
					return ret;
				}
				trace("超控", null);
			}
			++m_Pending;
			return null;
		}

		public int size() {
			return m_Channels.size();
		}

		synchronized boolean remove(ServiceChannel serviceChannel) {
			if (m_Channels.remove(serviceChannel)) {
				fin();
				return true;
			}
			return false;
		}

		@SuppressWarnings("unlikely-arg-type")
		synchronized boolean remove(Channel channel) {
			return m_Channels.remove(channel);
		}

		public int getIdleMillis() {
			return NettyHttpClientFactory.this.getIdleMillis();
		}

		synchronized public Channel get() {
			if (m_Channels.size() > 0) {
				ServiceChannel sc = m_Channels.remove(m_Channels.size() - 1);
				if (null != sc) {
					++m_Pending;
					++m_Reuses;
					++m_Requests;
					return sc.take();
				}
			}
			return null;
		}

		synchronized public void free(Channel channel) {
			ServiceChannel sc = null;
			Attribute<ServiceChannel> attribute = null;
			if (m_KeepaliveRequests > 0) {
				attribute = channel.attr(KEEPALIVE_REQUESTS_KEY);
				sc = attribute.get();
				if (null != sc && sc.getRequests() >= m_KeepaliveRequests) {
					// 超过keepalive requests，关闭吧
					channel.close();
					if (_Logger.isTraceEnabled()) {
						trace("over keepalive requests[" + sc.getRequests() + "]", channel);
					}
					return;
				}
			}
			if (null == sc) {
				sc = new ServiceChannel(this, channel);
				if (null != attribute) {
					attribute.set(sc);
				}
			}
			// 放回池复用
			m_Channels.add(sc);
			--m_Pending;
		}

		// @Override
		// public void channelActive(ChannelHandlerContext ctx) throws Exception
		// {
		// super.channelActive(ctx);
		// }

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			Channel channel = ctx.channel();
			remove(channel);
			fin();
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
				builder.append("{idle:").append(size());
				if (m_Reuses > 0) {
					builder.append(",reuses:").append(m_Reuses);
				}
				if (m_Pending > 0) {
					builder.append(",pending:").append(m_Pending);
				}
				builder.append("}");
				if (null != channel) {
					builder.append(channel.toString());
				}
				content = builder.toString();
			} finally {
				StringBuilderPool._128.offer(builder);
			}
			_Logger.trace(content);
		}
	}
}
