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
package cn.weforward.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import cn.weforward.common.crypto.Hex;
import cn.weforward.common.sys.StackTracer;
import cn.weforward.common.util.StringUtil;

/**
 * 联合标识（以便在整个系统中比较简单实现唯一标识）
 * 
 * 由三部分组成：
 * <p>
 * 类型名（对象/类）
 * <p>
 * 序号（同类型下的唯一标识）
 * <p>
 * 名称/说明
 * <p>
 * 如：“user$12345678!小明”；“12345678”；“12345678!小明”；“user$12345678”
 * 
 * 分隔符尽量避免在URL中传输需要编码（符号“$-_.+!*'(),”不需要编码，但“+”表示空格）
 * 
 * @author liangyi,daibo
 * 
 */
public class UniteId implements Serializable, UniqueKey {

	private static final long serialVersionUID = 1L;
	/** 日志记录器 */
	protected final static org.slf4j.Logger _Logger = LoggerFactory.getLogger(UniteId.class);
	/** 联合标识中类型与序号间的分隔号 */
	public static final char TYPE_SPEARATOR = '$';
	/** 联合标识中序号与描述间的分隔号 */
	public static final char UNITE_SPEARATOR = '!';
	/** 子对象分隔号 */
	public static final char OBJECT_SPEARATOR = '.';
	/** 前缀分隔号 */
	public static final char PREFIX_SPEARATOR = '_';

	/** 换行分隔号 */
	public static final char LN_SPEARATOR = '\n';
	/** 转义符 */
	public static final char ESCAPE = '^';

	/** 空的联合ID */
	public static final UniteId _nil = new UniteId(null);

	/** 是否检查分隔符 */
	protected final static boolean _SpeartorChecked = ("true"
			.equals(System.getProperty("UniteId.SpeartorChecked", "false")));
	/** 使用Map来缓存，查表加速 */
	protected static volatile Map<Class<?>, String> _SimpleNameCache = Collections.emptyMap();
	/** 格式串 */
	protected final String m_Unite;
	/** 类型与序号间的分隔位置 */
	protected final int m_IndexType;
	/** 序号与描述间的分隔位置 */
	protected final int m_IndexId;
	/** 数值型的序号 */
	protected int m_IntOrdinal;

	/**
	 * 构建联合ID
	 * 
	 * @param uniteid 联合ID格式串，如：user$12345678!小明
	 */
	public UniteId(String uniteid) {
		if (null == uniteid || 0 == uniteid.length()) {
			m_IndexType = 0;
			m_IndexId = 0;
			m_Unite = "";
			m_IntOrdinal = 0;
			return;
		}
		int idxId = -1;
		// 先找类型分隔符
		int idxType = uniteid.indexOf(TYPE_SPEARATOR);
		if (-1 != idxType) {
			// 不是旧格式，找描述分隔符
			idxId = uniteid.indexOf(UNITE_SPEARATOR, idxType);
		}
		if (-1 == idxType && -1 == idxId) {
			// 尝试找描述分隔符
			idxId = uniteid.indexOf(UNITE_SPEARATOR, 0);
		}
		idxType = (-1 == idxType) ? 0 : (idxType + 1);
		if (-1 == idxId) {
			idxId = uniteid.length();
		}
		m_IndexType = idxType;
		m_IndexId = idxId;
		m_Unite = uniteid;
		m_IntOrdinal = Integer.MIN_VALUE;
	}

	protected UniteId(int ordinal, String caption, String type) {
		StringBuilder sb = new StringBuilder();
		if (!StringUtil.isEmpty(type)) {
			sb.append(type).append(TYPE_SPEARATOR);
			m_IndexType = type.length() + 1;
		} else {
			m_IndexType = 0;
		}
		Hex.toHexFixed(ordinal, sb);
		m_IndexId = sb.length();
		if (!StringUtil.isEmpty(caption)) {
			sb.append(UNITE_SPEARATOR).append(caption);
		}
		m_IntOrdinal = ordinal;
		m_Unite = sb.toString();
	}

