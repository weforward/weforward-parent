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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import cn.weforward.common.util.ListUtil;
import cn.weforward.common.util.StringUtil;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.prometheus.PrometheusNamingConvention;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.exporter.common.TextFormat;

public class PrometheusCollector implements MetricsCollector {

	private List<MetricFamilySamples> m_List;

	private NamingConvention m_NamingConvention;

	public PrometheusCollector() {
		m_List = new ArrayList<>();
		m_NamingConvention = new PrometheusNamingConvention();
	}

	@Override
	public void write(Writer writer) throws IOException {
		List<MetricFamilySamples> list = m_List;
		if (list.size() > 10000) {// 太多了，清除掉重新来吧
			m_List = new ArrayList<>();
		}
		Enumeration<MetricFamilySamples> metricFamilySamples = new MyEnum(list.iterator());
		TextFormat.write004(writer, metricFamilySamples);
	}

	@Override
	public void collect(Id id, Iterable<Measurement> measure) {
		Collector.Type promType;
		Meter.Type type = id.getType();
		switch (type) {
		case COUNTER:
			promType = Collector.Type.COUNTER;
			break;
		case GAUGE:
			promType = Collector.Type.GAUGE;
			break;
		case DISTRIBUTION_SUMMARY:
		case TIMER:
			promType = Collector.Type.SUMMARY;
			break;
		default:
			promType = Collector.Type.UNTYPED;
		}
		String conventionName = m_NamingConvention.name(id.getName(), type, id.getBaseUnit());
		String help = StringUtil.toString(id.getDescription());
		List<String> keys = new ArrayList<>();
		List<String> values = new ArrayList<>();
		for (Tag t : id.getTags()) {
			keys.add(t.getKey());
			values.add(t.getValue());
		}
		List<Sample> samples = new ArrayList<>();
		for (Measurement m : measure) {
			String name = conventionName;
			switch (m.getStatistic()) {
			case COUNT:
				if (type != Meter.Type.COUNTER) {
					name += "_count";
				}
				break;
			case TOTAL:
			case TOTAL_TIME:
				name += "_sum";
				break;
			case MAX:
				name += "_max";
				break;
			case ACTIVE_TASKS:
				name += "_active_count";
				break;
			case DURATION:
				name += "_duration_sum";
				break;
			default:

			}
			samples.add(new Collector.MetricFamilySamples.Sample(name, keys, values, m.getValue()));
		}
		synchronized (this) {
			List<MetricFamilySamples> list = m_List;
			List<MetricFamilySamples> result = new ArrayList<>(list.size());
			MetricFamilySamples oldSamples = null;
			for (MetricFamilySamples s : list) {
				if (StringUtil.eq(s.name, conventionName) && s.type.equals(promType)) {
					oldSamples = s;
				} else {
					result.add(s);
				}
			}
			if (null == oldSamples) {
				result.add(new MetricFamilySamples(conventionName, promType, help, samples));
			} else {
				List<Sample> olds = oldSamples.samples;
				for (Sample old : olds) {
					if (contains(samples, old)) {
						continue;
					}
					samples.add(old);
				}
				result.add(new MetricFamilySamples(conventionName, promType, help, samples));
			}
			m_List = result;
		}

	}

	private boolean contains(List<Sample> samples, Sample old) {
		for (Sample s : samples) {
			if (StringUtil.eq(s.name, old.name) && ListUtil.eq(s.labelNames, old.labelNames)
					&& ListUtil.eq(s.labelValues, old.labelValues)) {
				return true;
			}
		}
		return false;
	}

	private class MyEnum implements Enumeration<MetricFamilySamples> {
		private Iterator<MetricFamilySamples> m_It;

		MyEnum(Iterator<MetricFamilySamples> it) {
			m_It = it;
		}

		@Override
		public boolean hasMoreElements() {
			return m_It.hasNext();
		}

		@Override
		public MetricFamilySamples nextElement() {
			return m_It.next();
		}
	}
}
