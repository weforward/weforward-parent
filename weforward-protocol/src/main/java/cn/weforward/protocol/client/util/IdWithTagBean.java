package cn.weforward.protocol.client.util;

import cn.weforward.common.DistributedObject;

public class IdWithTagBean {

	protected String m_Id;

	protected String m_Tag;

	public IdWithTagBean() {

	}

	public void setId(String v) {
		m_Id = v;
	}

	public String getId() {
		return m_Id;
	}

	public void setTag(String tag) {
		m_Tag = tag;
	}

	public String getTag() {
		return m_Tag;
	}

	@Override
	public String toString() {
		return "id:" + m_Id + "," + m_Tag;
	}

	/**
	 * 构造
	 * 
	 * @param id  对象id
	 * @param tag 对象源
	 * @return
	 */
	public static IdWithTagBean valueOf(String id, Object obj) {
		if (obj instanceof DistributedObject) {
			return valueOf(id, ((DistributedObject) obj).getDriveIt());
		}
		return valueOf(id, null);
	}

	/**
	 * 构造
	 * 
	 * @param id  对象id
	 * @param tag 对象源
	 * @return
	 */
	public static IdWithTagBean valueOf(String id, String tag) {
		IdWithTagBean b = new IdWithTagBean();
		b.setId(id);
		b.setTag(tag);
		return b;
	}

}
