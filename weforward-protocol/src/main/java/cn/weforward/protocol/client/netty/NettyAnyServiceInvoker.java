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
package cn.weforward.protocol.client.netty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.weforward.common.util.StringUtil;
import cn.weforward.protocol.Header;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.RequestConstants;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.aio.ClientContext;
import cn.weforward.protocol.aio.ClientHandler;
import cn.weforward.protocol.aio.http.HttpConstants;
import cn.weforward.protocol.aio.netty.NettyHttpClient;
import cn.weforward.protocol.aio.netty.NettyHttpClientFactory;
import cn.weforward.protocol.aio.netty.NettyOutputStream;
import cn.weforward.protocol.client.AbstractServiceInvoker;
import cn.weforward.protocol.client.AioServiceInvoker;
import cn.weforward.protocol.client.execption.ServiceInvokeException;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.exception.AuthException;
import cn.weforward.protocol.exception.SerialException;
import cn.weforward.protocol.ext.Producer;
import cn.weforward.protocol.support.SimpleProducer.SimpleProducerInput;
import cn.weforward.protocol.support.SimpleProducer.SimpleProducerOutput;
import cn.weforward.protocol.support.datatype.SimpleDtObject;
import cn.weforward.protocol.support.datatype.SimpleDtString;

/**
 * 使用netty的服务调用器（不绑定服务名）
 * 
 * @author daibo,liangyi
 *
 */
public class NettyAnyServiceInvoker extends AbstractServiceInvoker implements AioServiceInvoker {
	protected final static NettyHttpClientFactory FACTORY = new NettyHttpClientFactory();

	protected String m_Charset = Header.CHARSET_UTF8;
	protected String m_ContentType = Header.CONTENT_TYPE_JSON;
	protected String m_AuthType = Header.AUTH_TYPE_SHA2;
	protected String m_Url;
	protected String m_AccessId;
	protected int m_ConnectTimeout = 5 * 1000;
	protected int m_ReadTimeout = 50 * 1000;

	protected Producer m_Producer;

	public NettyAnyServiceInvoker(String preUrl, Producer producer) {
		if (StringUtil.isEmpty(preUrl)) {
			throw new IllegalArgumentException("链接前缀不能为空");
		}
		if (preUrl.endsWith("/")) {
			m_Url = preUrl;
		} else {
			m_Url = preUrl + "/";
		}
		m_Producer = producer;
	}

	public Request createRequest(String serviceName, String method, DtObject params) {
		SimpleDtObject invoke = new SimpleDtObject(false);
		invoke.put(RequestConstants.METHOD, SimpleDtString.valueOf(method));
		if (null != params) {
			invoke.put(RequestConstants.PARAMS, params);
		}
		return createRequest(invoke, serviceName);
	}

	protected String getServiceUrl(String serviceName) {
		return m_Url + serviceName;
	}

	public void setProducer(Producer producer) {
		m_Producer = producer;
	}

	@Override
	public String getContentType() {
		return m_ContentType;
	}

	@Override
	public void setContentType(String type) {
		m_ContentType = type;
	}

	@Override
	public String getAuthType() {
		return m_AuthType;
	}

	@Override
	public void setAuthType(String type) {
		m_AuthType = type;
	}

	@Override
	public int getConnectTimeout() {
		return m_ConnectTimeout;
	}

	@Override
	public void setConnectTimeout(int ms) {
		m_ConnectTimeout = ms;
	}

	@Override
	public int getReadTimeout() {
		return m_ReadTimeout;
	}

	@Override
	public void setReadTimeout(int ms) {
		m_ReadTimeout = ms;
	}

	@Override
	protected String getServiceName() {
		throw new IllegalArgumentException("无绑定服务名");
	}

	@Override
	protected String getCharset() {
		return m_Charset;
	}

	public void setAccessId(String accessId) {
		m_AccessId = accessId;
	}

	@Override
	protected String getAccessId() {
		return m_AccessId;
	}

