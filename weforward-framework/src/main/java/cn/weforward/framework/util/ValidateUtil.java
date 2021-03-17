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
package cn.weforward.framework.util;

import cn.weforward.common.util.StringUtil;
import cn.weforward.framework.ApiException;

/**
 * 验证工具类
 * 
 * @author daibo
 *
 */
public class ValidateUtil {

	private ValidateUtil() {

	}

	/**
	 * 对象为空时抛出异常
	 * 
	 * @param v   对象
	 * @param tip 提示
	 * @throws ApiException 接口异常
	 */
	public static void isNull(Object v, String tip) throws ApiException {
		checkIllegalArgument(null == v, tip);
	}

	/**
	 * 对象为空或空串时抛出异常
	 * 
	 * @param v   对象
	 * @param tip 提示
	 * @throws ApiException 接口异常
	 */
	public static void isEmpty(Object v, String tip) throws ApiException {
		if (v instanceof String) {
			checkIllegalArgument(StringUtil.isEmpty((String) v), tip);
		} else {
			checkIllegalArgument(null == v, tip);
		}
	}

	/**
	 * 对象为0时抛出异常
	 * 
	 * @param number 数字
	 * @param tip    提示
	 * @throws ApiException 接口异常
	 */
	public static void isZero(int number, String tip) throws ApiException {
		checkIllegalArgument(number == 0, tip);
	}

	/**
	 * 对象大于0抛出异常
	 * 
	 * @param number 数字
	 * @param tip    提示
	 * @throws ApiException 接口异常
	 */
	public static void gtZero(int number, String tip) throws ApiException {
		checkIllegalArgument(number > 0, tip);
	}

	/**
	 * 对象大于等于0抛出异常
	 * 
	 * @param number 数字
	 * @param tip    提示
	 * @throws ApiException 接口异常
	 */
	public static void gtOrEqZero(int number, String tip) throws ApiException {
		checkIllegalArgument(number >= 0, tip);
	}

	/**
	 * 对象小于0抛出异常
	 * 
	 * @param number 数字
	 * @param tip    提示
	 * @throws ApiException 接口异常
	 */
	public static void ltZero(int number, String tip) throws ApiException {
		checkIllegalArgument(number < 0, tip);
	}

	/**
	 * 对象小于等于0抛出异常
	 * 
	 * @param number 数字
	 * @param tip    提示
	 * @throws ApiException 接口异常
	 */
	public static void ltOrEqZero(int number, String tip) throws ApiException {
		checkIllegalArgument(number <= 0, tip);
	}

	/**
	 * 检查参数异常
	 * 
	 * @param condition 条件 为true时返回异常
	 * @param message   错误信息
	 * @throws ApiException 接口异常
	 */
	public static void checkIllegalArgument(boolean condition, String message) throws ApiException {
		check(condition, ApiException.CODE_INTERNAL_ERROR, message);
	}

	/**
	 * 检查参数
	 * 
	 * @param condition 条件
	 * @param code      错误码
	 * @param message   错误信息
	 * @throws ApiException 接口异常
	 */
	public static void check(boolean condition, int code, String message) throws ApiException {
		if (condition) {
			throw new ApiException(code, message);
		}
	}
}
