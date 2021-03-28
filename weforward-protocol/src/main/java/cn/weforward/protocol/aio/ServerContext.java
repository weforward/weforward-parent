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
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 服务端处理上下文
 * 
 * @author liangyi
 *
 */
public interface ServerContext {
	/**
	 * 请求的URI
	 */
	String getUri();

	/**
	 * 客户端地址
	 */
	String getRemoteAddr();

	/**
	 * 获取请求头
	 */
	Headers getRequestHeaders();

	/**
	 * 建立传输管道直接输出请求内容
	 * 
	 * @param writer
	 *            输出请求内容的流
	 * @param skipBytes
	 *            >0则跳过已在缓冲区的部分内容
	 */
	void requestTransferTo(OutputStream writer, int skipBytes) throws IOException;

	/**
	 * 获取请求内容流
	 * 
	 * @throws IOException
	 */
	InputStream getRequestStream() throws IOException;

	/**
	 * 设置响应超时值
	 * 
	 * @param millis
	 *            超时值（毫秒），若=Integer.MAX_VALUE则使用空闲超时值
	 */
	public void setResponseTimeout(int millis);

	/**
	 * 设置响应头
	 * 
	 * @param name
	 *            名称
	 * @param value
	 *            值（若为null则移除该项）
	 */
	void setResponseHeader(String name, String value) throws IOException;

	/**
	 * 打开响应输出
	 * 
	 * @return 响应输出流
	 */
	OutputStream openResponseWriter() throws IOException;
}
