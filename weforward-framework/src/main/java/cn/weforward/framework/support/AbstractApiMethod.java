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

import cn.weforward.framework.ApiMethod;
import cn.weforward.framework.WeforwardSession;

/**
 * 抽象方法实现
 * 
 * @author daibo
 *
 */
public abstract class AbstractApiMethod implements ApiMethod {
	/** 方法名 */
	protected String m_Name;
	/** 种类 */
	protected String m_Kind;

	public AbstractApiMethod(String name) {
		this(name, null);
	}

	public AbstractApiMethod(String name, String kind) {
		m_Name = name;
		m_Kind = kind;
	}

	public void setKind(String kind) {
		m_Kind = kind;
	}

	public boolean isAllow(WeforwardSession user) {
		return true;
	}

	@Override
	public String getName() {
		return m_Name;
	}

	public String getKind() {
		return m_Kind;
	}
}
