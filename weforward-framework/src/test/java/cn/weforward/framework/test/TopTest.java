package cn.weforward.framework.test;

import org.junit.Test;

import cn.weforward.common.util.ClassUtil;
import cn.weforward.framework.TopicListener;

public class TopTest {
	@Test
	public void test() {
		Class<?> clazz = ClassUtil.find(MessageService.class, TopicListener.class, "E");
		System.out.println(clazz);
	}
}
