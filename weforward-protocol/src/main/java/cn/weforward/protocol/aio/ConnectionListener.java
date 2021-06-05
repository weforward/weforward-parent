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
package cn.weforward.protocol.aio;

import java.io.Closeable;

/**
 * 连接的事件监听器
 * 
 * @author liangyi
 *
 */
public interface ConnectionListener {
	/**
	 * 连接已就绪
	 * 
	 * @param context 相关的上下文
	 */
	void establish(Closeable context);

	/**
	 * 连接失败
	 * 
	 * @param url   要连接的URL
	 * @param cause 原因
	 */
	void fail(String url, Throwable cause);

	/**
	 * 连接断开
	 * 
	 * @param context 相关的上下文
	 */
	void lost(Closeable context);

	/**
	 * 未指定前占位用
	 */
	static ConnectionListener _unassigned = new ConnectionListener() {
		@Override
		public String toString() {
			return "_unassigned";
		}

		@Override
		public void establish(Closeable context) {
		}

		@Override
		public void fail(String url, Throwable cause) {
		}

		@Override
		public void lost(Closeable context) {
		}

	};
}
