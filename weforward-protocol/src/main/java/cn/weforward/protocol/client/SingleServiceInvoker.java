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
package cn.weforward.protocol.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import cn.weforward.common.Dictionary;
import cn.weforward.common.util.StringUtil;
import cn.weforward.protocol.Header;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.execption.HttpTransportException;
import cn.weforward.protocol.client.execption.ServiceInvokeException;
import cn.weforward.protocol.ext.Producer;
import cn.weforward.protocol.support.SimpleProducer.SimpleProducerInput;
import cn.weforward.protocol.support.SimpleProducer.SimpleProducerOutput;

/**
 * 单一链接的服务调用器
 * 
 * @author zhangpengji
 *
 */
public class SingleServiceInvoker extends AbstractServiceInvoker {
	protected String m_ServiceName;

	protected String m_Charset = Header.CHARSET_UTF8;
	protected String m_ContentType = Header.CONTENT_TYPE_JSON;
	protected String m_AuthType = Header.AUTH_TYPE_SHA2;

	protected String m_AccessId;
	protected Producer m_Producer;

	protected String m_UrlStr;
	protected URL m_Url;
	protected int m_ConnectTimeout = 5 * 1000;
	protected int m_ReadTimeout = 60 * 1000;

	public SingleServiceInvoker(String preUrl, String serviceName, Producer producer) {
		if (StringUtil.isEmpty(preUrl) || StringUtil.isEmpty(serviceName)) {
			throw new IllegalArgumentException("链接与服务名不能为空");
		}
		String url;
		if (preUrl.endsWith(serviceName)) {
			url = preUrl;
		} else if (preUrl.endsWith("/")) {
			url = preUrl + serviceName;
		} else {
			url = preUrl + "/" + serviceName;
		}
		m_UrlStr = url;
		try {
			m_Url = new URL(url);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Url格式异常：" + url, e);
		}
		m_ServiceName = serviceName;
		m_Producer = producer;
	}

	public String getServiceName() {
		return m_ServiceName;
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

	public String getAccessId() {
		return m_AccessId;
	}

	public void setAccessId(String id) {
		m_AccessId = id;
	}

	public void setProducer(Producer producer) {
		m_Producer = producer;
	}

	public String getCharset() {
		return m_Charset;
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
		m_ReadTimeout = (ms);
	}

	@Override
	public Response invoke(Request request) throws ServiceInvokeException {
		OutputStream out = null;
		InputStream in = null;
		try {
			HttpURLConnection conn = createConnection();
			if (request.getWaitTimeout() > 0) {
				conn.setReadTimeout(request.getWaitTimeout() * 1000);
			}
			out = new HttpOutput(conn);
			m_Producer.make(request, new SimpleProducerOutput(conn, out));
			out.close();
			out = null;
			int responseCode = conn.getResponseCode();
			if (HttpURLConnection.HTTP_OK != responseCode) {
				// 确保finally能执行close，及时释放连接
				if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
					in = conn.getErrorStream();
				} else {
					in = conn.getInputStream();
				}
				// if (503 == responseCode) {
				// throw new UnavailableException(responseCode + "/" +
				// conn.getResponseMessage());
				// }
				throw new HttpTransportException(responseCode, conn.getResponseMessage());
			}
			in = conn.getInputStream();
			String service = request.getHeader().getService();
			Response res;
			res = m_Producer.fetchResponse(
					new SimpleProducerInput(toDictionary(conn.getHeaderFields()), in, service));
			in.close();
			return res;
		} catch (Exception e) {
			throw new ServiceInvokeException(e);
		} finally {
			close(out);
			close(in);
		}
	}

	protected HttpURLConnection createConnection() throws IOException {
		HttpURLConnection conn = null;
		conn = (HttpURLConnection) m_Url.openConnection();
		conn.setRequestMethod("POST");

		// post不自动重发
		// System.setProperty("sun.net.http.retryPost", "false");
		// conn.setChunkedStreamingMode(0); 会导致conn.getOutputStream()变得巨慢无比

		conn.setConnectTimeout(m_ConnectTimeout);
		conn.setReadTimeout(m_ReadTimeout);
		conn.setDoOutput(true);
		conn.setDoInput(true);
		return conn;
	}

	private static Dictionary<String, String> toDictionary(final Map<String, List<String>> map) {
		return new Dictionary<String, String>() {
			@Override
			public String get(String key) {
				List<String> list = map.get(key);
				if (null != list && list.size() > 0) {
					return list.get(0);
				}
				return null;
			}
		};
	}

	private void close(Closeable close) {
		if (null == close) {
			return;
		}
		try {
			close.close();
		} catch (IOException e) {
		}
	}

	/**
	 * 封装HttpURLConnection延后到输出header完后再打开输出流
	 */
	static class HttpOutput extends OutputStream {
		OutputStream m_Original;
		HttpURLConnection m_Connection;

		HttpOutput(HttpURLConnection conn) {
			m_Connection = conn;
		}

		private OutputStream open() throws IOException {
			if (null == m_Original) {
				m_Original = m_Connection.getOutputStream();
			}
			return m_Original;
		}

		@Override
		public void write(int b) throws IOException {
			open().write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			open().write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			open().write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			open().flush();
		}

		@Override
		public void close() throws IOException {
			open().close();
		}
	}

	@Override
	public String toString() {
		return "{url:" + m_UrlStr + ",acc:" + m_AccessId + ",cs:" + m_Charset + ",ct:"
				+ m_ContentType + ",at:" + m_AuthType + "}";
	}
}
