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
package cn.weforward.framework.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.util.StringUtils;

import cn.weforward.common.sys.GcCleaner;
import cn.weforward.common.util.LruCache;
import cn.weforward.common.util.StringUtil;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.ServiceInvoker;
import cn.weforward.protocol.client.ServiceInvokerFactory;
import cn.weforward.protocol.ops.Right;
import cn.weforward.protocol.ops.Role;
import cn.weforward.protocol.ops.User;
import cn.weforward.protocol.ops.UserService;
import cn.weforward.protocol.support.NamingConverter;
import cn.weforward.protocol.support.datatype.FriendlyList;
import cn.weforward.protocol.support.datatype.FriendlyObject;
import cn.weforward.protocol.support.datatype.SimpleDtObject;
import cn.weforward.protocol.support.datatype.SimpleDtString;

/**
 * 基于微服务的用户服务
 * 
 * @author daibo
 *
 */
public class MicroserviceUserService implements UserService {
	/** 网关服务地址 */
	protected String m_ApiUrl;
	/** 服务访问id */
	protected String m_AccessId;
	/** 服务访问key */
	protected String m_AccessKey;
	/** 服务名 */
	protected String m_ServiceName;
	/** 方法组名 */
	protected String m_MethodGroup;
	/** 调用器 */
	protected ServiceInvoker m_Invoker;
	/** 基于用户id的缓存表 */
	protected LruCache<String, User> m_UserIdCache;
	/** 基于access的缓存表 */
	protected LruCache<String, User> m_AccessIdCache;

	public MicroserviceUserService(String apiUrl, String accessId, String accessKey, String serviceName,
			String methodGroup) {
		if (StringUtils.isEmpty(serviceName)) {
			throw new NullPointerException("serviceName不能为空");
		}
		m_ApiUrl = apiUrl;
		m_AccessId = accessId;
		m_AccessKey = accessKey;
		m_ServiceName = serviceName;
		m_MethodGroup = NamingConverter.camelToWf(StringUtil.toString(methodGroup));
		m_UserIdCache = new LruCache<String, User>("userservice-userid-cache");
		m_AccessIdCache = new LruCache<String, User>("userservice-accessid-cache");
		GcCleaner.register(m_UserIdCache);
		GcCleaner.register(m_AccessIdCache);
	}

	@Override
	public User getUser(String id) {
		LruCache<String, User> cache = m_UserIdCache;
		User user = cache.get(id);
		if (null == user) {
			ServiceInvoker invoker = getInvoker();
			SimpleDtObject params = new SimpleDtObject();
			params.put("id", SimpleDtString.valueOf(id));
			Response response = invoker.invoke(genMethod("getUser"), params);
			FriendlyObject content = getContent(response);
			user = getUser(content);
			User old = cache.putIfAbsent(id, user);
			if (null != old) {
				user = old;
			}
		}
		return user;
	}

	@Override
	public User getUserByAccess(String accessId) {
		LruCache<String, User> cache = m_AccessIdCache;
		User user = cache.get(accessId);
		if (null == user) {
			ServiceInvoker invoker = getInvoker();
			SimpleDtObject params = new SimpleDtObject();
			params.put("access_id", SimpleDtString.valueOf(accessId));
			Response response = invoker.invoke(genMethod("getUserByAccess"), params);
			FriendlyObject content = getContent(response);
			user = getUser(content);
			User old = cache.putIfAbsent(accessId, user);
			if (null != old) {
				user = old;
			}
		}
		return user;
	}

	/* 获取调用器 */
	protected ServiceInvoker getInvoker() {
		if (null == m_Invoker) {
			m_Invoker = ServiceInvokerFactory.create(m_ServiceName, m_ApiUrl, m_AccessId, m_AccessKey);
		}
		return m_Invoker;
	}

	/* 生成方法名 */
	protected String genMethod(String method) {
		return m_MethodGroup + NamingConverter.camelToWf(method);
	}

	/* 获取内容 */
	protected FriendlyObject getContent(Response response) {
		if (response.getResponseCode() != 0) {
			throw new RuntimeException("网关异常:" + response.getResponseCode() + "/" + response.getResponseMsg());
		}
		FriendlyObject result = FriendlyObject.valueOf(response.getServiceResult());
		if (0 != result.getInt("code", -1)) {
			throw new RuntimeException("业务异常:" + result.getInt("code", -1) + "/" + result.getString("msg"));
		}
		return result.getFriendlyObject("content");
	}

	/* 获取用户 */
	protected User getUser(FriendlyObject content) {
		if (content.isNull()) {
			return null;
		}
		SimpleUser user = new SimpleUser(content.getString("id"), content.getString("name"),
				getRight(content.getFriendlyList("right")));
		return user;
	}

	/* 获取权限 */
	protected List<Right> getRight(FriendlyList list) {
		List<Right> result = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			result.add(getRight(list.getFriendlyObject(i)));
		}
		return result;
	}

	/* 获取权限 */
	protected Right getRight(FriendlyObject object) {
		if (object.isNull()) {
			return null;
		}
		return new SimpleRight((short) object.getInt("rule"), object.getString("uriPattern"));
	}

	/*
	 * 用户实现
	 */
	protected static class SimpleUser implements User {

		protected String m_Id;

		protected String m_Name;

		protected String m_Password;

		protected List<Right> m_Right;

		public SimpleUser() {
		}

		public SimpleUser(String id, String name, List<Right> right) {
			m_Id = id;
			m_Name = name;
			m_Right = right;
		}

		@Override
		public String getId() {
			return m_Id;
		}

		public void setId(String id) {
			m_Id = id;
		}

		@Override
		public String getName() {
			return m_Name;
		}

		public void setName(String name) {
			m_Name = name;
		}

		@Override
		public List<Right> getRight() {
			return m_Right;
		}

		@SuppressWarnings("unchecked")
		public void setRight(List<SimpleRight> right) {
			List<? extends Right> list = right;
			m_Right = (List<Right>) list;
		}

		@Override
		public List<Right> getRights() {
			return m_Right;
		}

		@Override
		public List<Role> getRoles() {
			return Collections.emptyList();
		}

		@Override
		public String toString() {
			return getId() + "!" + getName();
		}

	}

	/*
	 * 权限实现
	 */
	protected static class SimpleRight implements Right {

		protected short m_Rule;

		protected String m_UriPattern;

		public SimpleRight() {
		}

		public SimpleRight(short rule, String uriPattern) {
			m_Rule = rule;
			m_UriPattern = uriPattern;
		}

		@Override
		public short getRule() {
			return m_Rule;
		}

		public void setRule(short rule) {
			m_Rule = rule;
		}

		@Override
		public String getUriPattern() {
			return m_UriPattern;
		}

		public void setUriPattern(String uri) {
			m_UriPattern = uri;
		}

		@Override
		public String toString() {
			return (m_Rule == RULE_DISALLOW ? "[x]" : (m_Rule == RULE_ALLOW ? "[√]" : "[-]")) + m_UriPattern;
		}

	}

}
