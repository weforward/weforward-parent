package cn.weforward.framework.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.weforward.common.execption.DuplicateNameException;
import cn.weforward.common.util.UriMatcher;

/**
 * UriHandler集合
 * 
 * @author zhangpengji
 *
 */
class UriHandlers {

	Map<String, UriHandler> m_Handlers;
	UriMatcher m_UriMatcher;

	UriHandlers() {
		m_Handlers = Collections.emptyMap();
	}

	synchronized void add(UriHandler handler) {
		// cow
		Map<String, UriHandler> map = new HashMap<>(m_Handlers);
		String uri = handler.getUri().toLowerCase();
		UriHandler old = map.get(uri);
		if (null != old && !old.equals(handler)) {
			throw new DuplicateNameException("URI重复：" + uri);
		}
		map.put(uri, handler);
		m_Handlers = map;

		if (uri.indexOf('*') >= 0) {
			// 有通配符
			if (null == m_UriMatcher) {
				m_UriMatcher = new UriMatcher();
			}
			m_UriMatcher.addUri(uri);
		}
	}

	synchronized void addAll(List<UriHandler> handlers) {
		// cow
		Map<String, UriHandler> map = new HashMap<>(m_Handlers);
		UriHandler old;
		List<String> wildcards = null;
		String uri;
		for (UriHandler handler : handlers) {
			uri = handler.getUri().toLowerCase();
			old = map.get(uri);
			if (null != old && !old.equals(handler)) {
				throw new DuplicateNameException("URI重复：" + uri);
			}
			map.put(uri, handler);

			if (uri.indexOf('*') >= 0) {
				// 有通配符
				if (null == wildcards) {
					wildcards = new ArrayList<String>();
				}
				wildcards.add(uri);
			}
		}
		m_Handlers = map;

		if (null != wildcards) {
			if (null == m_UriMatcher) {
				m_UriMatcher = new UriMatcher();
			}
			m_UriMatcher.addUris(wildcards);
		}
	}

	synchronized UriHandler remove(String uri) {
		if (null == m_Handlers.get(uri)) {
			return null;
		}
		// cow
		Map<String, UriHandler> map = new HashMap<>(m_Handlers);
		UriHandler handler = map.remove(uri);
		m_Handlers = map;

		if (null != handler && uri.indexOf('*') >= 0) {
			// 有通配符
			if (null != m_UriMatcher) {
				m_UriMatcher.removeUri(uri);
			}
		}
		return handler;
	}

	UriHandler find(String uri) {
		Map<String, UriHandler> handlers = m_Handlers;
		if (null == handlers || handlers.isEmpty()) {
			return null;
		}
		// 先在绝对URI表匹配
		UriHandler handler = handlers.get(uri);
		if (null != handler) {
			return handler;
		}
		// 没有则再试在有通配符的表匹配
		UriMatcher matcher = m_UriMatcher;
		if (null != matcher) {
			String match = matcher.match(uri);
			if (null != match) {
				handler = handlers.get(match);
				return handler;
			}
		}
		return null;
	}
}
