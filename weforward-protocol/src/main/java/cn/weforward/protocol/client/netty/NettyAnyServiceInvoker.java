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
import cn.weforward.protocol.aio.ClientChannel;
import cn.weforward.protocol.aio.netty.NettyHttpClientFactory;
import cn.weforward.protocol.ext.Producer;

/**
 * 使用netty的服务调用器（不绑定服务名）
 * 
 * @author daibo,liangyi
 *
 */
public class NettyAnyServiceInvoker extends AbstractNettyServiceInvoker {
	protected final static NettyHttpClientFactory FACTORY = new NettyHttpClientFactory();

	protected String m_Url;

	public NettyAnyServiceInvoker(String preUrl, Producer producer) {
		super(producer);
		if (StringUtil.isEmpty(preUrl)) {
			throw new IllegalArgumentException("链接前缀不能为空");
		}
		if (preUrl.endsWith("/")) {
			m_Url = preUrl;
		} else {
			m_Url = preUrl + "/";
		}
	}

	@Override
	protected ClientChannel open() {
		return FACTORY;
	}

	@Override
	protected String getServiceName() {
		throw new IllegalArgumentException("未绑定服务名");
	}

	@Override
	protected String getServiceUrl(String serviceName) {
		return m_Url + serviceName;
	}

	@Override
	public int getConnectTimeout() {
		return FACTORY.getConnectTimeout();
	}

	@Override
	public void setConnectTimeout(int ms) {
//		FACTORY.setConnectTimeout(ms);
	}
}
