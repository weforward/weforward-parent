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
package cn.weforward.boot;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import cn.weforward.boot.support.AbstractSpringApp;
import cn.weforward.common.util.StringUtil;

/**
 * 基于{@link AnnotationConfigApplicationContext} 的入口
 * 
 * 要求使用java类配置(使用@Configuration注解的类)，默认读取cn.weforward.SpringConfig类
 * 
 * 如果要指定格式，可用-Dweforward.springconfig=xxx 指定
 * 
 * @author daibo
 *
 */
public class SpringAnnotationApp extends AbstractSpringApp {
	/**
	 * 主入口
	 * 
	 * @param args 入口参数
	 */
	public static void main(String[] args) {
		init();
		try {
			final AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
			String springconf = System.getProperty("cn.weforward.springconfig");
			if (StringUtil.isEmpty(springconf)) {
				springconf = "cn.weforward.SpringConfig";
			}
			String[] confs = springconf.split(";");
			for (String v : confs) {
				Class<?> annotatedClasses;
				try {
					annotatedClasses = Class.forName(v);
				} catch (ClassNotFoundException e) {
					_Logger.warn("找不到类" + v);
					continue;
				}
				ac.register(annotatedClasses);
			}
			ac.refresh();
			end(ac);
		} catch (Throwable e) {
			_Logger.error("初始化Spring容器异常", e);
			System.exit(1);
		}

	}
}