	protected UniteId(String ordinal, int intOrdinal, String caption, String type) {
		m_IntOrdinal = intOrdinal;
		if (!StringUtil.isEmpty(type)) {
			if (_SpeartorChecked) {
				if (-1 != ordinal.indexOf(TYPE_SPEARATOR) || -1 != ordinal.indexOf(UNITE_SPEARATOR)) {
					throw new IllegalArgumentException("联合ID的序号中包含有分隔符（若是必须的应使用UniteId.escapeOrdinal编码）：" + ordinal);
				}
			} else if (_Logger.isDebugEnabled()) {
				if (-1 != ordinal.indexOf(TYPE_SPEARATOR) || -1 != ordinal.indexOf(UNITE_SPEARATOR)) {
					_Logger.debug(StackTracer.printStackTrace(Thread.currentThread(),
							(new StringBuilder("联合ID的序号中包含有分隔符（若是必须的应使用UniteId.escapeOrdinal编码）：")).append(ordinal)
									.append('\n'))
							.toString());
				}
			}
			ordinal = (type + TYPE_SPEARATOR + ordinal);
			m_IndexType = type.length() + 1;
		} else {
			m_IndexType = 0;
		}
		if (!StringUtil.isEmpty(caption)) {
			m_IndexId = ordinal.length();
			m_Unite = ordinal + UNITE_SPEARATOR + caption;
		} else {
			m_Unite = ordinal;
			m_IndexId = ordinal.length();
		}
	}

	/**
	 * 取得类型部分
	 * 
	 * @return 类型名
	 */
	public String getType() {
		if (m_IndexType > 0) {
			return m_Unite.substring(0, m_IndexType - 1);
		}
		return null;
	}

	/**
	 * 返回序号及类型部分的标识串（不包含名称/描述部分），与getUuid()方法等同
	 * 
	 * @return 标识串
	 */
	public String getId() {
		if (m_Unite.length() == m_IndexId) {
			return m_Unite;
		}
		return m_Unite.substring(0, m_IndexId);
	}

	@Override
	public String getKey() {
		return getId();
	}

	/**
	 * 取得序号部分
	 * 
	 * @return 序号串
	 */
	public String getOrdinal() {
		if (0 == m_IndexId) {
			return "";
		}
		return m_Unite.substring(m_IndexType, m_IndexId);
	}

	/**
	 * 返回整数值的序号部分（若它是数值的话）
	 * 
	 * @return 数值型序号
	 */
	public int getIntOrdinal() {
		if (Integer.MIN_VALUE == m_IntOrdinal) {
			// 尝试转换数值的序号
			String uniteid = m_Unite.substring(m_IndexType, m_IndexId);
			if (8 == uniteid.length()) {
				// 刚好是8个字符，尝试按16进转为数值
				m_IntOrdinal = Hex.parseHex(uniteid, 8, 0);
			} else if (uniteid.length() > 8 && uniteid.charAt(8) == PREFIX_SPEARATOR) {
				// 大于8个字符，且第9个字符为前缀分隔号，尝试把前8个字符按16进转为数值
				m_IntOrdinal = Hex.parseHex(uniteid, 8, 0);
			} else {
				m_IntOrdinal = 0;
			}
		}
		return m_IntOrdinal;
	}

	/**
	 * 取得名称/说明部分
	 * 
	 * @return 描述串
	 */
	public String getCaption() {
		if (m_Unite.length() == m_IndexId) {
			return "";
		}
		return m_Unite.substring(m_IndexId + 1);
	}

	/**
	 * 返回序号及类型部分的标识串（不包含名称/描述部分），与getId()方法等同
	 * 
	 * @return 标识串
	 */
	public String getUuid() {
		return getId();
	}

	/**
	 * 返回不要类型前缀的ID
	 * 
	 * @return 保留序号及描述的ID串
	 */
	public String withoutType() {
		if (m_IndexType > 0) {
			return m_Unite.substring(m_IndexType);
		}
		return m_Unite;
	}

	/**
	 * 返回不要序号后缀及描述的联合标识串（根据PREFIX_SPEARATOR截断）
	 * 
	 * @return 保留类型序号前缀的联合标识串
	 */
	public String withoutPostfix() {
		int idx = m_Unite.indexOf(PREFIX_SPEARATOR, m_IndexType);
		if (idx < m_IndexType || idx >= m_IndexId) {
			// 没有后缀，直接就是getId()
			return getId();
		}
		return m_Unite.substring(0, idx);
	}

