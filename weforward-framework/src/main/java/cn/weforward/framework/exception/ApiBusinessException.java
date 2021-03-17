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
package cn.weforward.framework.exception;

import cn.weforward.framework.ApiException;

/**
 * 业务类API异常
 * 
 * @author daibo
 *
 */
public class ApiBusinessException extends ApiException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** 业务错误码 */
	protected int m_BusinessCode;

	public ApiBusinessException(int businessCode, String message) {
		this(businessCode, message, null);
	}

	public ApiBusinessException(int businessCode, String message, Throwable e) {
		super(fix(businessCode), message, e);
		m_BusinessCode = businessCode;
	}

	/**
	 * 获取业务代码
	 * 
	 * @return 代码
	 */
	public int getBusinessCode() {
		return m_BusinessCode;
	}

	/**
	 * 将业务代码转换成浩云码
	 * 
	 * @param businessCode 业务码
	 * @return 代码
	 */
	public static int fix(int businessCode) {
		if (businessCode >= CODE_BUSINESS_ERROR) {
			throw new IllegalArgumentException("业务错误码必须小于" + CODE_BUSINESS_ERROR);
		}
		return CODE_BUSINESS_ERROR + businessCode;
	}

	/**
	 * 是否业务代码
	 * 
	 * @param code 业务码
	 * @return 是返回true
	 */
	public static boolean isBusinessCode(int code) {
		return code >= CODE_BUSINESS_ERROR;
	}

	/**
	 * 获取业务代码
	 * 
	 * @param code 业务码
	 * @return 业务码
	 */
	public static int getBusinessCode(int code) {
		return code - CODE_BUSINESS_ERROR;
	}

}
