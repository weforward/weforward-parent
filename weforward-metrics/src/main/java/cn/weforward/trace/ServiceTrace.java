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

import java.util.Arrays;
import java.util.List;

import cn.weforward.trace.ext.AbstractTrace;
import io.micrometer.core.instrument.Tag;

/**
 * 服务追踪对象
 * 
 * @author daibo
 *
 */
public class ServiceTrace extends AbstractTrace {

	protected ServiceTrace(String id, String parentId, String traceId, long timestamp, long duration, String kind,
			Iterable<Tag> tags) {
		super(id, parentId, traceId, timestamp, duration, kind, tags);
	}

	public static Trace newTrace(String id, String parentId, String traceId, long timestamp, long duration) {
		return newTrace(id, parentId, traceId, timestamp, duration, (List<Tag>) null);
	}

	public static Trace newTrace(String id, String parentId, String traceId, long timestamp, long duration,
			Tag... tags) {
		return newTrace(id, parentId, traceId, timestamp, duration, Arrays.asList(tags));
	}

	public static Trace newTrace(String id, String parentId, String traceId, long timestamp, long duration,
			Iterable<Tag> tags) {
		return new ServiceTrace(id, parentId, traceId, timestamp, duration, WeforwardTrace.KIND_SERVICE, tags);
	}

}
