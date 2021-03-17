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
package cn.weforward.boot.support;

import java.io.Console;
import java.util.Arrays;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import cn.weforward.common.Destroyable;
import cn.weforward.common.sys.Shutdown;
import cn.weforward.common.util.StringUtil;

/**
 * 抽象SpringApp类
 *
 */
public class AbstractSpringApp {
	/** 日志记录器 */
	protected final static Logger _Logger = LoggerFactory.getLogger(AbstractSpringApp.class);
	/** 服务id */
	public final static String SERVERID_KEY = "weforward.serverid";
	/** 默认的服务id */
	public final static String SERVERID_DEFAULT = "x00ff";
	/** 输入属性key */
	public final static String INPUT_PROP_KEY = "wefoward.input.prop";

	private static Destroyable DESTROYABLE_RUNNING;

	/**
	 * 初始化
	 */
	protected static void init() {
		String inputprop = System.getProperty(INPUT_PROP_KEY);
		if (!StringUtil.isEmpty(inputprop)) {
			Console console = System.console();
			String[] arr = inputprop.split(";");
			if (null == console) {
				System.out.println("no console use scanner");
				try (Scanner scanner = new Scanner(System.in)) {
					for (String key : arr) {
						System.out.print("Please Enter " + key + ":");
						String value = scanner.nextLine();
						System.setProperty(key, new String(value));
						System.out.println();
					}
					scanner.close();
				}
			} else {
				for (String key : arr) {
					char[] value = console.readPassword("Please Enter " + key + ":");
					System.setProperty(key, new String(value));
				}
			}
			System.out.println("ok..");
		}

		{
			String sid = System.getProperty(SERVERID_KEY);
			if (null == sid || sid.trim().length() == 0) {
				_Logger.warn("未指定" + SERVERID_KEY + "默认使用" + SERVERID_DEFAULT);
				sid = SERVERID_DEFAULT;
				System.setProperty(SERVERID_KEY, sid);
			}
		}
	}

	/**
	 * 运行结束
	 * 
	 * @param ac 应用容器
	 */
	public static void end(final ApplicationContext ac) {
		_Logger.info("start success " + Arrays.toString(ac.getBeanDefinitionNames()));
		DESTROYABLE_RUNNING = new Destroyable() {

			@Override
			public void destroy() {
				if (ac instanceof ConfigurableApplicationContext) {
					((ConfigurableApplicationContext) ac).close();
					_Logger.info("close success");
				}
				_Logger.info("destroy success");
			}
		};
		Shutdown.register(DESTROYABLE_RUNNING);
	}
}
