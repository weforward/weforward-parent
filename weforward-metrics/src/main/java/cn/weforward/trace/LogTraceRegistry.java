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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.trace.ext.AbstractTraceRegistry;

/**
 * 基于日志的追踪登记
 * 
 * @author daibo
 *
 */
public class LogTraceRegistry extends AbstractTraceRegistry {
	/** 日志 */
	private static final Logger _Logger = LoggerFactory.getLogger(LogTraceRegistry.class);

	@Override
	protected void send(Trace vo) throws Exception {
		Appendable sb = new StringBuilder();
		out(sb, vo);
		_Logger.info(sb.toString());
	}

}