	/**
	 * 是否声明描述段（检查有没有序号与描述间的分隔符）
	 * 
	 * @return true/false
	 */
	public boolean isDeclareCaption() {
		return (m_IndexId > 0) && (m_Unite.length() != m_IndexId);
	}

	/**
	 * 是否声明类型段（检查有没有类型与序号间的分隔符）
	 * 
	 * @return true/false
	 */
	public boolean isDeclareType() {
		return (m_IndexType > 0);
	}

	/**
	 * 替换或增加类型部分
	 * 
	 * @param type 新类型
	 * @return 转换后的联合标识
	 */
	public UniteId changeType(Class<?> type) {
		return changeType(getType(type));
	}

	/**
	 * 替换或增加类型部分
	 * 
	 * @param type 新类型
	 * @return 转换后的联合标识
	 */
	public UniteId changeType(String type) {
		if (StringUtil.eq(type, getType())) {
			// 没变化
			return this;
		}
		return new UniteId(getOrdinal(), getIntOrdinal(), getCaption(), type);
	}

	/**
	 * 替换或增加描述部分
	 * 
	 * @param caption 新描述
	 * @return 转换后的联合标识
	 */
	public UniteId changeCaption(String caption) {
		if (StringUtil.eq(caption, getCaption())) {
			// 没变化
			return this;
		}
		return new UniteId(getOrdinal(), getIntOrdinal(), caption, getType());
	}

	/**
	 * 是否为空
	 * 
	 * @return true/false
	 */
	public boolean isEmpty() {
		return (m_Unite.length() == 0);
	}

	/**
	 * 比较方法
	 * 
	 * @param ordinal 序号
	 * @return true/false
	 */
	public boolean equals(String ordinal) {
		return getId().equals(getId(ordinal));
	}

	/**
	 * 比较方法
	 * 
	 * @param ordinal 序号
	 * @return true/false
	 */
	public boolean equals(int ordinal) {
		return m_IntOrdinal == ordinal;
	}

	/**
	 * 联合标识是否为空
	 * 
	 * @param unid 联合标识
	 * @return true/false
	 */
	public static final boolean isEmtpy(UniteId unid) {
		return (null == unid || 0 == unid.m_Unite.length());
	}

