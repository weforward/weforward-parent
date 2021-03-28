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
package cn.weforward.metrics.ext;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.KvPair;
import cn.weforward.common.json.JsonUtil;
import cn.weforward.common.util.SimpleKvPair;
import cn.weforward.common.util.StringUtil;
import cn.weforward.metrics.RemoteMeterRegistry;
import cn.weforward.metrics.WeforwardMetrics;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.micrometer.core.instrument.util.NamedThreadFactory;

/**
 * 抽象指标注册表实现
 * 
 * @author daibo
 *
 */
public abstract class AbstarctMeterRegistry extends StepMeterRegistry {
	/** 日志记录器 */
	protected static final Logger _Logger = LoggerFactory.getLogger(RemoteMeterRegistry.class);
	/** 线程工厂 */
	protected static final NamedThreadFactory FACTORY = new NamedThreadFactory("metrics-publisher");

	protected boolean m_AddGlobal = false;

	protected boolean m_AlreadAddGlobal = false;

	static NumberFormat NF;

	static {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		NF = nf;
	}

	private static String format(double number) {
		synchronized (NF) {
			return NF.format(number);
		}

	}

	/** 一次性指标 */
	protected List<String> m_OneMetrics;

	public AbstarctMeterRegistry(StepRegistryConfig config, Clock clock, boolean global) {
		super(config, clock);
		m_AddGlobal = global;
		m_AlreadAddGlobal = false;
		start(FACTORY);
	}

	/**
	 * 启用jvm指标
	 * 
	 * @param enable 开关
	 */
	public void setEnableJvmMetrics(boolean enable) {
		if (enable) {
			InnerMetricsRegistry.JVM.bindTo(this);
		}
	}

	/**
	 * 启用处理器指标
	 * 
	 * @param enable 开关
	 */
	public void setEnableProcessorMetrics(boolean enable) {
		if (enable) {
			InnerMetricsRegistry.PROCESSOR.bindTo(this);
		}
	}

	/**
	 * 启用运行时间指标
	 * 
	 * @param enable 开关
	 */
	public void setEnableUpTimeMetrics(boolean enable) {
		if (enable) {
			InnerMetricsRegistry.UPTIME.bindTo(this);
		}
	}

	/**
	 * 服务id
	 * 
	 * @param id 服务id
	 */
	public void setServiceId(String id) {
		config().commonTags(WeforwardMetrics.LABEL_SERVICE_ID, id);
		if (m_AddGlobal) {
			Metrics.globalRegistry.config().commonTags(WeforwardMetrics.LABEL_SERVICE_ID, id);
		}
	}

	/**
	 * 服务编号
	 * 
	 * @param no 服务编号
	 */
	public void setServiceNo(String no) {
		config().commonTags(WeforwardMetrics.LABEL_SERVICE_NO, no);
		if (m_AddGlobal) {
			Metrics.globalRegistry.config().commonTags(WeforwardMetrics.LABEL_SERVICE_NO, no);
		}
	}

	/**
	 * 服务名
	 * 
	 * @param name 服务名
	 */
	public void setServiceName(String name) {
		config().commonTags(WeforwardMetrics.LABEL_SERVICE_NAME, name);
		if (m_AddGlobal) {
			Metrics.globalRegistry.config().commonTags(WeforwardMetrics.LABEL_SERVICE_NAME, name);
		}
	}

	public void setOneMetrics(List<String> metrics) {
		m_OneMetrics = metrics;
	}

	@Override
	protected void publish() {
		if (m_AddGlobal && !m_AlreadAddGlobal) {
			Metrics.addRegistry(this);
			m_AlreadAddGlobal = true;
		}
		List<Id> clears = new ArrayList<>();
		forEachMeter((m) -> {
			Meter.Id id = m.getId();
			Iterator<Measurement> it = m.measure().iterator();
			List<KvPair<String, Double>> list = new ArrayList<>();
			while (it.hasNext()) {
				Measurement e = it.next();
				if (Double.isNaN(e.getValue())) {
					continue;
				}
				list.add(SimpleKvPair.valueOf(e.getStatistic().name(), e.getValue()));
			}
			if (list.isEmpty()) {
				clears.add(id);
			} else {
				try {
					publish(id, list);
				} catch (Exception e) {
					if (_Logger.isDebugEnabled()) {
						_Logger.debug("忽略推送异常", e);
					}
				}
				if (isOne(id)) {
					clears.add(id);
				}
			}
		});
		for (Meter.Id id : clears) {
			remove(id);
			if (m_AddGlobal) {
				Metrics.globalRegistry.remove(id);
			}
		}

	}

	/* 一次性记录的对象，如追踪 */
	protected boolean isOne(Meter.Id id) {
		List<Tag> tags = id.getTags();
		if (!tags.isEmpty()) {
			for (Tag t : tags) {
				if (WeforwardMetrics.ONE_METRICS_TAG == t) {
					return true;
				}
			}
		}
		String name = id.getName();
		if (StringUtil.eq(WeforwardMetrics.TRACE_START_TIME, name)
				|| StringUtil.eq(WeforwardMetrics.TRACE_END_TIME, name)
				|| StringUtil.eq(WeforwardMetrics.TRACE_KEY, name)
				|| StringUtil.eq(WeforwardMetrics.GATEWAY_TRACE_KEY, name)) {
			return true;
		}
		List<String> ones = m_OneMetrics;
		if (null != ones && ones.contains(name)) {
			return true;
		}
		return false;
	}

	protected abstract void publish(Id id, List<KvPair<String, Double>> measure) throws IOException;

	/* 输出数据 */
	protected void out(Appendable sb, Id id, List<KvPair<String, Double>> list) throws IOException {
		sb.append('{');
		sb.append("\"id\":{");
		sb.append("\"name\":\"");
		JsonUtil.escape(id.getName(), sb);
		sb.append("\",");
		sb.append("\"description\":\"");
		JsonUtil.escape(id.getDescription(), sb);
		sb.append("\",");

		sb.append("\"baseUnit\":\"");
		JsonUtil.escape(id.getBaseUnit(), sb);
		sb.append("\",");
		sb.append("\"type\":\"");
		JsonUtil.escape(id.getType().name(), sb);
		sb.append("\"");
		sb.append("},");
		sb.append("\"tags\":{");
		List<Tag> tags = id.getTags();
		if (!tags.isEmpty()) {
			boolean first = true;
			for (int i = 0; i < tags.size(); i++) {
				Tag tag = tags.get(i);
				if (WeforwardMetrics.ONE_METRICS_TAG == tag) {
					continue;
				}
				if (first) {
					first = false;
				} else {
					sb.append(',');
				}
				sb.append("\"");
				JsonUtil.escape(tag.getKey(), sb);
				sb.append("\":\"");
				JsonUtil.escape(tag.getValue(), sb);
				sb.append("\"");
			}

		}
		sb.append("},");
		sb.append("\"measure\":{");
		KvPair<String, Double> m = list.get(0);
		sb.append("\"");
		JsonUtil.escape(m.getKey(), sb);
		sb.append("\":");
		sb.append(format(m.getValue()));
		for (int i = 1; i < list.size(); i++) {
			m = list.get(i);
			sb.append(",\"");
			JsonUtil.escape(m.getKey(), sb);
			sb.append("\":");
			sb.append(format(m.getValue()));
		}
		sb.append("}");
		sb.append('}');
	}

	@Override
	protected TimeUnit getBaseTimeUnit() {
		return TimeUnit.SECONDS;
	}

}
