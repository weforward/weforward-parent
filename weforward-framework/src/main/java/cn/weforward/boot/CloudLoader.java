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
package cn.weforward.boot;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.boot.support.AbstractSpringApp;
import cn.weforward.common.util.StringUtil;
import cn.weforward.framework.ApiException;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.ServiceInvoker;
import cn.weforward.protocol.client.ServiceInvokerFactory;
import cn.weforward.protocol.support.datatype.FriendlyList;
import cn.weforward.protocol.support.datatype.FriendlyObject;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 云配置加载
 * 
 * @author daibo
 *
 */
public class CloudLoader {
	/** 日志 */
	static final Logger _Logger = LoggerFactory.getLogger(CloudLoader.class);
	/** 服务器id */
	String m_Serverid;
	/** 项目名 */
	String m_ProjectName;
	/** 云配置服务名 */
	String m_CloudConfigServiceName = "devops";
	/** 云配置方法名 */
	String m_CloudConfigMethodName = "/devops/config/serviceprops";
	/** 服务调用器 */
	ServiceInvoker m_Invoker;
	/** 访问地址 */
	String m_ApiUrl;
	/** 访问凭证id */
	String m_AccessId;
	/** 访问凭证key */
	String m_AccessKey;
	/** 是否禁用云功能 */
	boolean m_DisableCloud = false;
	/** 项目名key */
	private final static String PROJECT_NAME_KEY = "project_name";
	/** 服务名Key */
	private final static String CLOUD_CONFIG_SERVICENAME_KEY = "cloud.config.servicename";
	/** 方法名Key */
	private final static String CLOUD_CONFIG_METHODNAME_KEY = "cloud.config.methodname";
	/** 访问地址key */
	private final static String APIURL_KEY = "weforward.apiUrl";
	/** 访问凭证id的key */
	private final static String ACCESSID_KEY = "weforward.service.accessId";
	/** 访问凭证id的key */
	private final static String ACCESSKEY_KEY = "weforward.service.accessKey";

	public CloudLoader() {
		m_Serverid = System.getProperty(AbstractSpringApp.SERVERID_KEY);
		m_ProjectName = System.getProperty(PROJECT_NAME_KEY);
		String serviceName = System.getProperty(CLOUD_CONFIG_SERVICENAME_KEY);
		if (!StringUtil.isEmpty(serviceName)) {
			m_CloudConfigServiceName = serviceName;
		}
		String methodName = System.getProperty(CLOUD_CONFIG_METHODNAME_KEY);
		if (!StringUtil.isEmpty(methodName)) {
			m_CloudConfigMethodName = methodName;
		}
		m_ApiUrl = System.getProperty(APIURL_KEY);
		m_AccessId = System.getProperty(ACCESSID_KEY);
		m_AccessKey = System.getProperty(ACCESSKEY_KEY);
	}

	/**
	 * 加载资源
	 * 
	 * @param result 加载前的配置
	 * @return 加载后的配置
	 * @throws IOException 调用异常时抛出
	 */
	public Properties load(Properties result) throws IOException {
		if (StringUtil.isEmpty(m_ProjectName)) {
			m_ProjectName = result.getProperty(PROJECT_NAME_KEY);
		}
		if (StringUtil.isEmpty(m_ApiUrl)) {
			m_ApiUrl = result.getProperty(APIURL_KEY);
		}
		if (StringUtil.isEmpty(m_AccessId)) {
			m_AccessId = result.getProperty(ACCESSID_KEY);
		}
		if (StringUtil.isEmpty(m_AccessKey)) {
			m_AccessKey = result.getProperty(ACCESSKEY_KEY);
		}
		if (!m_DisableCloud) {
			ServiceInvoker invoker = getInvoker();
			if (null != invoker) {
				try {
					result = load(result, invoker, m_ProjectName, m_Serverid);
				} catch (Throwable e) {
					throw new IllegalArgumentException("获取配置失败:" + e.getMessage(), e);
				}
			}
		}
		return result;
	}

	/* 调用器 */
	private ServiceInvoker getInvoker() throws IOException {
		if (null == m_Invoker) {
			if (StringUtil.isEmpty(m_ApiUrl)) {
				_Logger.warn("未配置" + APIURL_KEY);
				return null;
			}
			if (StringUtil.isEmpty(m_AccessId) && StringUtil.isEmpty(m_AccessKey)) {
				// 同时为空时忽略
				_Logger.warn("未配置" + ACCESSID_KEY + "和" + ACCESSKEY_KEY);
				return null;
			}
			if (StringUtil.isEmpty(m_AccessId) || StringUtil.isEmpty(m_AccessKey)) {
				// 有一个为空时报错
				throw new IllegalArgumentException("未配置" + ACCESSID_KEY + "或" + ACCESSKEY_KEY);
			}
			m_Invoker = ServiceInvokerFactory.create(m_CloudConfigServiceName, m_ApiUrl, m_AccessId, m_AccessKey);
		}
		return m_Invoker;
	}

	/* 加载云资源 */
	private Properties load(Properties result, ServiceInvoker invoker, String name, String sid) throws ApiException {
		SimpleDtObject params = new SimpleDtObject();
		params.put("projectName", name);
		params.put("serverid", sid);
		Response response = invoker.invoke(m_CloudConfigMethodName, params);
		if (response.getResponseCode() != 0) {
			throw new ApiException(ApiException.CODE_INTERNAL_ERROR, "网关响应异常：" + response.getResponseMsg());
		}
		FriendlyObject hyresult = FriendlyObject.valueOf(response.getServiceResult());
		int code = hyresult.getInt("code");
		if (code != 0) {
			throw new ApiException(ApiException.CODE_INTERNAL_ERROR, "业务异常：" + hyresult.getString("msg"));
		}
		FriendlyList content = hyresult.getFriendlyList("content");
		for (int i = 0; i < content.size(); i++) {
			FriendlyObject item = content.getFriendlyObject(i);
			String key = item.getString("key");
			if (null != result.get(key)) {
				continue;
			}
			String value = item.getString("value");
			result.put(key, StringUtil.toString(value));
		}
		return result;
	}
}
