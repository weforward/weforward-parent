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

import cn.weforward.common.util.StringUtil;
import cn.weforward.framework.ApiException;
import cn.weforward.framework.InnerApiMethod;
import cn.weforward.framework.WeforwardSession;
import cn.weforward.framework.support.AbstractApiMethod;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.ops.User;
import cn.weforward.protocol.support.datatype.SimpleDtObject;

/**
 * 当前用户方法
 * 
 * @author daibo
 *
 */
public class CurrentUserMethod extends AbstractApiMethod implements InnerApiMethod {

	public CurrentUserMethod(WeforwardService service) {
		this(service, null);
	}

	public CurrentUserMethod(WeforwardService service, String bashpath) {
		super(StringUtil.toString(bashpath) + "_cuser");
		service.registerMethod(this);
	}

	@Override
	public DtObject handle(String path, DtObject params, Request request, Response response) throws ApiException {
		SimpleDtObject mapped = new SimpleDtObject();
		User user = WeforwardSession.TLS.getUser();
		if (null == user) {
			return mapped;
		}
		mapped.put("id", user.getId());
		mapped.put("name", user.getName());
		return mapped;
	}

}
