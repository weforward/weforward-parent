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

import cn.weforward.protocol.Header;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.RequestConstants;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.aio.ClientChannel;
import cn.weforward.protocol.aio.ClientContext;
import cn.weforward.protocol.aio.ClientHandler;
import cn.weforward.protocol.aio.http.HttpConstants;
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
 * 使用netty的服务调用器基类
 * 
 * @author liangyi
 *
 */
public abstract class AbstractNettyServiceInvoker extends AbstractServiceInvoker implements AioServiceInvoker {
	protected String m_Charset = Header.CHARSET_UTF8;
	protected String m_ContentType = Header.CONTENT_TYPE_JSON;
	protected String m_AuthType = Header.AUTH_TYPE_SHA2;
	protected String m_AccessId;
	protected int m_ReadTimeout = 50 * 1000;
	protected Producer m_Producer;

	public AbstractNettyServiceInvoker(Producer producer) {
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

	abstract protected ClientChannel open() throws IOException;

	abstract protected String getServiceUrl(String serviceName);

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
	public int getReadTimeout() {
		return m_ReadTimeout;
	}

	@Override
	public void setReadTimeout(int ms) {
		m_ReadTimeout = ms;
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
		Sync sync = new Sync();
		invoke(request, sync);
		return sync.await(getReadTimeout());
	}

	@Override
	public void invoke(Request request, Listener listener) throws ServiceInvokeException {
		ClientContext client = null;
		try {
			InvokeHandler handler = new InvokeHandler(request, listener);
			String serviceName = request.getHeader().getService();
			client = open().request(handler, getServiceUrl(serviceName), HttpConstants.METHOD_POST);
			client.setTimeout(getReadTimeout());
			client.setRequestHeader(HttpConstants.USER_AGENT, serviceName);
			client = null;
		} catch (IOException e) {
			throw new ServiceInvokeException("IO异常", e);
		} finally {
			if (null != client) {
				client.disconnect();
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
		ClientContext m_Client;
		Request m_Request;
		Listener m_Listener;

		InvokeHandler(Request request, Listener listener) {
			m_Listener = listener;
			m_Request = request;
		}

		@Override
		public void connectFail() {
			fail(null);
		}

		@Override
		public void established(ClientContext context) {
			m_Client = context;
			// 提交请求
			OutputStream out = null;
			try {
				out = m_Client.openRequestWriter();
				m_Producer.make(m_Request, new SimpleProducerOutput(m_Client, out));
				out.close();
				out = null;
			} catch (SerialException | AuthException | IOException e) {
				fail(e);
			} finally {
				if (null != out) {
					// 提交失败，断开连接
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

	/**
	 * 阻塞调用
	 */
	static class Sync implements Listener {
		Response m_Response;
		Throwable m_Error;
		boolean m_Completed;

		@Override
		public void success(Request request, Response response) {
			m_Response = response;
		}

		@Override
		public void fail(Request request, Throwable throwable) {
			m_Error = throwable;
		}

		@Override
		synchronized public void complete(Request request) {
			m_Completed = true;
			this.notifyAll();
		}

		synchronized public Response await(int timeout) throws ServiceInvokeException {
			if (!m_Completed) {
				try {
					this.wait(timeout);
				} catch (InterruptedException e) {
					new ServiceInvokeException(e);
				}
			}
			if (null != m_Error) {
				if (m_Error instanceof ServiceInvokeException) {
					throw (ServiceInvokeException) m_Error;
				}
				throw new ServiceInvokeException(m_Error);
			}
			if (null == m_Response) {
				throw new ServiceInvokeException("resposne timeout");
			}
			return m_Response;
		}
	}
}
