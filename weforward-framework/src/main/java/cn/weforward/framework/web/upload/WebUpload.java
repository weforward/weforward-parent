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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.weforward.common.io.BytesOutputStream;
import cn.weforward.common.util.Bytes;
import cn.weforward.common.util.StringUtil;

/**
 * 对上传multipart表单的支持
 * 
 * @author liangyi
 * 
 */
public abstract class WebUpload {
	/** 上传输入流 */
	protected InputStream m_Input;
	/** 边界串（已包含前缀“--”） */
	protected byte[] m_Boundary;
	/** 表单 */
	protected Map<String, WebForm> m_FormsMap;
	protected WebForm[] m_Forms;
	/** 用于分析时的临时缓冲区 */
	protected BytesOutputStream m_TempBuffer;

	/**
	 * 实现此方法提取表单项，如果返回null将丢弃此表单项
	 * 
	 * @param in      输入流
	 * @param headers 请求头
	 * @return 表单
	 * @throws IOException IO异常
	 */
	protected abstract WebForm onFetchForm(InputStream in, List<WebForm.Header> headers) throws IOException;

	/**
	 * 由下标取得表单项
	 * 
	 * @param index 索引
	 * @return 表单
	 */
	public final WebForm get(int index) {
		if (null == m_Forms) {
			Collection<WebForm> c = m_FormsMap.values();
			m_Forms = c.toArray(new WebForm[c.size()]);
		}
		return m_Forms[index];
	}

	/**
	 * 由名称取得表单项
	 * 
	 * @param name 名称
	 * @return 表单
	 */
	public final WebForm get(String name) {
		return m_FormsMap.get(name);
	}

	/**
	 * 等同于get
	 * 
	 * @param name 名称
	 * @return 表单
	 */
	public final WebForm getForm(String name) {
		return get(name);
	}

	/**
	 * 等同于get
	 * 
	 * @param index 索引
	 * @return 表单
	 */
	public final WebForm getForm(int index) {
		return get(index);
	}

	/**
	 * 表单项数
	 * 
	 * @return 项数
	 */
	public final int size() {
		return m_FormsMap.size();
	}

	/**
	 * 表单Map集合
	 * 
	 * @return 集合
	 */
	public final Map<String, WebForm> getForms() {
		return m_FormsMap;
	}

	/**
	 * 由流输入分析表单项
	 * 
	 * @param in 输入流
	 * @throws IOException io异常
	 */
	public void input(InputStream in) throws IOException {
		if (null == in) {
			throw new IOException("Invalid format MIME part(InputStream is null)!");
		}

		// if (!(in instanceof BufferedInputStream)) {
		// // 使用有缓存区的流
		// in = new BufferedInputStream(in);
		// }
		m_Input = in;
		// m_Input = CachedInputStream.cached(in);

		m_FormsMap = new HashMap<String, WebForm>();

		// 首先读取第一个边界
		readBoundary();

		// 读取表单项
		WebForm form = null;
		PartInputStream uin = null;
		for (;;) {
			// 分析段headers
			List<WebForm.Header> headers = analyzeHeaders();
			if (0 == headers.size()) {
				break;
			}

			// 提取此段表单的内容及读取完边界
			uin = new PartInputStream();
			form = onFetchForm(uin, headers);
			if (null != form) {
				m_FormsMap.put(form.getName(), form);
			}
			// onFetchForm有可能没读取完uin流中的所有数据，要清空它才行
			uin.skipAtEnd();
		}
	}

	protected BytesOutputStream getTempBuffer() {
		if (null == m_TempBuffer) {
			m_TempBuffer = new BytesOutputStream(512);
		}
		return m_TempBuffer;
	}

	/**
	 * 读取MIME边界
	 * 
	 * @throws IOException IO异常
	 */
	protected void readBoundary() throws IOException {
		BytesOutputStream buf = getTempBuffer();
		int ret;
		while (true) {
			ret = m_Input.read();
			if (-1 == ret) {
				throw new IOException(
						"Invalid format MIME part,please check this form 'enctype=\"multipart/form-data\"'!");
			}
			if ('\r' == ret) {
				continue;
			}
			if ('\n' == ret) {
				break;
			}
			buf.write(ret);
		}
		m_Boundary = buf.detach();
		if (null == m_Boundary || m_Boundary.length < 2) {
			throw new IOException("Invalid format MIME part(boundary too short)!");
		}
	}

