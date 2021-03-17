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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import cn.weforward.common.Destroyable;
import cn.weforward.common.compiler.CompilerJavaUtil;
import cn.weforward.common.sys.Shutdown;
import cn.weforward.common.util.ClassUtil;
import cn.weforward.common.util.FileUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.framework.ApiException;
import cn.weforward.framework.InnerApiMethod;
import cn.weforward.framework.debug.VersionScript;
import cn.weforward.framework.support.AbstractApiMethod;
import cn.weforward.protocol.Access;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.datatype.DtString;
import cn.weforward.protocol.support.datatype.FriendlyObject;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 脚本执行方法
 * 
 * @author daibo
 *
 */
public class DebugMethod extends AbstractApiMethod implements InnerApiMethod, ApplicationContextAware, Destroyable {
	/** 日志 */
	private static final Logger _Logger = LoggerFactory.getLogger(DebugMethod.class);
	/** class临时文件目录 */
	protected final File m_ClassTmp;
	/** java临时文件目录 */
	protected final File m_JavaTmp;
	/** spring上下文 */
	protected ApplicationContext m_ApplicationContext;
	/** 内部标识 */
	protected final static String INNER_SCRIPT_TAG = "_";
	/** 内部脚本包名 */
	protected final static String INNER_PACKAGE = VersionScript.class.getPackage().getName() + '.';

	public DebugMethod(WeforwardService service) {
		this(service, null);
	}

	public DebugMethod(WeforwardService service, String bashPath) {
		this(service, bashPath, Access.KIND_GATEWAY);
	}

	public DebugMethod(WeforwardService service, String bashPath, String kind) {
		super(StringUtil.toString(bashPath) + "_debug", kind);
		m_ClassTmp = new File(FileUtil.getAbsolutePath("weforward_debug_class", System.getProperty("java.io.tmpdir")));
		if (!m_ClassTmp.exists()) {
			m_ClassTmp.mkdirs();
		}
		m_JavaTmp = new File(FileUtil.getAbsolutePath("weforward_debug_java", System.getProperty("java.io.tmpdir")));
		if (!m_JavaTmp.exists()) {
			m_JavaTmp.mkdirs();
		}
		service.registerMethod(this);
		Shutdown.register(this);
	}

	@Override
	public DtObject handle(String path, DtObject params, Request request, Response response) throws ApiException {
//		src:"", // 脚本源代码
//		name:"", // 脚本类名
//		args:"
		URLClassLoader loader = null;
		try {
			try {
				loader = new URLClassLoader(new URL[] { m_ClassTmp.toURI().toURL() },
						ClassUtil.getDefaultClassLoader());
			} catch (MalformedURLException e) {
				throw new ApiException(ApiException.CODE_INTERNAL_ERROR, "创建loader异常，" + e.getMessage(), e);
			}
			FriendlyObject myparams = FriendlyObject.valueOf(params);
			String name = myparams.getString("name");
			String src = myparams.getString("src");
			Class<?> clazzObj;
			if (!StringUtil.isEmpty(name)) {
				if (name.startsWith(INNER_SCRIPT_TAG)) {
					String fullclazzName = name.replace(INNER_SCRIPT_TAG, INNER_PACKAGE);
					try {
						clazzObj = Class.forName(fullclazzName);
					} catch (ClassNotFoundException e) {
						throw new ApiException(ApiException.CODE_INTERNAL_ERROR, "加载异常，找不到" + fullclazzName + "类", e);
					}
				} else {

					File java = new File(m_JavaTmp, name + ".java");
					if (!java.exists()) {
						try {
							clazzObj = Class.forName(name);
						} catch (ClassNotFoundException e) {
							throw new ApiException(ApiException.CODE_ILLEGAL_ARGUMENT, "不存在" + name + "脚本");
						}
					} else {
						complier(java);
						String fullclazzName = readPackage(java) + name;
						clazzObj = loadClass(loader, fullclazzName);
					}
				}
			} else if (!StringUtil.isEmpty(src)) {
				name = readClass(src);
				File java = new File(m_JavaTmp, name + ".java");
				createSrc(java, src);
				complier(java);
				String fullclazzName = readPackage(src) + name;
				clazzObj = loadClass(loader, fullclazzName);
			} else {
				throw new ApiException(ApiException.CODE_ILLEGAL_ARGUMENT, "缺少name或src参数");
			}
			return doHandle(clazzObj, path, params);
		} finally

		{
			if (null != loader) {
				try {
					loader.close();
				} catch (IOException e) {
					_Logger.warn("忽略关闭异常", e);
				}
			}
		}

	}

