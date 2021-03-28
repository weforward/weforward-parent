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
package cn.weforward.framework;

import java.util.function.Supplier;

import cn.weforward.protocol.ops.User;

/**
 * 会话
 * 
 * @author daibo
 *
 */
public interface WeforwardSession {
	/**
	 * 访问凭证
	 * 
	 * @return 凭证
	 */
	String getAccessId();

	/**
	 * 调用端ip
	 * 
	 * @return ip
	 */
	String getIp();

	/**
	 * 绑定操作员
	 * 
	 * @param user 用户
	 */
	void bindOperator(User user);

	/**
	 * 绑定操作员
	 * 
	 * @param user 用户
	 */
	void bindOperator(Supplier<User> user);

	/**
	 * 获取操作员
	 * 
	 * @return 操作员
	 */
	User getOperator();

	/**
	 * 租户标识
	 * 
	 * @return 标识
	 */
	String getTenant();

	/**
	 * 基于OAuth协议生成的用户身份
	 * 
	 * @return 用户身份
	 */
	String getOpenid();

	/**
	 * 获取当前用户
	 * 
	 * @param <E> 用户类
	 * @return 用户
	 */
	<E extends User> E getUser();

	/**
	 * 是否支持转发
	 * 
	 * @return 支持返回true
	 */
	boolean isSupportForward();

	/** 当前线程中的会话 */
	public static final ThreadSession TLS = new ThreadSession();

	/**
	 * 在线程中的用户会话（若实现支持的话）
	 * 
	 * @author daibo
	 *
	 */
	public class ThreadSession {
		/** 会话对象 */
		private ThreadLocal<WeforwardSession> m_Session;

		/**
		 * 置入用户会话
		 * 
		 * @param session 用户会话，若为null则清空
		 * @return 旧的会话
		 */
		public WeforwardSession putSession(WeforwardSession session) {
			if (null == session) {
				// 清空
				if (null == m_Session) {
					return null;
				}
				WeforwardSession old = m_Session.get();
				m_Session.set(session);
				return old;
			}

			WeforwardSession old;
			if (null == m_Session) {
				m_Session = new ThreadLocal<WeforwardSession>();
				old = null;
			} else {
				old = m_Session.get();
			}
			m_Session.set(session);
			return old;
		}

		/**
		 * 当前用户会话
		 * 
		 * @return 会话
		 */
		public WeforwardSession getSession() {
			return null == m_Session ? null : m_Session.get();
		}

		/**
		 * 调用端ip
		 * 
		 * @return ip
		 */
		public String getIp() {
			WeforwardSession s = getSession();
			return null == s ? null : s.getIp();
		}

		/**
		 * 获取操作员
		 * 
		 * @return 操作员
		 */
		public User getOperator() {
			return getUser();
		}

		/**
		 * 获取当前用户
		 * 
		 * @param <E> 用户类
		 * @return 用户
		 */
		@SuppressWarnings("unchecked")
		public <E extends User> E getUser() {
			WeforwardSession s = getSession();
			return null == s ? null : (E) s.getUser();
		}

		/**
		 * 租户标识
		 * 
		 * @return 租户标识
		 */
		public String getTenant() {
			WeforwardSession s = getSession();
			return null == s ? null : s.getTenant();
		}

		/**
		 * 基于OAuth协议生成的用户身份
		 * 
		 * @return 用户身份
		 */
		public String getOpenid() {
			WeforwardSession s = getSession();
			return null == s ? null : s.getOpenid();
		}

		/**
		 * 是否支持转发
		 * 
		 * @return 支持返回true
		 */
		public boolean isSupportForward() {
			WeforwardSession s = getSession();
			return null == s ? false : s.isSupportForward();
		}

	}

}
