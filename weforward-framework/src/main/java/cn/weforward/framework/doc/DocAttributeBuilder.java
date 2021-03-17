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

import java.util.Collection;
import java.util.Date;

import cn.weforward.protocol.datatype.DataType;
import cn.weforward.protocol.doc.DocAttribute;
import cn.weforward.protocol.doc.DocObject;
import cn.weforward.protocol.support.doc.DocAttributeVo;
import cn.weforward.protocol.support.doc.DocObjectVo;

/**
 * {@link DocAttribute} 构建者
 * 
 * @author daibo
 *
 */
public class DocAttributeBuilder {
	/** 值vo */
	private DocAttributeVo m_Vo;

	/**
	 * 构造
	 * 
	 * @param vo
	 */
	private DocAttributeBuilder(DocAttributeVo vo) {
		m_Vo = vo;
	}

	/**
	 * 构建
	 * 
	 * @param name        属性名
	 * @param description 描述
	 * @param type        类型
	 * @return 构建者
	 */
	public static DocAttributeBuilder valueOf(String name, String description, Class<?> type) {
		DocAttributeVo vo = new DocAttributeVo();
		vo.name = name;
		vo.description = description;
		vo.type = getType(type);
		return new DocAttributeBuilder(vo);
	}

	/**
	 * 元素组件
	 * 
	 * @param clazz 类
	 * @return 构建者
	 */
	public DocAttributeBuilder component(Class<?> clazz) {
		m_Vo.component = getType(clazz);
		return this;
	}

	/**
	 * 示例
	 * 
	 * @param example 示例内容
	 * @return 构建者
	 */
	public DocAttributeBuilder example(String example) {
		m_Vo.example = example;
		return this;
	}

	/**
	 * 详情
	 * 
	 * @param detailBuilder 详情
	 * @return 构建者
	 */
	public DocAttributeBuilder detail(DocObjectBuilder detailBuilder) {
		return detail(detailBuilder.build());
	}

	/**
	 * 详情
	 * 
	 * @param detail 详情
	 * @return 构建者
	 */
	public DocAttributeBuilder detail(DocObject detail) {
		m_Vo.detail = DocObjectVo.valueOf(detail);
		return this;
	}

	/**
	 * 属性是否为必须
	 * 
	 * @param necessary 属性是否为必须
	 * @return 构建者
	 */
	public DocAttributeBuilder necessary(boolean necessary) {
		if (necessary) {
			m_Vo.marks |= DocAttribute.MARK_NECESSARY;
		} else {
			m_Vo.marks &= ~(-DocAttribute.MARK_NECESSARY);
		}
		return this;
	}

	/**
	 * 构建
	 * 
	 * @return 对象
	 */
	public DocAttribute build() {
		return m_Vo;
	}

	/* 获取类型 */
	private static String getType(Class<?> type) {
		if (null == type) {
			return null;
		}
		if (type == Void.class) {
			return null;
		}
		if (Number.class.isAssignableFrom(type) || type == short.class || type == int.class || type == Long.class
				|| type == float.class || type == long.class || type == byte.class) {
			return DataType.NUMBER.value;
		}
		if (CharSequence.class.isAssignableFrom(type)) {
			return DataType.STRING.value;
		}
		if (Boolean.class.isAssignableFrom(type) || type == boolean.class) {
			return DataType.BOOLEAN.value;
		}
		if (Date.class.isAssignableFrom(type)) {
			return DataType.DATE.value;
		}
		if (type.isArray()) {
			return DataType.LIST.value;
		}
		if (Collection.class.isAssignableFrom(type)) {
			return DataType.LIST.value;
		}
		return type.getSimpleName();
	}
}
