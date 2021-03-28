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

import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ClassUtils;

import cn.weforward.common.util.StringUtil;

/**
 * 发现控制器的类（需配合spring框架）
 * 
 * @author daibo
 *
 */
public class MethodsAware extends MethodsRegister implements BeanPostProcessor {
	/** 包 */
	protected List<String> m_Packages;

	public MethodsAware(WeforwardService service) {
		super(service);
	}

	public void setPackage(String pk) {
		setPackages(Collections.singletonList(pk));
	}

	public void setPackages(List<String> ps) {
		m_Packages = ps;
	}

//	@Override
//	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//		m_Service.setApplicationContext(applicationContext);
//		for (String bean : applicationContext.getBeanDefinitionNames()) {
//			Object value = applicationContext.getBean(bean);
//			if (null == value) {
//				continue;
//			}
//			Class<?> clazz = ClassUtils.getUserClass(value);
//			List<String> ps = m_Packages;
//			if (null != ps && !ps.isEmpty()) {
//				String p = clazz.getPackage().getName();
//				boolean match = false;
//				for (String v : ps) {
//					if (StringUtil.eq(v, p)) {
//						match = true;
//						break;
//					}
//				}
//				if (!match) {
//					continue;
//				}
//			}
//			register(value);
//		}
//	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> clazz = ClassUtils.getUserClass(bean);
		List<String> ps = m_Packages;
		if (null != ps && !ps.isEmpty()) {
			Package mypackage = clazz.getPackage();
			String p;
			if (null == mypackage) {
				p = "";
			} else {
				p = mypackage.getName();
			}
			boolean match = false;
			for (String v : ps) {
				if (StringUtil.eq(v, p)) {
					match = true;
					break;
				}
			}
			if (!match) {
				return bean;
			}
		}
		register(bean);
		return bean;
	}

}
