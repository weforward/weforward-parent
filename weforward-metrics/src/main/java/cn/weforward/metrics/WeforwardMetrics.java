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
package cn.weforward.metrics;

import java.util.ArrayList;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * weforward指标
 * 
 * @author daibo
 *
 */
public interface WeforwardMetrics {
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
	/** 标签-状态码 */
	String LABEL_STATUS_CODE = "code";
	/** 标签-追踪链的唯一标识 */
	String LABEL_TRACE_ID = "traceId";
	/** 标签-追踪链中的spanId */
	String LABEL_TRACE_SPAN_ID = "traceSpanId";
	/** 标签-追踪链中的parentId */
	String LABEL_TRACE_PARENT_ID = "traceParentId";
	/** 标签-网关标识 */
	String LABEL_GATEWAY_ID = "gatewayId";
	/** 标签-开始时间 */
	String LABEL_START_TIME_MS = "startTimeMillisecond";
	/** 标签-结束时间 */
	String LABEL_END_TIME_MS = "endTimeMillisecond";
	/** RPC请求数统计值 */
	String RPC_REQUEST_KEY = "weforward.service.rpc.requests";
	/** 当前 RPC并发数统计值 */
	String RPC_CURRENT_REQUEST_KEY = "weforward.service.rpc.current.request";
	/** stream请求数统计值 */
	String STREAM_REQUEST_KEY = "weforward.service.stream.requests";
	/** 当前stream并发数统计值 */
	String STREAM_CURRENT_REQUEST_KEY = "weforward.service.stream.current.request";
	/** 追踪统计值-开始时间 */
	String TRACE_START_TIME = "weforward.service.trace.start";
	/** 追踪统计值-结束时间 */
	String TRACE_END_TIME = "weforward.service.trace.end";
	/** 追踪统计值 */
	String TRACE_KEY = "weforward.service.trace";

	/** 内存上限 */
	String MEMORY_MAX = "weforward.service.memorymax";
	/** 已分配的内存 */
	String MEMORY_ALLOC = "weforward.service.memoryalloc";
	/** 已使用的内存 */
	String MEMORY_USED = "weforward.service.memoryused";
	/** Full-GC次数 */
	String GC_FULL_COUNT = "weforward.service.gcfullcount";
	/** Full-GC消耗时间（秒） */
	String GC_FULL_TIME = "weforward.service.gcfulltime";
	/** 线程数 */
	String THREAD_COUNT = "weforward.service.threadcount";
	/** CPU使用率 */
	String CPU_USAGE_RATE = "weforward.service.cpuusagerate";
	/** 微服务启动时间 */
	String START_TIME = "weforward.service.starttime";
	/** 网关运行持续时间 */
	String UP_TIME = "weforward.service.uptime";

	/** 网关内存上限 */
	String GATEWAY_MEMORY_MAX = "weforward.gateway.memorymax";
	/** 网关已使用的内存 */
	String GATEWAY_MEMORY_USED = "weforward.gateway.memoryused";
	/** 网关已分配的内存 */
	String GATEWAY_MEMORY_ALLOC = "weforward.gateway.memoryalloc";
	/** 网关Full-GC次数 */
	String GATEWAY_GC_FULL_COUNT = "weforward.gateway.gcfullcount";
	/** 网关Full-GC消耗时间（秒） */
	String GATEWAY_GC_FULL_TIME = "weforward.gateway.gcfulltime";
	/** 网关线程数 */
	String GATEWAY_THREAD_COUNT = "weforward.gateway.threadcount";
	/** 网关CPU使用率 */
	String GATEWAY_CPU_USAGE_RATE = "weforward.gateway.cpuusagerate";
	/** 网关启动时间 */
	String GATEWAY_START_TIME = "weforward.gateway.starttime";
	/** 网关运行持续时间 */
	String GATEWAY_UP_TIME = "weforward.gateway.uptime";
	/** 网关RPC次数 */
	String GATEWAY_RPC_COUNT = "weforward.gateway.rpccount";
	/** 网关RPC并发数 */
	String GATEWAY_RPC_CONCURRENT = "weforward.gateway.rpcconcurrent";
	/** 网关STREAM次数 */
	String GATEWAY_STREAM_COUNT = "weforward.gateway.streamcount";
	/** 网关STREAM并发数 */
	String GATEWAY_STREAM_CONCURRENT = "weforward.gateway.streamconcurrent";
	/** 网关追踪统计值 */
	String GATEWAY_TRACE_KEY = "weforward.gateway.trace";
	/** （网关中）服务RPC请求数 */
	String GATEWAY_SERVICE_RPC_COUNT = "weforward.gateway.service.rpc.count";
	/** （网关中）服务RPC并发数 */
	String GATEWAY_SERVICE_RPC_CONCURRENT = "weforward.gateway.service.rpc.concurrent";
	/** （网关中）服务RPC请求失败数 */
	String GATEWAY_SERVICE_RPC_FAIL = "weforward.gateway.service.rpc.fail";
	
	/**
	 * 一次性指标标识
	 */
	Tag ONE_METRICS_TAG = new Tag() {

		@Override
		public String getValue() {
			return "";
		}

		@Override
		public String getKey() {
			return "";
		}
	};

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

		public static final Tag code(int code) {
			return new ImmutableTag(LABEL_STATUS_CODE, String.valueOf(code));
		}

		public static final Tag code(String code) {
			if (null == code) {
				return null;
			}
			return new ImmutableTag(LABEL_STATUS_CODE, code);
		}

		public static final Tag traceId(String traceId) {
			if (null == traceId) {
				return null;
			}
			return new ImmutableTag(LABEL_TRACE_ID, traceId);
		}

		public static final Tag traceSpanId(String spanId) {
			if (null == spanId) {
				return null;
			}
			return new ImmutableTag(LABEL_TRACE_SPAN_ID, spanId);
		}

		public static final Tag traceParentId(String parentId) {
			if (null == parentId) {
				return null;
			}
			return new ImmutableTag(LABEL_TRACE_PARENT_ID, parentId);
		}

		public static final Tag gatewayId(String id) {
			if (null == id) {
				return null;
			}
			return new ImmutableTag(LABEL_GATEWAY_ID, id);
		}

		public static final Tag startTimeMs(long v) {
			return startTimeMs(String.valueOf(v));
		}

		public static final Tag startTimeMs(String id) {
			if (null == id) {
				return null;
			}
			return new ImmutableTag(LABEL_START_TIME_MS, id);
		}

		public static final Tag endTimeMs(long v) {
			return endTimeMs(String.valueOf(v));
		}

		public static final Tag endTimeMs(String id) {
			if (null == id) {
				return null;
			}
			return new ImmutableTag(LABEL_END_TIME_MS, id);
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
