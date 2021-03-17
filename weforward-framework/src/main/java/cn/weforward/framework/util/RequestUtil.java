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

import cn.weforward.protocol.Request;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.datatype.DtString;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * DtObject工具类
 * 
 * @author daibo
 *
 */
public class RequestUtil {
	/** 空对象 */
	private static final DtObject DT_EMPTY = new SimpleDtObject();

	/**
	 * 获取方法
	 * 
	 * @param request 请求
	 * @return 方法名
	 */
	public static String getMethod(Request request) {
		if (null == request) {
			return null;
		}
		DtObject invoke = request.getServiceInvoke();
		if (null == invoke) {
			return null;
		}
		DtString v = invoke.getString("method");
		return null == v ? null : v.value();
	}

	/**
	 * 获取参数
	 * 
	 * @param request 请求
	 * @return 数据
	 */
	public static DtObject getParams(Request request) {
		DtObject invoke = request.getServiceInvoke();
		if (null == invoke) {
			return DT_EMPTY;
		}
		DtObject v = invoke.getObject("params");
		if (null == v) {
			return DT_EMPTY;
		}
		return v;
	}

}
