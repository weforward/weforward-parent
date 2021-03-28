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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import cn.weforward.common.util.StringUtil;
import cn.weforward.common.util.TransList;
import cn.weforward.framework.ext.WeforwardScript;
import cn.weforward.framework.util.VersionUtil;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.support.datatype.FriendlyObject;
import cn.weforward.protocol.support.datatype.SimpleDtList;
import cn.weforward.protocol.support.datatype.SimpleDtObject;
import cn.weforward.protocol.support.datatype.SimpleDtString;

/**
 * 版本脚本
 * 
 * @author daibo
 *
 */
public class VersionScript implements WeforwardScript {

	private static final Logger _Logger = LoggerFactory.getLogger(VersionScript.class);

	private static DtObject VERSION;

	private static DtObject getVersion() throws IOException {
		if (null != VERSION) {
			return VERSION;
		}
		String current = "";
		String jar = VersionUtil.findJarName(VersionScript.class);

		List<String> weforwardlib = new ArrayList<>();
		List<String> otherlib = new ArrayList<>();
		if (!StringUtil.isEmpty(jar)) {
			try (JarFile file = new JarFile(jar)) {
				Manifest mf = file.getManifest();
				Attributes main = mf.getMainAttributes();
				String libtag = "BOOT-INF/lib/";
				String jartag = ".jar";
				String jarname = file.getName();
				int index = jarname.lastIndexOf(File.separator);
				current = jarname.substring(index + 1, jarname.length() - jartag.length()) + "-"
						+ main.getValue("Implementation-Version");
				Enumeration<JarEntry> e = file.entries();
				while (e.hasMoreElements()) {
					String name = e.nextElement().getName();
					if (name.startsWith(libtag) && name.endsWith(jartag)) {
						name = name.substring(libtag.length(), name.length() - jartag.length());
						if (name.startsWith("weforward-")) {
							weforwardlib.add(name);
						} else {
							otherlib.add(name);
						}
					}
				}
				Collections.sort(weforwardlib);
				Collections.sort(otherlib);
			}
		}
		SimpleDtObject version = new SimpleDtObject();
		version.put("current", current);
		version.put("weforward_lib", SimpleDtList.valueOf(new TransList<DtBase, String>(weforwardlib) {

			@Override
			protected DtBase trans(String src) {
				return new SimpleDtString(src);
			}
		}));
		version.put("other_lib", SimpleDtList.valueOf(new TransList<DtBase, String>(otherlib) {

			@Override
			protected DtBase trans(String src) {
				return new SimpleDtString(src);
			}
		}));
		VERSION = version;
		return VERSION;
	}

	@Override
	public DtObject handle(ApplicationContext context, String path, FriendlyObject params) {
		try {
			return getVersion();
		} catch (IOException e) {
			_Logger.warn("获取版本异常", e);
			SimpleDtObject result = new SimpleDtObject();
			result.put("error", e.getMessage());
			return result;
		}
	}

}
