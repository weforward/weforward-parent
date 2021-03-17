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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import cn.weforward.common.Dictionary;
import cn.weforward.common.KvPair;
import cn.weforward.common.io.BytesInputStream;
import cn.weforward.common.io.BytesOutputStream;
import cn.weforward.common.util.Bytes;

/**
 * 处理HTTP以Mulipart上传表单
 * 
 * @author liangyi
 * 
 */
public class WebFileUpload extends WebUpload {
	// /** 限制单个上传文件最大字节数 */
	// protected int m_MaxFileSize;
	/** 限制上传文件最大数 */
	protected int m_MaxFileCount;
	/** 允许的文件类型（扩展名），如：.txt;.htm，当指定允许的文件类型表示只允许指定的类型 */
	protected String m_AllowFileExt; //
	/** 拒绝的文件类型（扩展名），如：.exe;.dll，如果指定拒绝的文件类型且允许类型为null表示只拒绝指定的类型 */
	protected String m_DenialFileExt; //
	/** 文件表单项数 */
	protected int m_FileCount;
	/** 表单直接转存本地文件或输出流 */
	protected Dictionary<String, Object> m_DirectSend;

	public WebFileUpload() {
		m_DirectSend = Dictionary.Util.empty();
	}

	/**
	 * 把指定的文件表单直接转存到本地文件
	 * 
	 * @param toFiles 转存表
	 */
	public WebFileUpload(List<KvPair<String, Object>> toFiles) {
		m_DirectSend = Dictionary.Util.valueOf(toFiles);
	}

	// // 指定单个文件最大尺寸及只允许的文件类型
	// public WebFileUpload(int maxSize, String allowFileTypes) {
	// m_MaxFileSize = maxSize;
	// m_AllowFileExt = allowFileTypes;
	// }

	public int getFileCount() {
		return m_FileCount;
	}

	// public int getMaxFileSize() {
	// return m_MaxFileSize;
	// }
	//
	// public void setMaxFileSize(int mMaxFileSize) {
	// m_MaxFileSize = mMaxFileSize;
	// }

	public int getMaxFileCount() {
		return m_MaxFileCount;
	}

	public void setMaxFileCount(int maxFileCount) {
		m_MaxFileCount = maxFileCount;
	}

	public String getAllowFileExt() {
		return m_AllowFileExt;
	}

	public void setAllowFileExt(String fileExt) {
		m_AllowFileExt = fileExt;
	}

	public String getDenialFileExt() {
		return m_DenialFileExt;
	}

	public void setDenialFileExt(String fileExt) {
		m_DenialFileExt = fileExt;
	}

	protected WebForm onFetchForm(InputStream in, List<WebForm.Header> headers) throws IOException {
		WebFormMix form = new WebFormMix(headers);
		if (null != form.getFileName() && 0 != form.getFileName().length()) {
			// 文件表单，先检查是否超出最大文件数
			if (m_MaxFileCount > 0 && m_FileCount >= m_MaxFileCount) {
				throw new IOException("Over count:" + m_FileCount + ">" + m_MaxFileCount);
			}
			++m_FileCount;
			String ext = form.getFileNameExt();
			if (null != ext) {
				if (0 == ext.length()) {
					ext = null;
				} else {
					ext = ext.toLowerCase() + ';';
				}
			}
			if (null != m_AllowFileExt && m_AllowFileExt.length() > 0) {
				// 检查是否允许的文件类型
				if (null == ext || -1 == m_AllowFileExt.indexOf(ext)) {
					// 不在接受的文件类型列表中
					throw new IOException("Without allow file type :" + ext + " not in " + m_AllowFileExt);
				}
			} else if (null != m_DenialFileExt && m_DenialFileExt.length() > 0) {
				// 检查是否拒绝的文件类型
				if (null != ext && -1 != m_DenialFileExt.indexOf(ext)) {
					// 被拒绝的文件类型
					throw new IOException("Denial file type :" + ext + " in " + m_DenialFileExt);
				}
			}
		}
		Object sendTo = m_DirectSend.get(form.getName());
		form.setValue(in, sendTo);
		// in.close();
		return form;
	}

	/** 标记表单内容不可达（因为转存） */
	private static byte[] MARK_UNREACHABLE = {};

	/**
	 * 混合的表单项
	 * 
	 * @author liangyi
	 * 
	 */
	protected static class WebFormMix extends WebForm {
		/** 内容长度 */
		protected int m_ContentLength;
		/** 缓冲的内容 */
		protected byte[] m_Content;

		public byte[] getBytes() throws IOException {
			if (0 == m_ContentLength) {
				return Bytes._nilBytes;
			}
			if (MARK_UNREACHABLE == m_Content) {
				throw new IOException("Unreachable");
			}
			// if (null == m_Content && null != m_LocalFile) {
			// // 由文件加载
			// Bytes bytes = CachedInputStream.toBytes(getStream());
			// m_Content = bytes.fit();
			// }
			return m_Content;
		}

		public InputStream getStream() throws IOException {
			if (0 == m_ContentLength) {
				return BytesInputStream.empty();
			}
			if (null != m_Content) {
				if (MARK_UNREACHABLE == m_Content) {
					throw new IOException("Unreachable");
				}
				return new ByteArrayInputStream(m_Content, 0, m_ContentLength);
			}

			// if (null != m_LocalFile) {
			// return new FileInputStream(m_LocalFile);
			// }
			throw new IOException("Unkown");
		}

		public int getContentLength() {
			return m_ContentLength;
		}

		public WebFormMix(List<Header> headers) {
			m_ContentLength = -1;
			analyze(headers);
		}

		public void setContentLength(int length) {
			m_ContentLength = length;
		}

		/**
		 * 转储或缓存表单内容
		 * 
		 * @param content 表单内容流
		 * @param sendTo  直接转传的文件或输出流
		 * @throws IOException IO异常
		 */
		public void setValue(InputStream content, Object sendTo) throws IOException {
			if (sendTo instanceof File) {
				// 转储到本地文件
				File localFile = (File) sendTo;
				File dir = localFile.getParentFile();
				if (null != dir) {
					dir.mkdirs();
				}
				try (FileOutputStream out = new FileOutputStream(localFile)) {
					if (content instanceof PartInputStream) {
						// ((PartInputStream) content).transfer(out);
						m_ContentLength = (int) (Integer.MAX_VALUE & ((PartInputStream) content).transfer(out));
					} else {
						m_ContentLength = BytesOutputStream.transfer(content, out, 0);
					}
				}
				// m_ContentLength = (int) localFile.length();
				m_Content = MARK_UNREACHABLE;
				return;
			}
			if (sendTo instanceof OutputStream) {
				// 转存到输出流
				OutputStream out = (OutputStream) sendTo;
				if (content instanceof PartInputStream) {
					m_ContentLength = (int) (Integer.MAX_VALUE & ((PartInputStream) content).transfer(out));
				} else {
					m_ContentLength = BytesOutputStream.transfer(content, out, 0);
				}
				m_Content = MARK_UNREACHABLE;
				return;
			}

			BytesOutputStream bs = new BytesOutputStream(content);
			bs.close();
			m_Content = bs.detach();
			m_ContentLength = m_Content.length;
		}
	}
}
