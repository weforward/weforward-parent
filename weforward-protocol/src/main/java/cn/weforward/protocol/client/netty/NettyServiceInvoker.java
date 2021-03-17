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
package cn.weforward.protocol.client.netty;

import cn.weforward.common.util.StringUtil;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.ext.Producer;

/**
 * 使用netty的服务调用器
 * 
 * @author daibo
 *
 */
public class NettyServiceInvoker extends NettyAnyServiceInvoker {
	protected String m_ServiceName;

	public NettyServiceInvoker(String preUrl, String serviceName, Producer producer) {
		super(preUrl, producer);
		if (StringUtil.isEmpty(serviceName)) {
			throw new IllegalArgumentException("服务名不能为空");
		}
		m_Url = m_Url + serviceName;
		m_ServiceName = serviceName;
	}

	@Override
	protected String getServiceName() {
		return m_ServiceName;
	}

	protected String getServiceUrl(String serviceName) {
		if (!StringUtil.eq(serviceName, m_ServiceName)) {
			throw new IllegalArgumentException("服务名不匹配：" + serviceName + "=>" + m_ServiceName);
		}
		return m_Url;
	}

	public Request createRequest(String serviceName, String method, DtObject params) {
		throw new IllegalArgumentException("不能指定服务名");
	}
}
