/**
d * Copyright (c) 2019,2020 honintech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package cn.weforward.framework.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.util.AntPathPattern;
import cn.weforward.common.util.StringUtil;
import cn.weforward.framework.ApiException;
import cn.weforward.framework.Authorizer;
import cn.weforward.framework.WeforwardSession;
import cn.weforward.framework.util.RequestUtil;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.ops.Right;
import cn.weforward.protocol.ops.User;
import cn.weforward.protocol.ops.UserService;
import cn.weforward.protocol.support.NamingConverter;

/**
 * 验证器
 * 
 * @author daibo
 *
 */
public class UserAuthorizer implements Authorizer {
	/** 日志记录器 */
	private static final Logger _Logger = LoggerFactory.getLogger(UserAuthorizer.class);

	/** 运维人员服务 */
	protected UserService m_UserService;
	/** 忽略需登录验证的URI列表 */
	protected List<String> m_IgnoreUris;
	/** 忽略检查权限的验证（需要先登陆） */
	protected List<String> m_IgnoreCheckRightUris;
	/** 选项 */
	protected int m_Options;
	/** 默认权限 */
	protected int m_DefaultRightRule = Right.RULE_DISALLOW;

	/**
	 * 构造验证器
	 * 
	 */
	public UserAuthorizer() {
	}

	/**
	 * 设置默认权限
	 * 
	 * @param rule 规则
	 */
	public void setDefaultRightRule(String rule) {
		if ("allow".equalsIgnoreCase(rule)) {
			m_DefaultRightRule = Right.RULE_ALLOW;
		} else if ("disallow".equalsIgnoreCase(rule)) {
			m_DefaultRightRule = Right.RULE_DISALLOW;
		} else {
			m_DefaultRightRule = Right.RULE_NONE;
		}

	}

	/**
	 * 注入UserService
	 * 
	 * @param service 服务
	 */
	public void setUserService(UserService service) {
		m_UserService = service;
	}

	/**
	 * 设置忽略需登录验证的URI列表，支持URI最后使用“*”作为通配符（如“/admin/*”）
	 * 
	 * @param uris URI列表
	 */
	public void setIgnoreUris(List<String> uris) {
		if (uris == null) {
			m_IgnoreUris = Collections.emptyList();
		} else {
			m_IgnoreUris = new ArrayList<>(uris);
			Collections.sort(m_IgnoreUris, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return o1.compareToIgnoreCase(o2);
				}
			});
		}
	}

	/**
	 * 忽略检查权限的验证的URI列表（需要先登陆），支持URI最后使用“*”作为通配符（如“/admin/*”）
	 * 
	 * @param uris URI列表
	 */
	public void setIgnoreCheckRightUris(List<String> uris) {
		if (uris == null) {
			m_IgnoreCheckRightUris = Collections.emptyList();
		} else {
			m_IgnoreCheckRightUris = new ArrayList<>(uris);
			Collections.sort(m_IgnoreCheckRightUris, new Comparator<String>() {
				public int compare(String o1, String o2) {
					return o1.compareToIgnoreCase(o2);
				}
			});
		}
	}

	@Override
	public void auth(Request request) throws ApiException {
		String uri = RequestUtil.getMethod(request);
		if (StringUtil.isEmpty(uri)) {
			throw new ApiException(ApiException.CODE_METHOD_NOT_EXISTS, "方法名为空");
		}
		User user = m_UserService.getUserByAccess(request.getAccess().getAccessId());
		try {
			WeforwardSession session = WeforwardSession.TLS.getSession();
			if (null != session && null != user) {
				session.bindOperator(user);
			}
			// 检查URL是否不需要登录访问
			if (null != match(m_IgnoreUris, uri)) {
				return;
			}
			if (null == user) {
				throw ApiException.NO_LOGIN;
			}
			// 是否不需要检查权限
			if (null != match(m_IgnoreCheckRightUris, uri)) {
				return;
			}
			if (!checkRight(user, uri)) {
				_Logger.error("验证失败:" + uri + "," + user.getName() + "," + user.getRights());
				throw ApiException.AUTH_FAILED;
			}
		} finally {
		}
	}

	/*
	 * 是否匹配
	 * 
	 * @param patterns
	 * 
	 * @param uri
	 * 
	 * @return
	 */
	private static String match(List<String> patterns, String uri) {
		if (null == patterns) {
			return null;
		}
		for (String pattern : patterns) {
			if (StringUtil.isEmpty(pattern)) {
				continue;
			}
			if (match(pattern, uri)) {
				return pattern;
			}
		}
		return null;
	}

	/*
	 * 是否匹配
	 * 
	 * @param right
	 * 
	 * @param uri
	 * 
	 * @return
	 */
	private static boolean match(Right right, String uri) {
		String pattern = NamingConverter.camelToWf(right.getUriPattern());
		return match(pattern, uri);
	}

	/*
	 * 是否匹配
	 * 
	 * @param pattern
	 * 
	 * @param uri
	 * 
	 * @return
	 */
	private static boolean match(String pattern, String uri) {
		return AntPathPattern.match(pattern, uri);
	}

	/* 检查权限 */
	protected boolean checkRight(User user, String uri) {
		List<Right> rights = getRight(user);
		Right match = null;
		for (Right r : rights) {
			if (match(r, uri)) {
				match = r;
				if (r.getRule() == Right.RULE_DISALLOW) {
					break;// 禁止权限最高
				}
			}
		}
		if (null == match) {
			return false;
		}
		if (Right.RULE_ALLOW == match.getRule()) {
			return true;
		} else if (Right.RULE_DISALLOW == match.getRule()) {
			return false;
		} else {
			return m_DefaultRightRule == Right.RULE_ALLOW;
		}

	}

	/* 获取权限 */
	private List<Right> getRight(User user) {
		return user.getRights();
	}

}
