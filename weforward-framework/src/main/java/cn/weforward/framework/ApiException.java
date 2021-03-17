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

import cn.weforward.framework.exception.ApiBusinessException;

/**
 * API异常
 * 
 * @author daibo
 *
 */
public class ApiException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** 0 成功 **/
	public final static int CODE_SUCCESS = 0;
	/** 10001 方法不存在 **/
	public final static int CODE_METHOD_NOT_EXISTS = 10001;
	/** 10002 无权限调用此方法 **/
	public final static int CODE_AUTH_FAILED = 10002;
	/** 10003 未登录 **/
	public final static int CODE_NO_LOGIN = 10003;
	/** 10004 需要验证手机 **/
	public final static int CODE_NEED_VERIFY_PHONE = 10004;
	/** 20001 参数不合法 **/
	public final static int CODE_ILLEGAL_ARGUMENT = 20001;
	/** 30001 内部错误 **/
	public final static int CODE_INTERNAL_ERROR = 30001;
	/** 业务错误 {@link ApiBusinessException} **/
	public final static int CODE_BUSINESS_ERROR = 100000000;

	/** 未登录 **/
	public static final ApiException NO_LOGIN = new ApiException(CODE_NO_LOGIN, "未登录");
	/** 身份验证失败 **/
	public static final ApiException AUTH_FAILED = new ApiException(CODE_AUTH_FAILED, "无权限调用此方法");
	/** 方法不存在 **/
	public static final ApiException METHOD_NOT_EXISTS = new ApiException(CODE_METHOD_NOT_EXISTS, "方法不存在");
	/** 访问凭证类型不匹配 **/
	public static final ApiException METHOD_KIND_NO_MATCH = new ApiException(CODE_AUTH_FAILED, "访问凭证类型不匹配");
	/** 错误码 */
	protected int m_Code;
	/** 错误信息 */
	protected String m_Message;

	public ApiException(int code, Throwable e) {
		this(code, e.getMessage(), e);
	}

	public ApiException(int code, String message) {
		this(code, message, null);
	}

	public ApiException(int code, String message, Throwable e) {
		super(genTip(code, message), e);
		m_Code = code;
		m_Message = message;
	}

	public int getCode() {
		return m_Code;
	}

	public String getMessage() {
		return m_Message;
	}

	public static String genTip(int code, String message) {
		return code + "/" + message;
	}

	@Override
	public String toString() {
		return genTip(m_Code, m_Message);
	}

}
