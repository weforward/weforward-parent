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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;

/**
 * jvm指标监控
 * 
 * @author daibo
 *
 */
public interface InnerMetricsRegistry {
	/** jvm指标 */
	JvmMetrics JVM = new JvmMetrics();
	/** 处理器指标 */
	ProcessorMetrics PROCESSOR = new ProcessorMetrics();
	/** 运行时间指标 */
	UptimeMetrics UPTIME = new UptimeMetrics();

	/**
	 * jvm指标
	 * 
	 * @author daibo
	 *
	 */
	static class JvmMetrics implements MeterBinder {

		JvmGcMetrics m_JvmGcMetrics;
		JvmMemoryMetrics m_JvmMemoryMetrics;
		JvmThreadMetrics m_JvmThreadMetrics;
		ClassLoaderMetrics m_ClassLoaderMetrics;

		public JvmMetrics() {
			m_JvmGcMetrics = new JvmGcMetrics();
			m_JvmMemoryMetrics = new JvmMemoryMetrics();
			m_JvmThreadMetrics = new JvmThreadMetrics();
			m_ClassLoaderMetrics = new ClassLoaderMetrics();
		}

		@Override
		public void bindTo(MeterRegistry registry) {
			m_JvmGcMetrics.bindTo(registry);
			m_JvmMemoryMetrics.bindTo(registry);
			m_JvmThreadMetrics.bindTo(registry);
			m_ClassLoaderMetrics.bindTo(registry);
		}
	}

}
