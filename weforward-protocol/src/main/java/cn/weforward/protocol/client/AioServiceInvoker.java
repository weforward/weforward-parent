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
package cn.weforward.protocol.client;

import cn.weforward.protocol.Request;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.execption.ServiceInvokeException;

/**
 * 基于aio的服务调整器
 * 
 * @author daibo
 *
 */
public interface AioServiceInvoker extends ServiceInvoker {
	/**
	 * 调用
	 * 
	 * @param request 调用请求
	 * @throws ServiceInvokeException 当出现网络异常，或者状态非成功时抛出
	 */
	void invoke(Request request, Listener listener) throws ServiceInvokeException;

	/**
	 * 监听异步调用，可能收到的事件，失败：--&gt;fail--&gt;complete，成功：--&gt;success--&gt;complete
	 * 
	 * @author liangyi
	 *
	 */
	interface Listener {
		/**
		 * 成功
		 * 
		 * @param request  调用请求
		 * @param response 调用成功的响应
		 */
		void success(Request request, Response response);

		/**
		 * 失败
		 * 
		 * @param request   调用请求
		 * @param throwable 相关异常（可能为null）
		 */
		void fail(Request request, Throwable throwable);

		/**
		 * 执行结束
		 * 
		 * @param request 调用请求
		 */
		void complete(Request request);
	}

}
