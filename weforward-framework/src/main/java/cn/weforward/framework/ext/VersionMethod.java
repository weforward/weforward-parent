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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.util.StringUtil;
import cn.weforward.framework.ApiException;
import cn.weforward.framework.InnerApiMethod;
import cn.weforward.framework.support.AbstractApiMethod;
import cn.weforward.framework.util.VersionUtil;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 查看版本号的方法
 * 
 * @author daibo
 */
public class VersionMethod extends AbstractApiMethod implements InnerApiMethod {
	/** 日志 */
	static final Logger _Logger = LoggerFactory.getLogger(VersionMethod.class);
	/** 版本 */
	private SimpleDtObject m_Version;

	public VersionMethod(WeforwardService service, List<String> atJarClasss) {
		this(service, atJarClasss, null);
	}

	public VersionMethod(WeforwardService service, List<String> atJarClasss, String basepath) {
		super(StringUtil.toString(basepath) + "_version");
		SimpleDtObject m = new SimpleDtObject();
		m.put("version", VersionUtil.getVersion(atJarClasss));
		m_Version = m;
		service.registerMethod(this);
	}

	@Override
	public DtObject handle(String path, DtObject params, Request request, Response response) throws ApiException {
		return m_Version;
	}

}
