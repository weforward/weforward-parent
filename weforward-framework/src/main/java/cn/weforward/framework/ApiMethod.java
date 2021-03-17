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

import cn.weforward.protocol.Access;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtObject;

/**
 * 方法
 * 
 * @author daibo
 *
 */
public interface ApiMethod {

	/**
	 * 方法名，可以是Ant表达式，如/order,/order/**
	 * 
	 * @return 方法名
	 */
	String getName();

	/**
	 * 种类
	 * 
	 * @return {@link Access#KIND_ALL}
	 */
	String getKind();

	/**
	 * 是否允许访问
	 * 
	 * @param session 会话
	 * @return
	 */
	boolean isAllow(WeforwardSession session);

	/**
	 * 处理
	 * 
	 * @param path     请求路径
	 * @param params   请求参数
	 * @param request  请求
	 * @param response 响应
	 * @return 处理结果
	 * @throws ApiException 调用异常时抛出
	 */
	DtBase handle(String path, DtObject params, Request request, Response response) throws ApiException;
}