	/**
	 * 比较方法
	 * 
	 * @param id1 id对象
	 * @param id2 id对象
	 * @return true/false
	 */
	public static boolean equals(String id1, String id2) {
		return getId(id1).equals(getId(id2));
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param uniteid 联合标识串
	 * @return 联合标识对象
	 */
	public static UniteId valueOf(String uniteid) {
		if (null == uniteid || 0 == uniteid.length()) {
			return _nil;
		}
		return new UniteId(uniteid);
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param ordinal 序号部分
	 * @param type    类型名部分
	 * @param caption 名称/描述部分
	 * @return 联合标识
	 */
	public static UniteId valueOf(String ordinal, String type, String caption) {
		return new UniteId(ordinal, 0, caption, type);
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param ordinal    序号部分
	 * @param intOrdinal 整数序号
	 * @param type       类型名部分
	 * @param caption    名称/描述部分
	 * @return 联合标识
	 */
	public static UniteId valueOf(String ordinal, int intOrdinal, String type, String caption) {
		return new UniteId(ordinal, intOrdinal, caption, type);
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param ordinal 序号部分
	 * @param type    类型名部分
	 * @param caption 名称/描述部分
	 * @return 联合标识
	 */
	public static UniteId valueOf(int ordinal, String type, String caption) {
		return new UniteId(ordinal, caption, type);
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param ordinal 序号部分
	 * @param type    对象类
	 * @return 联合标识
	 */
	public static UniteId valueOf(String ordinal, Class<?> type) {
		return new UniteId(ordinal, 0, null, getType(type));
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param ordinal    序号部分
	 * @param intOrdinal 整数序号
	 * @param type       对象类
	 * @return 联合标识
	 */
	public static UniteId valueOf(String ordinal, int intOrdinal, Class<?> type) {
		if (StringUtil.isEmpty(ordinal) && 0 == intOrdinal) {
			return _nil;
		}
		return new UniteId(ordinal, intOrdinal, null, getType(type));
	}

	/**
	 * 转为联合标识对象
	 * 
	 * @param ordinal 序号部分
	 * @param type    对象类
	 * @return 联合标识
	 */
	public static UniteId valueOf(int ordinal, Class<?> type) {
		return new UniteId(ordinal, null, getType(type));
	}

	/**
	 * 由类型及数值序号组成联合标识串
	 * 
	 * @param type    类型
	 * @param ordinal 序号
	 * @return 标识串
	 */
	public static String getUniteId(String type, int ordinal) {
		return getUniteId(ordinal, null, type);
	}

	/**
	 * 由类型及数值序号组成联合标识串
	 * 
	 * @param type    类型
	 * @param ordinal 序号
	 * @return 标识串
	 */
	public static String getUniteId(Class<?> type, int ordinal) {
		return getUniteId(ordinal, null, getType(type));
	}

	/**
	 * 由类型及序号组成联合标识串
	 * 
	 * @param type    类型
	 * @param ordinal 序号
	 * @return 标识串
	 */
	public static String getUniteId(Class<?> type, String ordinal) {
		return getUniteId(ordinal, null, getType(type));
	}

	/**
	 * 由数值序号及名称/说明组成联合标识串
	 * 
	 * @param ordinal 序号
	 * @param caption 说明
	 * @return 标识串
	 */
	public static String getUniteId(int ordinal, String caption) {
		return getUniteId(Hex.toHex32(ordinal), caption, null);
	}

	/**
	 * 由数值序号、名称/说明及类型组成联合标识串
	 * 
	 * @param ordinal 序号
	 * @param caption 说明
	 * @param type    类型
	 * @return 标识串
	 */
	public static String getUniteId(int ordinal, String caption, String type) {
		StringBuilder sb = new StringBuilder();
		if (null != type && type.length() > 0) {
			sb.append(type);
			sb.append(TYPE_SPEARATOR);
		}
		Hex.toHexFixed(ordinal, sb);
		if (null != caption) {
			sb.append(UNITE_SPEARATOR);
			sb.append(caption);
		}
		return sb.toString();
	}

	/**
	 * 由序号、名称/说明及类型组成联合标识串
	 * 
	 * @param ordinal 序号
	 * @param caption 说明
	 * @param type    类型
	 * @return 标识串
	 */
	public static String getUniteId(String ordinal, String caption, String type) {
		StringBuilder sb = new StringBuilder();
		if (null != type && type.length() > 0) {
			sb.append(type);
			sb.append(TYPE_SPEARATOR);
		}
		sb.append(ordinal);
		if (null != caption) {
			sb.append(UNITE_SPEARATOR);
			sb.append(caption);
		}
		return sb.toString();
	}

	/**
	 * 取得类型部分
	 * 
	 * @param uniteid 联合id
	 * @return 标识串
	 */
	public static String getType(String uniteid) {
		return UniteId.valueOf(uniteid).getType();
	}

	/**
	 * 由对象类型取得类型名
	 * 
	 * @param classOf 类
	 * @return 类型
	 */
	public static String getType(Class<?> classOf) {
		return (null == classOf) ? null : getSimpleName(classOf);
	}

	/**
	 * 返回序号及类型部分的标识串（不包含名称/描述部分），与getUuid()方法等同
	 * 
	 * @param uniteid 联合标识串
	 * @return 标识串
	 */
	public static String getId(String uniteid) {
		return UniteId.valueOf(uniteid).getId();
	}

	/**
	 * 返回序号及类型部分的标识串（不包含名称/描述部分），与getId()方法等同
	 * 
	 * @param uniteid 联合标识串
	 * @return 标识串
	 */
	public static String getUuid(String uniteid) {
		return UniteId.valueOf(uniteid).getId();
	}

	/**
	 * 取得序号部分（数值型）
	 * 
	 * @param uniteid 联合标识串
	 * @return 数值型
	 */
	public static int getIntOrdinal(String uniteid) {
		return UniteId.valueOf(uniteid).getIntOrdinal();
	}

	/**
	 * 只返回序号部分
	 * 
	 * @param uniteid 联合标识串
	 * @return 标识串
	 */
	public static String getOrdinal(String uniteid) {
		return UniteId.valueOf(uniteid).getOrdinal();
	}

	/**
	 * 取得名称/说明部分
	 * 
	 * @param uniteid 联合标识串
	 * @return 名称
	 */
	public static String getCaption(String uniteid) {
		return UniteId.valueOf(uniteid).getCaption();
	}

	/**
	 * 修正标识串为只有ID部分（类型及序号），若没有类型部分则把type补充上去
	 * 
	 * @param unitid 标识串
	 * @param clazz  类型
	 * @return 调整后的联合标识
	 */
	public static UniteId fixId(String unitid, Class<?> clazz) {
		return fixId(unitid, getType(clazz));
	}

	/**
	 * 修正标识串为只有ID部分（类型及序号），若没有类型部分则把type补充上去
	 * 
	 * @param unitid 标识串
	 * @param type   类型名
	 * @return 调整后的联合标识
	 */
	public static UniteId fixId(String unitid, String type) {
		if (-1 != unitid.indexOf(TYPE_SPEARATOR)) {
			// 已有分隔符
			return valueOf(unitid);
		}
		int idx = unitid.indexOf(UNITE_SPEARATOR);
		if (-1 == idx) {
			// 没找到TYPE_SPEARATOR及UNITE_SPEARATOR加上type即可
			return valueOf(unitid, type, null);
		}
		// 找到有UNITE_SPEARATOR去除caption部分
		return valueOf(unitid.substring(0, idx), type, null);
	}

	/**
	 * 替换类型部分（若标识串中有的话）
	 * 
	 * @param uniteid 联合标识串
	 * @param type    类型
	 * @return 替换后的联合标识串
	 */
	public static String changeType(String uniteid, String type) {
		int idx = uniteid.indexOf(TYPE_SPEARATOR);
		if (-1 == idx) {
			// 没找到TYPE_SPEARATOR，直接加上type就行了
			return getUniteId(uniteid, null, type);
		}
		return (type + uniteid.substring(idx));
	}

	/**
	 * 替换类型部分（若标识串中有的话）
	 * 
	 * @param ordinal 序号
	 * @param type    对象类
	 * @return 替换后的联合标识串
	 */
	public static String changeType(String ordinal, Class<?> type) {
		return changeType(ordinal, getType(type));
	}

	/**
	 * 编码序号中可能存在的分隔符
	 * 
	 * @param ordinal 编码前的序号
	 * @see #unescapeOrdinal(String)
	 * @return 编码后的序号
	 */
	public static String escapeOrdinal(String ordinal) {
		return ordinal.replace(TYPE_SPEARATOR, ESCAPE);
	}

	/**
	 * 还原序号中可能存在的分隔符
	 * 
	 * @see #escapeOrdinal(String)
	 * @param ordinal 编码后的序号
	 * @return 解码后的序号
	 */
	public static String unescapeOrdinal(String ordinal) {
		return ordinal.replace(ESCAPE, TYPE_SPEARATOR);
	}

	/**
	 * 取得类用于标识/映射对象的短名（主要代替Class.getSimpleName，性能高于其将近10倍）
	 * 
	 * @param clazz 类，如 #cn.weforward.common.UniteId
	 * @return 短名，如 UniteId
	 */
	public static String getSimpleName(Class<?> clazz) {
		String name;
		Map<Class<?>, String> cache = _SimpleNameCache;
		name = cache.get(clazz);
		if (null == name) {
			// synchronized (Metatype.class) {
			name = clazz.getSimpleName();
			cache = new HashMap<Class<?>, String>(cache);
			cache.put(clazz, name);
			_SimpleNameCache = cache;
			// }
		}
		return name;
	}

	public String stringValue() {
		return m_Unite;
	}

	@Override
	public String toString() {
		return m_Unite;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (null == obj) {
			return false;
		}

		if (obj instanceof UniteId) {
			return getId().equals(((UniteId) obj).getId());
		}

		return getId().equals(getId(obj.toString()));
	}

}
