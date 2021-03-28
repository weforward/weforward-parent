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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import cn.weforward.common.util.StringUtil;

/**
 * 服务bean解析
 * 
 * @author daibo
 *
 */
public abstract class WeforwardBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	protected Class<?> m_Class;

	protected WeforwardBeanDefinitionParser(Class<?> clazz) {
		m_Class = clazz;
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return m_Class;
	}

	@Override
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		String id = element.getAttribute("id");
		if (StringUtil.isEmpty(id)) {
			id = getDefaultId();
			element.setAttribute(ID_ATTRIBUTE, id);
		}
		addConstructor(element, builder);
		autoSet(element, builder);
	}

	protected abstract void addConstructor(Element element, BeanDefinitionBuilder builder);

	protected abstract String getDefaultId();

	protected abstract String getDefaultValue(String fileName);

	protected void autoSet(Element element, BeanDefinitionBuilder builder) {
		Method[] methods = getBeanClass(element).getMethods();
		if (null != methods) {
			for (Method m : methods) {
				String name = m.getName();
				if (!name.startsWith("set")) {
					continue;
				}
				Parameter[] parameter = m.getParameters();
				if (null == parameter || parameter.length != 1) {
					continue;
				}
				String fileName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
				String value = getAttribute(element, fileName);
				if (null == value) {
					continue;
				}
				Class<?> clazz = parameter[0].getType();
				if (clazz.isPrimitive() || String.class == clazz) {
					builder.addPropertyValue(fileName, value);
					continue;
				}
				if (clazz.isAssignableFrom(List.class)) {
					List<String> list = Arrays.asList(value.split(";"));
					builder.addPropertyValue(fileName, list);
					continue;
				}

				builder.addPropertyReference(fileName, value);
			}
		}

	}

	protected String getAttribute(Element element, String fileName) {
		return getAttribute(element, fileName, getDefaultValue(fileName));
	}

	protected String getAttribute(Element element, String fileName, String defaultValue) {
		Attr attr = element.getAttributeNode(fileName);
		if (null == attr) {
			return defaultValue;
		} else {
			return attr.getNodeValue();
		}
	}

}
