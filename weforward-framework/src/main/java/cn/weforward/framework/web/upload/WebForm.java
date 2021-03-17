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
package cn.weforward.framework.web.upload;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import cn.weforward.common.util.SimpleKvPair;
import cn.weforward.common.util.StringUtil;

public abstract class WebForm {
	protected String m_Name;
	protected String m_FileName;
	protected String m_ContentType;
	protected String m_Charset;

	/*
	 * 以流方式取得表单内容
	 */
	public abstract InputStream getStream() throws IOException;

	/*
	 * 以字节数组取得表单内容
	 */
	public abstract byte[] getBytes() throws IOException;

	public String getName() {
		return m_Name;
	}

	/*
	 * 文件名（如果表单内容是上传文件）
	 */
	public String getFileName() {
		return m_FileName;
	}

	/*
	 * 内容的类型（通常表单内容是上传文件时才会有）
	 */
	public String getContentType() {
		return m_ContentType;
	}

	/*
	 * 文件扩展名（如果表单内容是上传文件）
	 */
	public String getFileNameExt() {
		if (null == m_FileName || 0 == m_FileName.length()) {
			return m_FileName;
		}
		int idx = m_FileName.lastIndexOf('.');
		if (-1 != idx) {
			return m_FileName.substring(idx);
		}
		return null;
	}

	public int getContentLength() {
		return -1;
	}

	/*
	 * 以字串返回表单内容
	 */
	public String getString() throws IOException {
		byte[] data = getBytes();
		if (null == data) {
			return null;
		}
		if (null == m_Charset) {
			// 检查字符集
			m_Charset = StringUtil.isUtf8(data, 0, data.length) ? "utf-8" : "";
		}
		if (0 == m_Charset.length()) {
			return new String(data);
		}
		return new String(data, m_Charset);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(128);
		builder.append("{name:").append(getName());
		if (!StringUtil.isEmpty(m_FileName)) {
			builder.append(",file:").append(m_FileName);
		}
		if (!StringUtil.isEmpty(m_ContentType)) {
			builder.append(",type:").append(m_ContentType);
		}
		if (!StringUtil.isEmpty(m_Charset)) {
			builder.append(",charset:").append(m_Charset);
		}
		builder.append('}');
		return builder.toString();
	}

	/*
	 * 分析headers
	 */
	void analyze(List<Header> headers) {
		// 分析headers
		Header header = null;
		for (int i = 0; i < headers.size(); i++) {
			header = (Header) headers.get(i);
			String name = header.getKey().toLowerCase();
			if (name.equals("content-disposition")) {
				// Content-Disposition: form-data; name="FILE1";
				// filename="C:\桌面\JeyoMobileSetup.log.txt"
				// Content-Disposition: form-data; name="action"
				m_Name = getValueByName("name", header.getValue());
				m_FileName = getValueByName("filename", header.getValue());
			} else if (name.equals("content-type")) {
				// Content-Type: application/octet-stream
				m_ContentType = header.getValue();
			} else if (name.equals("charset")) {
				// Charset: UTF-8
				m_Charset = header.getValue();
			}
		}
		return;
	}

	/*
	 * 在header格式行中提取指定名的值
	 */
	static String getValueByName(String name, String line) {
		int idx = line.indexOf(name);
		if (-1 == idx) {
			return null;
		}
		if ((0 != idx && ' ' != line.charAt(idx - 1) && '\t' != line.charAt(idx - 1))
				&& ';' != line.charAt(idx - 1)) {
			return null;
		}
		int begin = line.indexOf('=', idx + name.length());
		if (-1 == begin) {
			return null;
		}
		// 去除前导空格
		while (' ' == line.charAt(++begin)) {
			++begin;
		}
		if ('\"' != line.charAt(begin)) {
			return null;
		}
		idx = line.indexOf('\"', begin + 1);
		if (-1 != idx) {
			return line.substring(begin + 1, idx);
		}
		return null;
	}

	/*
	 * Header项
	 */
	public static class Header extends SimpleKvPair<String, String> {
		public Header(String name, String value) {
			super(name, value);
		}
	}
}
