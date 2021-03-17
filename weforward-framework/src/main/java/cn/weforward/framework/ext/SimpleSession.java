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

import java.util.function.Supplier;

import cn.weforward.framework.WeforwardSession;
import cn.weforward.protocol.Access;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.ops.User;

/**
 * 简单的会话
 * 
 * @author daibo
 *
 */
public class SimpleSession implements WeforwardSession {
	/** 请求 */
	protected Request m_Request;
	/** 用户 */
	protected Supplier<User> m_User;

	public SimpleSession(Request request) {
		m_Request = request;

	}

	@Override
	public String getAccessId() {
		Access access = m_Request.getAccess();
		return null == access ? null : access.getAccessId();
	}

	@Override
	public String getTenant() {
		Access access = m_Request.getAccess();
		return null == access ? null : access.getTenant();
	}

	@Override
	public String getOpenid() {
		Access access = m_Request.getAccess();
		return null == access ? null : access.getOpenid();
	}

	@Override
	public String getIp() {
		return m_Request.getAddr();
	}

	@Override
	public void bindOperator(User user) {
		bindOperator(() -> {
			return user;
		});
	}

	@Override
	public void bindOperator(Supplier<User> user) {
		m_User = user;
	}

	@Override
	public User getOperator() {
		return null == m_User ? null : m_User.get();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends User> E getUser() {
		return (E) getOperator();
	}

	@Override
	public boolean isSupportForward() {
		return m_Request.isMark(Request.MARK_SUPPORT_FORWARD);
	}

}
