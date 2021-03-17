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
package cn.weforward.framework.doc;

import java.util.concurrent.atomic.AtomicInteger;

import cn.weforward.protocol.StatusCode;
import cn.weforward.protocol.client.execption.MicroserviceException;
import cn.weforward.protocol.support.CommonServiceCodes;

/**
 * 通用的微服务状态码抽象实现
 * 
 * @author daibo
 *
 */
public class AbstractServiceCodes extends CommonServiceCodes {
	/** 基础code */
	protected int m_BaseCode;
	/** 当前code */
	protected AtomicInteger m_CurrentCode;

	public AbstractServiceCodes() {
		this(MicroserviceException.CUSTOM_CODE_START);
	}

	public AbstractServiceCodes(int code) {
		m_BaseCode = code;
		m_CurrentCode = new AtomicInteger(code);
	}

	/**
	 * 基础code
	 * 
	 * @return code
	 */
	public int getBaseCode() {
		return m_BaseCode;
	}

	/**
	 * 当前code
	 * 
	 * @return code
	 */
	public int getCurrentCode() {
		return m_CurrentCode.get();
	}

	/**
	 * 创建异常码(通过basecode+stepcode)
	 * 
	 * @param stepcode 步长
	 * @param msg      信息
	 * @return 状态码
	 */
	public StatusCode createCode(int stepcode, String msg) {
		StatusCode code = new StatusCode(m_BaseCode + stepcode, msg);
		append(code);
		return code;
	}

	/**
	 * 生成异常码(当前code加1)
	 * 
	 * @param msg 信息
	 * @return 状态码
	 */
	public StatusCode genCode(String msg) {
		StatusCode code = new StatusCode(m_CurrentCode.getAndIncrement(), msg);
		append(code);
		return code;
	}
}
