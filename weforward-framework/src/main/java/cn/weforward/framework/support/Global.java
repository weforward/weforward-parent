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

import java.util.Collections;

import cn.weforward.protocol.client.util.MappedUtil;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.support.datatype.FriendlyObject;

/**
 * 全局参数
 * 
 * 用来支持
 * 
 * key:value 值
 * 
 * @author daibo
 *
 */
public class Global {
	/** 参数名 */
	public static final String PARAMS_NAME = "_global";

	/** 当前线程中的会话 */
	public static final ThreadGlobal TLS = new ThreadGlobal();

	/**
	 * 在线程中的会话（若实现支持的话）
	 * 
	 * @author daibo
	 *
	 */
	public final static class ThreadGlobal {
		/** 会话对象 */
		private ThreadLocal<Global> m_Session;

		/**
		 * 置入参数
		 * 
		 * @param global 用户会话，若为null则清空
		 * @return 旧的会话
		 */
		public Global put(Global global) {
			if (null == global) {
				// 清空
				if (null == m_Session) {
					return null;
				}
				Global old = m_Session.get();
				m_Session.set(global);
				return old;
			}

			Global old;
			if (null == m_Session) {
				m_Session = new ThreadLocal<Global>();
				old = null;
			} else {
				old = m_Session.get();
			}
			m_Session.set(global);
			return old;
		}

		/**
		 * 当前参数
		 * 
		 * @return 全局参数
		 */
		public Global get() {
			return null == m_Session ? null : m_Session.get();
		}

		/**
		 * 获取值
		 * 
		 * @param key 键
		 * @return 值
		 */
		public String getValue(String key) {
			Global g = get();
			return null == g ? null : g.getValue(key);
		}

	}

	/** 参数 */
	protected DtBase m_Base;
	/** 友好对象 */
	protected FriendlyObject m_Object;

	/**
	 * 构造
	 * 
	 * @param base 数据对象
	 */
	public Global(DtBase base) {
		m_Base = base;
	}

	/**
	 * 参数
	 * 
	 * @return 数据对象
	 */
	public DtBase getBase() {
		return m_Base;
	}

	/**
	 * 对象
	 * 
	 * @return 数据对象
	 */
	public DtObject getObject() {
		if (m_Base instanceof DtObject) {
			return (DtObject) m_Base;
		}
		return null;
	}

	/**
	 * 友好对象
	 * 
	 * @return 数据对象
	 */
	public FriendlyObject getFriendlyObject() {
		if (null == m_Object) {
			m_Object = FriendlyObject.valueOf(getObject());
		}
		return m_Object;
	}

	/**
	 * 获取key列表
	 * 
	 * @return 列表
	 */
	public Iterable<String> keys() {
		DtObject v = getObject();
		if (null == v) {
			return Collections.emptyList();
		}
		return Collections.list(v.getAttributeNames());
	}

	/**
	 * 获取值
	 * 
	 * @param name 键
	 * @return 值
	 */
	public String getValue(String name) {
		return getFriendlyObject().getString(name);
	}

	/**
	 * 获取对象
	 * 
	 * @param <E>   javabean类
	 * @param clazz 类
	 * @return 类对象
	 */
	public <E> E get(Class<E> clazz) {
		return MappedUtil.fromBase(clazz, getObject());
	}
}
