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
package cn.weforward.framework.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import ch.qos.logback.classic.Level;
import cn.weforward.common.util.StringUtil;
import cn.weforward.framework.ext.WeforwardScript;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.support.datatype.FriendlyObject;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 日志脚本
 * 
 * @author daibo
 *
 */
public class LogScript implements WeforwardScript {

	@Override
	public DtObject handle(ApplicationContext context, String path, FriendlyObject params) {
		SimpleDtObject result = new SimpleDtObject();
		String name = params.getString("name");
		if (StringUtil.isEmpty(name)) {
			result.put("message", "请输出[name]参数");
			return result;
		}
		Logger logger = LoggerFactory.getLogger(name);
		if (null == logger) {
			result.put("message", "找不到[" + name + "]Logger");
			return result;
		}
		if (logger instanceof ch.qos.logback.classic.Logger) {
			ch.qos.logback.classic.Logger l = (ch.qos.logback.classic.Logger) logger;
			String level = params.getString("level");
			if (StringUtil.isEmpty(level)) {
				result.put("message", "请输入level=t|d|i|w|e|o");
			} else {
				if ("t".equals(level)) {
					l.setLevel(Level.TRACE);
				} else if ("d".equals(level)) {
					l.setLevel(Level.DEBUG);
				} else if ("i".equals(level)) {
					l.setLevel(Level.INFO);
				} else if ("w".equals(level)) {
					l.setLevel(Level.WARN);
				} else if ("e".equals(level)) {
					l.setLevel(Level.ERROR);
				} else if ("o".equals(level)) {
					l.setLevel(Level.OFF);
				}
			}
			result.put("level", StringUtil.toString(l.getLevel()));
			result.put("name", logger.getName());
		} else {
			result.put("message", "[" + name + "]非logback实例");
			return result;
		}

		return result;
	}

}
