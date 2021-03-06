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

import java.io.IOException;

/**
 * 业务处理器的工厂
 * 
 * @author liangyi
 *
 */
public interface ServerHandlerFactory {
	/**
	 * 创建/指定处理的服务端业务处理器
	 * 
	 * @param context 服务端上下文
	 * @return 业务处理器
	 */
	ServerHandler handle(ServerContext context) throws IOException;

	/**
	 * 未指定前占位用
	 */
	static ServerHandlerFactory _unassigned = new ServerHandlerFactory() {
		@Override
		public ServerHandler handle(ServerContext context) {
			return null;
		}

		@Override
		public String toString() {
			return "_unassigned";
		}
	};
}
