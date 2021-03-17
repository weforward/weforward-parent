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
package cn.weforward.framework.util;

import cn.weforward.framework.WeforwardResource;

/**
 * 资源vo
 * 
 * @author daibo
 *
 */
public class WeforwardResourceVo implements WeforwardResource {
	/** 唯一id */
	protected String m_Id;
	/** 资源过期时间（自1970年1月1日起的秒数） */
	protected long m_Expire;
	/** 服务 */
	protected String m_Service;
	/** 资源外的附加数据 */
	protected Object m_Data;

	protected WeforwardResourceVo(String id, long expire, String service, Object data) {
		m_Id = id;
		m_Expire = expire;
		m_Service = service;
		m_Data = data;
	}

	@Override
	public String getId() {
		return m_Id;
	}

	@Override
	public long getExpire() {
		return m_Expire;
	}

	@Override
	public String getService() {
		return m_Service;
	}

	@Override
	public Object getData() {
		return m_Data;
	}

}
