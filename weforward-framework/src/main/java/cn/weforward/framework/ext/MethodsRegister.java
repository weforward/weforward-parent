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
package cn.weforward.framework.ext;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.ClassUtils;

import cn.weforward.common.util.StringUtil;
import cn.weforward.framework.ResourceDownloader;
import cn.weforward.framework.ResourceHandler;
import cn.weforward.framework.ResourceUploader;
import cn.weforward.framework.TopicListener;
import cn.weforward.framework.WeforwardAfter;
import cn.weforward.framework.WeforwardBefore;
import cn.weforward.framework.WeforwardEvent;
import cn.weforward.framework.WeforwardMethod;
import cn.weforward.framework.WeforwardMethods;
import cn.weforward.framework.WeforwardWhenException;
import cn.weforward.framework.doc.DocObjectProvider;
import cn.weforward.protocol.Access;
import cn.weforward.protocol.doc.DocObject;
import cn.weforward.protocol.doc.DocSpecialWord;
import cn.weforward.protocol.ext.ObjectMapperSet;
import cn.weforward.protocol.support.CommonServiceCodes;
import cn.weforward.protocol.support.NamingConverter;

/**
 * 方法注册
 * 
 * @author daibo
 *
 */
public class MethodsRegister {
	/** 端点 */
	protected WeforwardService m_Service;
	/** 基础路径 */
	protected String m_BasePath;
	/** 类型 */
	protected String m_Kind;
	/** 对象映射器集合 */
	protected ObjectMapperSet m_ObjectMapperSet;

	/**
	 * 构造
	 * 
	 * @param service 服务
	 */
	public MethodsRegister(WeforwardService service) {
		m_Service = service;
	}

	/**
	 * 基础路径
	 * 
	 * @param v 值
	 */
	public void setBasePath(String v) {
		m_BasePath = v;
	}

	/**
	 * 类型
	 * 
	 * @param v 值
	 */
	public void setKind(String v) {
		m_Kind = v;
	}

	/**
	 * 映射表
	 * 
	 * @param m 值
	 */
	public void setObjectMapperSet(ObjectMapperSet m) {
		m_ObjectMapperSet = m;
	}

	/**
	 * 添加对象
	 * 
	 * @param value 对象
	 */
	public void register(Object value) {
		Class<?> clazz = ClassUtils.getUserClass(value);
		if (value instanceof CommonServiceCodes) {
			m_Service.addStatusCodeClass(clazz);
		}
		if (value instanceof DocObjectProvider) {
			m_Service.registerObjectProvider((DocObjectProvider) value);
		} else if (value instanceof DocObject) {
			m_Service.registerObject((DocObject) value);
		}
		if (value instanceof DocSpecialWord) {
			m_Service.addDocSpecialWord((DocSpecialWord) value);
		}
		if (value instanceof ResourceHandler) {
			m_Service.registerResources((ResourceHandler) value);
		}
		if (value instanceof ResourceDownloader) {
			m_Service.registerResources((ResourceDownloader) value);
		}
		if (value instanceof ResourceUploader) {
			m_Service.registerResources((ResourceUploader) value);
		}
		if (value instanceof TopicListener<?>) {
			m_Service.subscribe((TopicListener<?>) value);
		}
		String cname;
		WeforwardMethods methods = clazz.getAnnotation(WeforwardMethods.class);
		if (null != methods) {
			if (methods.root()) {
				cname = StringUtil.toString(m_BasePath);
			} else {
				cname = methods.name();
				if (StringUtil.isEmpty(cname)) {
					String clazzName = clazz.getSimpleName();
					if (clazzName.endsWith("Methods")) {
						cname = Character.toLowerCase(clazzName.charAt(0))
								+ clazzName.substring(1, clazzName.length() - 7);
					} else {
						cname = Character.toLowerCase(clazzName.charAt(0)) + clazzName.substring(1);
					}
				}
				cname = NamingConverter.camelToWf(cname);
				cname = StringUtil.toString(m_BasePath) + cname + "/";
			}
		} else {
			return;
		}
		List<java.lang.reflect.Method> befores = new ArrayList<>();
		List<java.lang.reflect.Method> afters = new ArrayList<>();
		List<java.lang.reflect.Method> exceptions = new ArrayList<>();
		for (java.lang.reflect.Method m : clazz.getMethods()) {
			if (m.getAnnotation(WeforwardBefore.class) != null) {
				befores.add(m);
			}
			if (m.getAnnotation(WeforwardAfter.class) != null) {
				afters.add(m);
			}
			if (m.getAnnotation(WeforwardWhenException.class) != null) {
				exceptions.add(m);
			}
		}
		for (java.lang.reflect.Method m : clazz.getMethods()) {
			String name = null;
			String k = null;
			WeforwardMethod wmethod = m.getAnnotation(WeforwardMethod.class);
			if (null != wmethod) {
				name = wmethod.name();
				k = wmethod.kind();
				if (StringUtil.isEmpty(name)) {
					name = m.getName();
				}
				name = NamingConverter.camelToWf(name);
			} else {
				WeforwardEvent wevent = m.getAnnotation(WeforwardEvent.class);
				if (null != wevent) {
					name = wevent.name();
					k = Access.KIND_SERVICE;
					if (StringUtil.isEmpty(name)) {
						name = m.getName();
					}
					name = NamingConverter.camelToWf(name);
				}
			}
			if (StringUtil.isEmpty(name)) {
				continue;
			}
			String mname = cname + name;
			ReflectMethod method = new ReflectMethod(mname, value, m);
			if (StringUtil.isEmpty(k)) {
				k = methods.kind();
			}
			if (StringUtil.isEmpty(k)) {
				k = m_Kind;
			}
			if (!StringUtil.isEmpty(k)) {
				method.setKind(k);
			}
			if (null != m_ObjectMapperSet) {
				method.setObjectMapperSet(m_ObjectMapperSet);
			}
			method.setAfters(afters);
			method.setBefores(befores);
			method.setExceptions(exceptions);
			m_Service.registerMethod(method);
		}

	}

}
