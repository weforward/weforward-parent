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
package cn.weforward.framework.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.weforward.common.util.AntPathPattern;
import cn.weforward.common.util.StringUtil;
import cn.weforward.framework.ApiException;
import cn.weforward.framework.Authorizer;
import cn.weforward.framework.util.RequestUtil;
import cn.weforward.protocol.Access;
import cn.weforward.protocol.Request;

/**
 * 租户验证器
 * 
 * @author daibo
 *
 */
public class TenantAuthorizer implements Authorizer {
	/** 忽略需登录验证的URI列表 */
	protected List<String> m_IgnoreUris;

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

	@Override
	public void auth(Request request) throws ApiException {
		String uri = RequestUtil.getMethod(request);
		if (StringUtil.isEmpty(uri)) {
			throw new ApiException(ApiException.CODE_METHOD_NOT_EXISTS, "方法名为空");
		}
		Access access = request.getAccess();
		if (null == access) {
			throw ApiException.NO_LOGIN;
		}
		if (StringUtil.isEmpty(access.getTenant())) {
			throw ApiException.NO_LOGIN;
		}
		if (StringUtil.isEmpty(access.getOpenid())) {
			throw ApiException.NO_LOGIN;
		}
		// 检查URL是否不需要登录访问
		if (null != match(m_IgnoreUris, uri)) {
			return;
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
			if (match(pattern, uri)) {
				return pattern;
			}
		}
		return null;
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

}
