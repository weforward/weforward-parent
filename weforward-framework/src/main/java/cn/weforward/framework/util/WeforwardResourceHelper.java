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

import java.io.InputStream;

import cn.weforward.framework.WeforwardFile;
import cn.weforward.framework.WeforwardResource;

/**
 * 资源文件工具类
 * 
 * @author daibo
 *
 */
public class WeforwardResourceHelper {
	/**
	 * 构造资源
	 * 
	 * @param id            资源id
	 * @param timeoutSecond 多久后过期（单位秒）
	 * @return 资源
	 */
	public static WeforwardResource valueOf(String id, long timeoutSecond) {
		return valueOf(id, timeoutSecond, null);
	}

	/**
	 * 构造资源
	 * 
	 * @param id            资源id
	 * @param timeoutSecond 多久后过期（单位秒）
	 * @param data          附加数据
	 * @return 资源
	 */
	public static WeforwardResource valueOf(String id, long timeoutSecond, Object data) {
		long expire = (System.currentTimeMillis() / 1000) + timeoutSecond;
		return new WeforwardResourceVo(id, expire, null, data);
	}

	/**
	 * 构造资源
	 * 
	 * @param id            资源id
	 * @param timeoutSecond 多久后过期（单位秒）
	 * @param service       服务名
	 * @param data          附加数据
	 * @return 资源
	 */
	public static WeforwardResource valueOf(String id, long timeoutSecond, String service, Object data) {
		long expire = (System.currentTimeMillis() / 1000) + timeoutSecond;
		return new WeforwardResourceVo(id, expire, service, data);
	}

	/**
	 * 构造文件
	 * 
	 * @param name   文件名
	 * @param stream 输入流
	 * @return 资源
	 */
	public static WeforwardFile newFile(String name, InputStream stream) {
		return new WeforwardFileVo(name, stream);
	}

	/**
	 * 构造文件
	 * 
	 * @param name        文件名
	 * @param stream      输入流
	 * @param contentType 内容类型
	 * @return 资源
	 */
	public static WeforwardFile newFile(String name, InputStream stream, String contentType) {
		return new WeforwardFileVo(name, stream, contentType);
	}

}
