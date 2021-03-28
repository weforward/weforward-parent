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
package cn.weforward.framework.doc;

import cn.weforward.common.util.NumberUtil;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtNumber;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.datatype.DtString;
import cn.weforward.protocol.doc.annotation.DocAttribute;
import cn.weforward.protocol.doc.annotation.DocObject;

/**
 * 页参数
 * 
 * @author daibo
 *
 */
@DocObject(description = "页参数")
public class DocPageParams {

	protected Integer m_Page;

	protected Integer m_PageSize;

	@DocAttribute(description = "第几页", example = "1")
	public Integer getPage() {
		return m_Page;
	}

	@DocAttribute(description = "一页数据大小", example = "20")
	public Integer getPageSize() {
		return m_PageSize;
	}

	public Integer getPage(DtObject params) {
		m_Page = tryGetInteger(params, "page");
		return m_Page;
	}

	public Integer getpageSize(DtObject params) {
		m_PageSize = tryGetInteger(params, "page_size");
		return m_PageSize;
	}

	public static Integer tryGetInteger(DtObject params, String name) {
		DtBase base = params.getAttribute(name);
		if (base instanceof DtNumber) {
			return ((DtNumber) base).valueInt();
		} else if (base instanceof DtString) {
			return Integer.valueOf(NumberUtil.toInt(((DtString) base).value()));
		} else {
			return null;
		}
	}
}
