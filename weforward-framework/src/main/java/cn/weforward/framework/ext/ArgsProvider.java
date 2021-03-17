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
package cn.weforward.framework.ext;

import cn.weforward.protocol.datatype.DtObject;

/**
 * 参数提供
 * 
 * @author daibo
 *
 */
public interface ArgsProvider {
	/**
	 * 调用方法前
	 * 
	 * @param params 参数
	 */
	void before(DtObject params);

	/**
	 * 是否要处理
	 * 
	 * @param clazz 类
	 * @return 是否要算是
	 */
	public boolean accept(Class<?> clazz);

	/**
	 * 创建参数
	 * 
	 * @param params 参数
	 * @param clazz  类
	 * @return 对象
	 */
	public Object create(DtObject params, Class<?> clazz);

	/**
	 * 调用后
	 * 
	 * @param returnObject 返回对象
	 */
	void after(Object returnObject);

}