	/**
	 * 分析MIME头
	 * 
	 * @return 分析结果
	 * @throws IOException IO异常
	 */
	protected List<WebForm.Header> analyzeHeaders() throws IOException {
		ArrayList<WebForm.Header> headers = new ArrayList<WebForm.Header>();
		WebForm.Header header = null;
		BytesOutputStream buf = getTempBuffer();
		int ret;
		Bytes line = null;
		while (true) {
			ret = m_Input.read();
			if (ret < 0) {
				if (headers.size() > 0) {
					throw new IOException("Invalid format MIME part(headers)!");
				}
				return headers;
			}
			if ('\r' == ret) {
				continue;
			}
			if ('\n' == ret) {
				// 是换行符了
				if (0 == buf.size()) {
					break; // 空行表示header结束
				}
				// line = out.toByteArray();
				line = buf.getBytes();
				if (null == line || 0 == line.getSize()) {
					break; // 空行表示header结束
				}
				String str;
				// 检查是否UTF8
				if (StringUtil.isUtf8(line.getBytes(), 0, line.getSize())) {
					str = new String(line.getBytes(), 0, line.getSize(), "utf-8");
					// 特意增加一个charset的header
					header = new WebForm.Header("charset", "utf-8");
					headers.add(header);
				} else {
					str = new String(line.getBytes(), 0, line.getSize(), "gbk");
				}
				int idx = str.indexOf(':');
				if (-1 != idx) {
					int beginValue = idx + 1;
					if (' ' == str.charAt(beginValue)) {
						++beginValue;
					}
					header = new WebForm.Header(str.substring(0, idx), str.substring(beginValue));
					headers.add(header);
				}
				buf.reset();
				continue;
			}
			buf.write(ret);
		}
		return headers;
	}

	/**
	 * 根据mutilpart边界对内容进行截流
	 * 
	 * @author liangyi
	 * 
	 */
	protected class PartInputStream extends InputStream {
		/** 缓冲区 */
		byte[] m_Buffer;
		/** 已在缓冲区中的数据量 */
		int m_BufferSize;
		/** 已由缓冲区读取的位置 */
		int m_ReadOffset;
		/** 已读取的总数 */
		int m_Size;
		/** 内容结束的边界空间（边界串及前面的回车+换行） */
		int m_BoundarySize;
		/** 可能是当前表单内容结束（边界开始的）位置 */
		int m_PartEndBoundary;
		/** 可能的边界位置 */
		int m_BoundaryOffset;

		PartInputStream() {
			m_BoundarySize = m_Boundary.length + 2;
			m_Buffer = new byte[m_BoundarySize * 2 + 2];
			m_Size = 0;
			m_ReadOffset = 0;
			m_BufferSize = 0;
		}

		/**
		 * 检查是否已到边界
		 * 
		 * @return 是否到达边界
		 */
		private boolean checkBoundary() throws IOException {
			int count, ret;
			// 看看是否边界
			if (m_BufferSize - m_BoundaryOffset >= m_Boundary.length
					&& 0 == Bytes.compare(m_Buffer, m_BoundaryOffset, m_Boundary, 0, m_Boundary.length)) {
				// 是结束边界:)，要把边界后的换行符读取走
				count = m_BufferSize - m_BoundaryOffset - m_Boundary.length;
				if (count > 0) {
					// 数据读多了？
					throw new IOException("数据读多了？");
				}
				// 读到换行符为止
				for (count = 0;; count++) {
					ret = m_Input.read();
					if ('\n' == ret) {
						// 换行符，很好
						break;
					}
					if (ret < 0) {
						// 流结束了？
						throw new IOException("意外结束（边界后没有换行符）");
					}
					if ('-' != ret && '\r' != ret) {
						throw new IOException("边界异常[" + count + "](" + ret + ')');
					}
				}

				// m_PartEndBoundary = m_ReadOffset;
				m_BufferSize = -1;
				return true;
			}
			return false;
		}