	@Override
	public Response invoke(Request request) throws ServiceInvokeException {
		NettyHttpClient client = null;
		try {
			client = FACTORY.open(ClientHandler.SYNC);
			client.setReadTimeout(getReadTimeout());
			String serviceName = request.getHeader().getService();
			client.setUserAgent(serviceName);
			client.request(getServiceUrl(serviceName), HttpConstants.METHOD_POST);
			OutputStream out = client.openRequestWriter();
			m_Producer.make(request, new SimpleProducerOutput(client, out));
			out.close();
			int responseCode = client.getResponseCode();
			if (HttpConstants.OK != responseCode) {
				// int expect =
				// NumberUtil.toInt(client.getResponseHeaders().get(HttpConstants.CONTENT_LENGTH),
				// 0);
				// int limit = 1024;
				// String msg =
				// CachedInputStream.readString(client.getResponseStream(),
				// expect, limit, m_Charset);
				// throw new HttpTransportException(responseCode, msg);
				throw new ServiceInvokeException("响应异常:" + responseCode);
			}
			String service = request.getHeader().getService();
			InputStream in = client.getResponseStream();
			Response res = m_Producer.fetchResponse(new SimpleProducerInput(client.getResponseHeaders(), in, service));
			in.close();
			return res;
		} catch (AuthException e) {
			throw new ServiceInvokeException("验证异常", e);
		} catch (SerialException e) {
			throw new ServiceInvokeException("序列化异常", e);
		} catch (IOException e) {
			throw new ServiceInvokeException("IO异常", e);
		} finally {
			if (null != client) {
				client.close();
			}
		}
	}

	@Override
	public void invoke(Request request, Listener listener) throws ServiceInvokeException {
		NettyHttpClient client = null;
		try {
			InvokeHandler handler = new InvokeHandler(request, listener);
			client = FACTORY.open(handler);
			handler.setHttpClient(client);
			String serviceName = request.getHeader().getService();
			client.setUserAgent(serviceName);
			client.setReadTimeout(getReadTimeout());
			client.request(getServiceUrl(serviceName), HttpConstants.METHOD_POST);
			client = null;
		} catch (IOException e) {
			throw new ServiceInvokeException("IO异常", e);
		} finally {
			if (null != client) {
				client.close();
			}
		}
	}

	/**
	 * 异步IO处理
	 * 
	 * @author liangyi
	 *
	 */
	class InvokeHandler implements ClientHandler {
		NettyHttpClient m_Client;
		Request m_Request;
		Listener m_Listener;

		InvokeHandler(Request request, Listener listener) {
			m_Listener = listener;
			m_Request = request;
		}

		void setHttpClient(NettyHttpClient client) {
			m_Client = client;
		}

		@Override
		public void connectFail() {
			fail(null);
		}

		@Override
		public void established(ClientContext context) {
			// 提交请求
			NettyOutputStream out = null;
			try {
				out = m_Client.openRequestWriter();
				m_Producer.make(m_Request, new SimpleProducerOutput(m_Client, out));
				out.close();
				out = null;
			} catch (SerialException | AuthException | IOException e) {
				fail(e);
			} finally {
				if (null != out) {
					// 提交失败
					// try {
					// // 尝试取消输出
					// out.cancel();
					// } catch (IOException e) {
					// // 略过
					// }
					// 断开连接
					m_Client.disconnect();
				}
			}
		}

		@Override
		public void requestCompleted() {
		}

		@Override
		public void requestAbort() {
			fail(null);
		}

		@Override
		public void responseHeader() {
		}

		@Override
		public void prepared(int available) {
		}

		@Override
		public void responseCompleted() {
			Response response;
			try {
				int responseCode;
				responseCode = m_Client.getResponseCode();
				if (HttpConstants.OK != responseCode) {
					fail(new ServiceInvokeException("响应异常:" + responseCode));
					return;
				}
				String service = m_Request.getHeader().getService();
				InputStream in = m_Client.getResponseStream();
				response = m_Producer
						.fetchResponse(new SimpleProducerInput(m_Client.getResponseHeaders(), in, service));
				in.close();
			} catch (SerialException | AuthException | IOException e) {
				fail(e);
				return;
			}
			Listener listener = m_Listener;
			if (null == listener) {
				return;
			}
			m_Listener = null;
			listener.success(m_Request, response);
			listener.complete(m_Request);
		}

		@Override
		public void responseTimeout() {
			fail(null);
		}

		@Override
		public void errorResponseTransferTo(IOException e, Object msg, OutputStream writer) {
		}

		private void fail(Throwable throwable) {
			Listener listener = m_Listener;
			if (null == listener) {
				return;
			}
			m_Listener = null;
			listener.fail(m_Request, throwable);
			listener.complete(m_Request);
		}
	}
}
