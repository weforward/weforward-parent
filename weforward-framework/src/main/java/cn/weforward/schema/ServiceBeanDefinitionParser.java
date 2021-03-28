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
package cn.weforward.schema;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

import cn.weforward.common.util.StringUtil;
import cn.weforward.framework.ext.WeforwardService;

/**
 * 服务bean定义解析
 * 
 * @author daibo
 *
 */
public class ServiceBeanDefinitionParser extends WeforwardBeanDefinitionParser {

	private static final Map<String, String> DEFAULT_VALUE = new HashMap<>();

	static {
		DEFAULT_VALUE.put("no", "${weforward.serverid}");
		DEFAULT_VALUE.put("servicesUrl", "${weforward.apiUrl}");
		DEFAULT_VALUE.put("accessId", "${weforward.service.accessId}");
		DEFAULT_VALUE.put("accessKey", "${weforward.service.accessKey}");
		DEFAULT_VALUE.put("meterRegistryUrl", "${weforward.meter.url:}");
		DEFAULT_VALUE.put("traceRegisterUrl", "${weforward.trace.url:}");
		DEFAULT_VALUE.put("innerMethodEnabled", "true");
		DEFAULT_VALUE.put("methodsAwareEnabled", "true");
	}

	@Override
	protected String getDefaultId() {
		return "wf-service";
	}

	protected ServiceBeanDefinitionParser() {
		super(WeforwardService.class);
	}

	@Override
	protected void addConstructor(Element element, BeanDefinitionBuilder builder) {
		String path = element.getAttribute("path");
		builder.addConstructorArgValue(getAttribute(element, "name", "${weforward.name}"));
		builder.addConstructorArgValue(getAttribute(element, "host", "${weforward.host}"));
		builder.addConstructorArgValue(getAttribute(element, "port", "${weforward.port}"));
		if (!StringUtil.isEmpty(path)) {
			builder.addConstructorArgValue(path);
		}
	}

	@Override
	protected String getDefaultValue(String fileName) {
		return DEFAULT_VALUE.get(fileName);
	}

}
