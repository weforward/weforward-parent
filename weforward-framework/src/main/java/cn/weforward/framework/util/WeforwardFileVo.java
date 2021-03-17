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

import java.io.InputStream;

import cn.weforward.framework.WeforwardFile;

/**
 * 文件vo
 * 
 * @author daibo
 *
 */
public class WeforwardFileVo implements WeforwardFile {
	/** 文件名 */
	protected String m_Name;
	/** 流 */
	protected InputStream m_Stream;

	protected String m_ContentType;

	protected WeforwardFileVo(String name, InputStream stream) {
		m_Name = name;
		m_Stream = stream;
	}

	protected WeforwardFileVo(String name, InputStream stream, String contentType) {
		m_Name = name;
		m_Stream = stream;
		m_ContentType = contentType;
	}

	@Override
	public String getName() {
		return m_Name;
	}

	@Override
	public InputStream getStream() {
		return m_Stream;
	}

	@Override
	public String getContentType() {
		return m_ContentType;
	}

}