	protected DtObject doHandle(Class<?> clazzObj, String path, DtObject params) throws ApiException {
		Object v;
		try {
			v = clazzObj.getConstructor().newInstance();
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			throw new ApiException(ApiException.CODE_INTERNAL_ERROR, "构造异常：" + e.getMessage(), e);
		}
		Method m;
		try {
			m = clazzObj.getMethod("handle", ApplicationContext.class, String.class, FriendlyObject.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new ApiException(ApiException.CODE_INTERNAL_ERROR, "获取方法异常：" + e.getMessage(), e);
		}
		Object back;
		try {
			DtBase args = params.getAttribute("args");
			back = m.invoke(v, m_ApplicationContext, path, valueOf(args));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new ApiException(ApiException.CODE_INTERNAL_ERROR, "调用方法异常：" + e.getMessage());
		}
		return (DtObject) back;
	}

	protected static FriendlyObject valueOf(DtBase args) {
		if (args instanceof DtObject) {
			return FriendlyObject.valueOf((DtObject) args);
		} else if (args instanceof DtString) {
			String v = ((DtString) args).value();
			String[] arr = v.split("&");
			SimpleDtObject params = new SimpleDtObject();
			for (String child : arr) {
				int index = child.indexOf('=');
				if (index < 0) {
					continue;
				}
				params.put(child.substring(0, index), child.substring(index + 1));
			}
			return FriendlyObject.valueOf(params);
		}
		return FriendlyObject.valueOf(null);
	}

	protected void createSrc(File src, String content) throws ApiException {
		try (FileOutputStream out = new FileOutputStream(src)) {
			out.write(content.getBytes());
			out.flush();
		} catch (IOException e) {
			throw new ApiException(ApiException.CODE_INTERNAL_ERROR, "写入源文件出错:" + e.getMessage(), e);
		}

	}

	protected void complier(File src) throws ApiException {
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		try {
			if (!CompilerJavaUtil.complier(src, m_ClassTmp, diagnostics)) {
				throw new ApiException(ApiException.CODE_INTERNAL_ERROR, "编译失败");
			}
			StringBuilder sb = new StringBuilder();
			for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
				sb.append("Code: " + diagnostic.getCode() + "  Kind: " + diagnostic.getKind() + "  Position: "
						+ diagnostic.getPosition() + "  Line Number:" + diagnostic.getLineNumber() + "  Column Number: "
						+ diagnostic.getColumnNumber() + "  Source: " + diagnostic.getSource() + "  Message: "
						+ diagnostic.getMessage(null));
				sb.append("\n");
			}
			if (!StringUtil.isEmpty(sb.toString())) {
				_Logger.warn("编译异常," + sb.toString());
			}
		} catch (IOException e) {

			throw new ApiException(ApiException.CODE_INTERNAL_ERROR, "编译异常", e);
		}

	}

	protected Class<?> loadClass(URLClassLoader loader, String clazz) throws ApiException {
		try {
			return loader.loadClass(clazz);
		} catch (ClassNotFoundException e) {
			throw new ApiException(ApiException.CODE_INTERNAL_ERROR, "加载异常，找不到" + clazz + "类", e);
		}
	}

	protected static String readPackage(File file) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String line = reader.readLine();
			while (null != line) {
				if (line.startsWith("package")) {
					return line.substring(8, line.length() - 1) + ".";
				} else {
					line = reader.readLine();
				}
			}
		} catch (IOException e) {
			_Logger.warn("读取异常", e);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				_Logger.warn("忽略异常", e);
			}
		}
		return "";
	}

	protected String readClass(String content) {
		String tag = "public class ";
		int start = content.indexOf(tag) + tag.length();
		int end = content.indexOf(" ", start + 1);
		return content.substring(start, end);
	}

	protected String readPackage(String content) {
		String tag = "package ";
		int index = content.indexOf(tag);
		if (index < 0) {
			return "";
		}
		int start = index + tag.length();
		int end = content.indexOf(";", start + 1);
		return content.substring(start, end) + ".";
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		m_ApplicationContext = applicationContext;
	}

	@Override
	public void destroy() {
		try {
			CompilerJavaUtil.delete(m_ClassTmp);
			CompilerJavaUtil.delete(m_JavaTmp);
		} catch (IOException e) {
			_Logger.warn("忽略清理异常", e);
		}
	}

}
