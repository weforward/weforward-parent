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
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

public class TimerTest {
	@Test
	public void test() throws MalformedURLException {
		RemoteMeterRegistry registry = new RemoteMeterRegistry("http://127.0.0.1:1500/metrics/");
//		LogMeterRegistry registry = new LogMeterRegistry();
//		registry.enableJvmMetrics(false);
//		registry.setServiceId("x00ff");
//		registry.setServiceName("myfinaltesttest");
		MetricsCollector c = new PrometheusCollector();
		// PrometheusMeterRegistry registry = new
		// PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		Metrics.addRegistry(registry);
		int i = 1;
		while (true) {
			long amount = Math.abs(i * 100);
			Timer timer = Metrics.timer(WeforwardMetrics.RPC_REQUEST_KEY);
			timer.record(amount, TimeUnit.MILLISECONDS);
			Metrics.gauge(WeforwardMetrics.RPC_CURRENT_REQUEST_KEY, i);
			Metrics.counter("test.counter").increment();
			i++;
			// System.out.println(registry.scrape());
			// System.out.println("--------");
			for (Meter m : Metrics.globalRegistry.getMeters()) {
				c.collect(m.getId(), m.measure());
			}
			try {
				StringWriter writer = new StringWriter();
				c.write(writer);
				System.out.println(writer.toString());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			synchronized (this) {
				try {
					this.wait(61 * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
