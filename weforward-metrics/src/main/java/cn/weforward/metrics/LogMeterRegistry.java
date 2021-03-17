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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.KvPair;
import cn.weforward.metrics.ext.AbstarctMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter.Id;

/**
 * 基于日志输出推送的监控注册表
 * 
 * @author daibo
 *
 */
public class LogMeterRegistry extends AbstarctMeterRegistry {
	/** 日志 */
	private static final Logger _Logger = LoggerFactory.getLogger(LogMeterRegistry.class);

	public LogMeterRegistry() {
		super(LogRegisterConfig.DEFAULT, Clock.SYSTEM, true);
	}

	@Override
	protected void publish(Id id, List<KvPair<String, Double>> measure) throws IOException {
		try {
			Appendable sb = new StringBuilder();
			out(sb, id, measure);
			_Logger.info(sb.toString());
		} catch (Exception e) {
			_Logger.warn("忽略推送异常", e);
		}
	}

}
