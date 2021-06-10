package cn.weforward.common.util;

import java.util.Comparator;

/**
 * 用于支撑查找及排序的比较器接口
 * <p>
 * 
 * compareTo(E element被比较项, Object
 * key比较值)，若element&gt;key返回&gt;0，若小于返回&lt;0，若相等返回0
 * 
 * @author liangyi
 * 
 * @param <E> 比较项类型
 * @param <K> 比较键类型
 */
public interface ComparatorExt<E, K> extends Comparator<E> {
	/**
	 * 比较element与key，若element&gt;key返回&gt;0，若小于返回&lt;0，若相等返回0
	 * 
	 * @param element 被比较项
	 * @param key     比较键值
	 * @return 若element&gt;key返回&gt;0，若小于返回&lt;0，若相等返回0
	 */
	int compareTo(E element, K key);
}
