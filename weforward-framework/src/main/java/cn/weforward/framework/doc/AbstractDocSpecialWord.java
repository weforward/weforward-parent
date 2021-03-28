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
import java.util.List;

import cn.weforward.common.NameItem;
import cn.weforward.common.NameItems;
import cn.weforward.protocol.doc.DocSpecialWord;

/**
 * 特殊名词抽象实现
 * 
 * @author daibo
 *
 */
public abstract class AbstractDocSpecialWord implements DocSpecialWord {

	protected String m_Name;
	protected String m_Description;
	protected Item m_Header;
	protected List<Item> m_Items;

	public AbstractDocSpecialWord(String name, String description) {
		m_Name = name;
		m_Description = description;
		m_Items = new ArrayList<>();
	}

	@Override
	public String getName() {
		return m_Name;
	}

	@Override
	public String getDescription() {
		return m_Description;
	}

	/**
	 * 设置表头
	 * 
	 * @param key   键
	 * @param value 值
	 */
	public void setTabelHeader(String key, String value) {
		setTabelHeader(key, value, null);
	}

	/**
	 * 设置表头
	 * 
	 * @param key         键
	 * @param value       值
	 * @param description 描述
	 */
	public void setTabelHeader(String key, String value, String description) {
		setTabelHeader(new ItemImpl(key, value, description));
	}

	/**
	 * 设置表头
	 * 
	 * @param item 项
	 */
	public void setTabelHeader(Item item) {
		m_Header = item;
	}

	@Override
	public Item getTableHeader() {
		return m_Header;
	}

	/**
	 * 添加表格项目
	 * 
	 * @param nis 项
	 */
	public void addTableItem(NameItems nis) {
		for (NameItem ni : nis) {
			addTableItem(ni.getValue(), ni.getName(), null);
		}
	}

	/**
	 * 添加表格项目
	 * 
	 * @param ni 项
	 */
	public void addTableItem(NameItem ni) {
		addTableItem(ni.getValue(), ni.getName(), null);
	}

	/**
	 * 添加表格项目
	 * 
	 * @param key   键
	 * @param value 值
	 */
	public void addTableItem(String key, String value) {
		addTableItem(key, value, null);
	}

	/**
	 * 添加表格项目
	 * 
	 * @param key         键
	 * @param value       值
	 * @param description 描述
	 */
	public void addTableItem(String key, String value, String description) {
		m_Items.add(new ItemImpl(key, value, description));
	}

	/**
	 * 添加表格项目
	 * 
	 * @param item 项
	 */
	public void addTableItem(Item item) {
		m_Items.add(item);
	}

	@Override
	public List<Item> getTableItems() {
		return m_Items;
	}

	/**
	 * 项目实现
	 * 
	 * @author daibo
	 *
	 */
	public static class ItemImpl implements Item {

		protected String m_Key;
		protected String m_Value;
		protected String m_Description;

		public ItemImpl(String key, String value, String description) {
			m_Key = key;
			m_Value = value;
			m_Description = description;
		}

		@Override
		public String getKey() {
			return m_Key;
		}

		@Override
		public String getValue() {
			return m_Value;
		}

		@Override
		public String getDescription() {
			return m_Description;
		}

	}

}
