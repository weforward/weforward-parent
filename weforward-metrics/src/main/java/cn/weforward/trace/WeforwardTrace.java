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

import java.util.ArrayList;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * weforward追踪块
 * 
 * @author daibo
 *
 */
public interface WeforwardTrace {
	/** 类型-网关 */
	String KIND_GATEWAY = "GATEWAY";
	/** 类型-服务 */
	String KIND_SERVICE = "SERVICE";
	/** 标签-服务id */
	String LABEL_SERVICE_ID = "serviceId";
	/** 标签-服务编号 */
	String LABEL_SERVICE_NO = "serviceNo";
	/** 标签-服务名 */
	String LABEL_SERVICE_NAME = "serviceName";
	/** 标签-服务版本号 */
	String LABEL_SERVICE_VERSION = "serviceVersion";
	/** 标签-方法名 */
	String LABEL_METHOD_NAME = "method";

	static TagHelper TAG = new TagHelper();

	static class TagHelper {

		public static final Tag serviceId(String id) {
			if (null == id) {
				return null;
			}
			return new ImmutableTag(LABEL_SERVICE_ID, id);
		}

		public static final Tag serviceNo(String no) {
			if (null == no) {
				return null;
			}
			return new ImmutableTag(LABEL_SERVICE_NO, no);
		}

		public static final Tag serviceName(String name) {
			if (null == name) {
				return null;
			}
			return new ImmutableTag(LABEL_SERVICE_NAME, name);
		}

		public static final Tag method(String name) {
			if (null == name) {
				return null;
			}
			return new ImmutableTag(LABEL_METHOD_NAME, name);
		}

		public static final Tags of(Tag... tags) {
			if (tags == null || tags.length == 0) {
				return Tags.empty();
			}
			int idx = -1;
			for (int i = 0; i < tags.length; i++) {
				if (null == tags[i]) {
					idx = i;
					break;
				}
			}
			if (-1 == idx) {
				return Tags.of(tags);
			}
			ArrayList<Tag> list = new ArrayList<>(tags.length);
			for (Tag tag : tags) {
				if (null != tag) {
					list.add(tag);
				}
			}
			return Tags.of(list);
		}
	}
}
