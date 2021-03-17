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
package cn.weforward.trace;

import io.micrometer.core.instrument.Tag;

/**
 * 追踪块
 * 
 * @author daibo
 *
 */
public interface Trace {

	/**
	 * 本次调用的唯一id
	 * 
	 * @return id
	 */
	String getId();

	/**
	 * 获取父调用id
	 * 
	 * @return id
	 */
	String getParentId();

	/**
	 * 获取追踪id
	 * 
	 * @return id
	 */
	String getTraceId();

	/**
	 * 获取调用时间戳
	 * 
	 * @return 时间戳，单位毫米
	 */
	long getTimestamp();

	/**
	 * 获取耗时
	 * 
	 * @return 耗时，单位毫米
	 */
	long getDuration();

	/**
	 * 获取类型
	 * 
	 * @return 类型
	 */
	String getKind();

	/**
	 * 标签
	 * 
	 * @return 标签列表
	 */
	Iterable<Tag> getTags();

	/**
	 * 获取标签
	 * 
	 * @param key 标签键
	 * @return 标签对应的值
	 */
	String getTag(String key);
}