		/**
		 * 读取内容到缓冲区
		 * 
		 * @return 返回false表示已结束
		 * @throws IOException IO异常
		 */
		protected boolean fullBuffer() throws IOException {
			if (m_ReadOffset < m_PartEndBoundary) {
				// 缓冲区还有数据能读
				return true;
			}
			if (m_BufferSize < 0) {
				// 当前部分的内容已经读取完了
				return false;
			}
			int count = m_BufferSize - m_PartEndBoundary;
			int remain;
			if (count < 1) {
				m_BufferSize = m_PartEndBoundary = m_ReadOffset = 0;
				m_BoundaryOffset = 0;
				remain = 0;
			} else {
				remain = m_Buffer.length - m_PartEndBoundary;
				if (remain < m_BoundarySize) {
					// 若缓冲区不够空间读取完整边界，把mabayBoundary后面的数据迁移到缓冲区前端
					System.arraycopy(m_Buffer, m_PartEndBoundary, m_Buffer, 0, count);
					m_BoundaryOffset = (m_BoundaryOffset > m_ReadOffset) ? m_BoundaryOffset - m_ReadOffset : 0;
					m_ReadOffset = m_PartEndBoundary = 0;
					m_BufferSize = count;
				}
			}
			// 保持缓冲区有自m_MabayBoundary开始的m_BoundaryLength数据量
			count = m_BoundarySize - count;
			int ret;
			do {
				ret = m_Input.read(m_Buffer, m_BufferSize, count);
				if (ret < 0) {
					// 这就结束了？太意外了
					throw new IOException("内容未完整，意外结束！");
				}
				m_BufferSize += ret;
				count = m_BoundarySize - (m_BufferSize - m_PartEndBoundary);
			} while (count > 0);

			if (m_PartEndBoundary > 0) {
				if (m_BoundaryOffset > 0 && m_BoundaryOffset + 1 < m_BufferSize && '\n' == m_Buffer[m_BoundaryOffset]
						&& '\r' == m_Buffer[m_BoundaryOffset - 1]) {
					// 也许上次刚好只读取到 '\r'就结束
					++m_BoundaryOffset;
				}
				if (checkBoundary()) {
					// 到达边界了
					return false;
				}
				++m_PartEndBoundary;
			}
			// 查找换行符“\r\n”
			for (; m_PartEndBoundary < m_BufferSize; m_PartEndBoundary++) {
				if ('\r' == m_Buffer[m_PartEndBoundary]) {
					// 可能是边界前的换行符
					m_BoundaryOffset = m_PartEndBoundary + 1;
					if (m_BoundaryOffset == m_BufferSize) {
						// 最后的字符是‘\r’
						break;
					}
					if ('\n' == m_Buffer[m_BoundaryOffset]) {
						++m_BoundaryOffset;
						// 检查是否为边界，且确保有数据可以读
						if (checkBoundary() || m_PartEndBoundary > m_ReadOffset) {
							break;
						}
						++m_PartEndBoundary;
					}
				}
			}
			if (m_ReadOffset < m_PartEndBoundary) {
				return true;
			}
			return false;
		}

		public int read() throws IOException {
			if (fullBuffer()) {
				byte ret = m_Buffer[m_ReadOffset++];
				return ret < 0 ? 0x100 + ret : ret;
			}
			return -1;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int total = 0;
			int count;
			while (len > 0 && fullBuffer()) {
				count = m_PartEndBoundary - m_ReadOffset;
				if (count > len) {
					count = len;
				}
				System.arraycopy(m_Buffer, m_ReadOffset, b, off, count);
				m_ReadOffset += count;
				total += count;
				len -= count;
				off += count;
			}
			return (0 == total && len > 0) ? -1 : total;
		}

		/**
		 * 跳过内容直致到流结束
		 * 
		 * @throws IOException IO异常
		 */
		final void skipAtEnd() throws IOException {
			while (fullBuffer()) {
				m_ReadOffset = m_PartEndBoundary;
			}
		}

		/**
		 * 直接转送到输入流
		 * 
		 * @param out 要传存到的流
		 * @return 流大小
		 * @throws IOException IO异常
		 */
		public long transfer(OutputStream out) throws IOException {
			long count = 0;
			int ret;
			while (fullBuffer()) {
				ret = m_PartEndBoundary - m_ReadOffset;
				out.write(m_Buffer, m_ReadOffset, ret);
				m_ReadOffset += ret;
				count += ret;
			}
			return count;
		}
	}

}
