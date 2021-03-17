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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

import cn.weforward.common.util.StringUtil;

/**
 * 云配置
 * 
 * @author daibo
 *
 */
public class CloudPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {
	/** 日志 */
	static final Logger _Logger = LoggerFactory.getLogger(CloudPropertyPlaceholderConfigurer.class);
	/** 是否禁用云功能 */
	boolean m_DisableCloud = false;

	/** 禁止cloud功能 */
	private final static String CLOUD_CONFIG_DISABLE = "cloud.disable";
	/** 开发配置文件key */
	private final static String DEV_KEY = "weforward-dev.properties";
	/** 测试配置文件key */
	private final static String TEST_PROP_KEY = "weforward-test.properties";
	/** 配置文件key */
	private final static String PROP_KEY = "weforward.properties";

	private CloudLoader m_CloudLoader;

	public CloudPropertyPlaceholderConfigurer() {
		m_CloudLoader = new CloudLoader();
		setDisableCloud("true".equalsIgnoreCase(System.getProperty(CLOUD_CONFIG_DISABLE)));
	}

	/**
	 * 是否禁止云功能
	 * 
	 * @param disable true=禁止，false=不禁止
	 */
	public void setDisableCloud(boolean disable) {
		m_DisableCloud = disable;
	}

	@Override
	protected Properties mergeProperties() throws IOException {
		// 优先级 setLocaion > weforward-dev.properties > weforward-test.properties >
		// weforward.properties > Service
		// properties
		Properties result = super.mergeProperties();
		try {
			result = load(result, DEV_KEY);
		} catch (Throwable e) {
			throw new IllegalArgumentException("获取配置失败:" + e.getMessage(), e);
		}
		try {
			result = load(result, TEST_PROP_KEY);
		} catch (Throwable e) {
			throw new IllegalArgumentException("获取配置失败:" + e.getMessage(), e);
		}
		try {
			result = load(result, PROP_KEY);
		} catch (Throwable e) {
			throw new IllegalArgumentException("获取配置失败:" + e.getMessage(), e);
		}
		if (!m_DisableCloud) {
			result = m_CloudLoader.load(result);
		}
		return result;
	}

	@Override
	protected void loadProperties(Properties props) throws IOException {
		try {
			super.loadProperties(props);
		} catch (FileNotFoundException e) {
			_Logger.warn("忽略找不到文件异常:" + e.getMessage());
		}
	}

//	@Override
//	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props)
//			throws BeansException {
//		super.processProperties(beanFactoryToProcess, props);
//		String dataPath = props.getProperty("data.path");
//		if (!StringUtil.isEmpty(dataPath)) {
//			File file = new File(FileUtil.getAbsolutePath(dataPath, null));
//			if (!file.exists()) {
//				if (file.mkdir()) {
//					_Logger.info("自动创建数据目录:" + file.getAbsolutePath());
//				}
//			}
//		}
//	}

	/* 加载资源 */
	private static Properties load(Properties result, String propkey) throws IOException {
		ClassPathResource res = new ClassPathResource(propkey);
		if (res.exists()) {
			_Logger.info("load " + propkey);
			Properties prop = new Properties();
			prop.load(res.getInputStream());
			for (Entry<Object, Object> e : prop.entrySet()) {
				String key = StringUtil.toString(e.getKey());
				if (null != result.get(key) || null != System.getProperty(key)) {
					continue;
				}
				String value = StringUtil.toString(e.getValue());
				result.put(key, value);
			}
		}
		return result;

	}

}
