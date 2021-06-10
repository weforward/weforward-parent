package cn.weforward.protocol.client.netty;

import java.io.IOException;

import cn.weforward.protocol.aio.ClientChannel;
import cn.weforward.protocol.aio.ClientContext;
import cn.weforward.protocol.aio.ClientHandler;
import cn.weforward.protocol.aio.ServerHandlerFactory;
import cn.weforward.protocol.aio.netty.NettyWebSocketFactory;
import cn.weforward.protocol.ext.Producer;

/**
 * 使用netty的websocket服务调用器
 * 
 * @author liangcha
 *
 */
public class NettyWebSocketInvoker extends AbstractNettyServiceInvoker {
	protected final static NettyWebSocketFactory FACTORY = new NettyWebSocketFactory();

	protected String m_Url;
	protected Handler m_Handler;
	protected ServerHandlerFactory m_ServiceHandler;
	protected String m_ServiceName;

	public NettyWebSocketInvoker(ServerHandlerFactory serviceHandler, String url, Producer producer) {
		super(producer);
		m_Url = url;
		m_ServiceHandler = serviceHandler;
	}

	public void setServiceName(String name) {
		m_ServiceName = name;
	}

	@Override
	protected String getServiceName() {
		return m_ServiceName;
	}

//	/**
//	 * 配置重连接间隔
//	 * 
//	 * @param secs 间隔时间（秒）
//	 */
//	public void setTryInterval(int secs) {
//		open();
//		m_Handler.setInterval(secs);
//	}

	@Override
	public int getConnectTimeout() {
		return FACTORY.getConnectTimeout();
	}

	@Override
	public void setConnectTimeout(int ms) {
	}

	@Override
	protected ClientChannel open() throws IOException {
		if (null == m_Handler) {
			synchronized (this) {
				if (null == m_Handler) {
					m_Handler = new Handler();
					FACTORY.connect(m_ServiceHandler, m_Url, m_Handler);
				}
			}
		}
		return m_Handler;
	}

	@Override
	protected String getServiceUrl(String serviceName) {
		return serviceName;
	}

	/**
	 * 保持的websocket通道
	 */
	class Handler extends NettyWebSocketFactory.Keepalive implements ClientChannel {
		ClientChannel m_Channel;

		//// ConnectionListener ////
		@Override
		public void establish(ClientChannel channel) {
			m_Channel = channel;
			super.establish(channel);
		}

		//// ClientChannel ////
		@Override
		public ClientContext request(ClientHandler handler, String uri, String verb) throws IOException {
			if (null == m_Channel) {
				throw new IOException("no connect");
			}
			return m_Channel.request(handler, uri, verb);
		}

		@Override
		public void close() {
			m_Channel.close();
		}
	}
}
