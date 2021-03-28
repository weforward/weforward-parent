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

import cn.weforward.framework.ApiException;
import cn.weforward.framework.Authorizer;
import cn.weforward.protocol.Request;

/**
 * 开关验证器
 * 
 * @author daibo
 *
 */
public class OnOffAuthorizer implements Authorizer {
	/** 验证器-开 */
	public static final Authorizer ON = new OnOffAuthorizer(true);
	/** 验证器-关 */
	public static final Authorizer OFF = new OnOffAuthorizer(false);

	/** 是否通行 */
	protected boolean m_On;

	public OnOffAuthorizer(boolean on) {
		m_On = on;
	}

	@Override
	public void auth(Request request) throws ApiException {
		if (!m_On) {
			throw ApiException.AUTH_FAILED;
		}

	}

}
