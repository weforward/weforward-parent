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

import java.util.ArrayList;

import cn.weforward.protocol.doc.DocAttribute;
import cn.weforward.protocol.doc.DocObject;
import cn.weforward.protocol.support.doc.DocAttributeVo;
import cn.weforward.protocol.support.doc.DocObjectVo;

/**
 * {@link DocObject} 构建者
 * 
 * @author daibo
 *
 */
public class DocObjectBuilder {
	/** 值vo */
	private DocObjectVo m_Vo;

	/**
	 * 构造
	 * 
	 * @param vo 值
	 */
	private DocObjectBuilder(DocObjectVo vo) {
		m_Vo = vo;
	}

	/**
	 * 构建
	 * 
	 * @param name        对象名称
	 * @param description 对象描述
	 * @return 构建者
	 */
	public static DocObjectBuilder valueOf(String name, String description) {
		DocObjectVo vo = new DocObjectVo();
		vo.name = name;
		vo.description = description;
		vo.attributes = new ArrayList<>();
		return new DocObjectBuilder(vo);
	}

	/**
	 * 属性
	 * 
	 * @param attrBuilder 属性
	 * @return 构建者
	 */
	public DocObjectBuilder attr(DocAttributeBuilder attrBuilder) {
		return attr(attrBuilder.build());
	}

	/**
	 * 属性
	 * 
	 * @param attr 属性
	 * @return 构建者
	 */
	public DocObjectBuilder attr(DocAttribute attr) {
		m_Vo.attributes.add(DocAttributeVo.valueOf(attr));
		return this;
	}

	/**
	 * 构建
	 * 
	 * @return 对象
	 */
	public DocObject build() {
		return m_Vo;
	}
}
