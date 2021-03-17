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

import cn.weforward.common.util.IpRanges;
import cn.weforward.framework.ApiException;
import cn.weforward.framework.Authorizer;
import cn.weforward.protocol.Request;

/**
 * ip范围验证器
 * 
 * @author daibo
 *
 */
public class IpRangesAuthorizer implements Authorizer {
	/**
	 * ip范围
	 */
	protected IpRanges m_IpRanges;

	/**
	 * 构造地址（段）表，每IP（段）项以分号分隔，如：“127.0.0.1;192.168.0.0-192.168.0.100”
	 * 
	 * @param ipList ip段
	 */
	public IpRangesAuthorizer(String ipList) {
		this(new IpRanges(ipList));
	}

	/**
	 * 构造
	 * 
	 * @param ipranges ip段
	 */
	public IpRangesAuthorizer(IpRanges ipranges) {
		m_IpRanges = ipranges;
	}

	@Override
	public void auth(Request request) throws ApiException {
		if (null == m_IpRanges.find(request.getAddr())) {
			throw ApiException.AUTH_FAILED;
		}

	}

}
