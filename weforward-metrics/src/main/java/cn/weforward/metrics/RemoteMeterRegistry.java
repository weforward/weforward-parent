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
package cn.weforward.metrics;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.weforward.common.KvPair;
import cn.weforward.common.crypto.Base64;
import cn.weforward.common.json.JsonOutputStream;
import cn.weforward.common.util.StringUtil;
import cn.weforward.metrics.ext.AbstarctMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter.Id;

/**
 * 基于远程推送的监控注册表
 * 
 * @author daibo
 *
 */
public class RemoteMeterRegistry extends AbstarctMeterRegistry {

	/** 远程url */
	protected List<URL> m_Urls;
	/** 访问帐号 */
	protected String m_UserName;
	/** 访问密码 */
	protected String m_Password;
	/** 连接超时值（毫秒） */
	protected int m_ConnectTimeout = 3 * 1000;
	/** 读/等待结果超时值（毫秒） */
	protected int m_ReadTimeout = 6 * 1000;

	/**
	 * 构造
	 * 
	 * @param url 远程链接
	 * @throws MalformedURLException 链接异常
	 */
	public RemoteMeterRegistry(String url) throws MalformedURLException {
		this(Collections.singletonList(url), false);
	}

	/**
	 * 构造
	 * 
	 * @param url    远程链接
	 * @param global 是否全局
	 * @throws MalformedURLException 链接异常
	 */
	public RemoteMeterRegistry(String url, boolean global) throws MalformedURLException {
		this(Collections.singletonList(url), global);
	}

	/**
	 * 构造
	 * 
	 * @param urls   远程链接列表
	 * @param global 是否全局
	 * @throws MalformedURLException 链接异常
	 */
	public RemoteMeterRegistry(List<String> urls, boolean global) throws MalformedURLException {
		super(RemoteRegisterConfig.DEFAULT, Clock.SYSTEM, global);
		if (null == urls || urls.size() == 0) {
			m_Urls = Collections.emptyList();
			return;
		}
		List<URL> ls = new ArrayList<>(urls.size());
		for (String url : urls) {
			if (StringUtil.isEmpty(url)) {
				continue;
			}
			ls.add(new URL(url));
		}
		m_Urls = ls;
	}

	/**
	 * 远程url
	 * 
	 * @return url列表
	 */
	public List<URL> getUrls() {
		return m_Urls;
	}

	public void setUserName(String username) {
		m_UserName = username;
	}

	public void setPassword(String password) {
		m_Password = password;
	}

	/**
	 * 连接超时值（秒）
	 * 
	 * @param second 秒
	 */
	public void setConnectTimeoutSecond(int second) {
		m_ConnectTimeout = second * 1000;
	}

	/**
	 * 读/等待结果超时值（秒）
	 * 
	 * @param second 秒
	 */
	public void setReadTimeoutSecond(int second) {
		m_ReadTimeout = second * 1000;
	}

	@Override
	protected void publish(Id id, List<KvPair<String, Double>> measure) throws IOException {
		for (URL url : m_Urls) {
			send(url, id, measure);
		}
	}

	private void send(URL url, Id id, List<KvPair<String, Double>> list) throws IOException {
		HttpURLConnection http = null;
		try {
			http = (HttpURLConnection) url.openConnection();
			http.setConnectTimeout(m_ConnectTimeout);
			http.setReadTimeout(m_ReadTimeout);
			http.setDoInput(true);
			http.setDoOutput(true);
			http.setUseCaches(false);
			http.setRequestMethod("POST");
			if (!StringUtil.isEmpty(m_UserName) && !StringUtil.isEmpty(m_Password)) {
				String basic = "Basic " + new String(Base64.encode((m_UserName + ":" + m_Password).getBytes()));
				http.setRequestProperty("Authorization", basic);
			}
			OutputStream out = http.getOutputStream();
			JsonOutputStream sb = new JsonOutputStream(out);
			out(sb, id, list);
			out.flush();
			sb.close();
			int status = http.getResponseCode();
			if (HttpURLConnection.HTTP_OK != status) {
				throw new UnknownServiceException(
						url + " 服务异常：" + status + " " + StringUtil.toString(http.getResponseMessage()));
			}
			http.getInputStream().close();
		} catch (IOException e) {
			throw e;
		}
	}

}
