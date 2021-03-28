package cn.weforward.framework.test;

import org.junit.Test;

import cn.weforward.common.util.ClassUtil;
import cn.weforward.framework.Topic;
import cn.weforward.framework.TopicListener;

public class ClassTest {
	@Test
	public void test() {
		Object m_Listener = new EticketListener() {

			@Override
			public String getTopic() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void onReceive(Topic topic, String content) {
				// TODO Auto-generated method stub

			}

		};
		Class<?> m_ContentClass = ClassUtil.find(m_Listener.getClass(), TopicListener.class, "E");
		System.out.println(m_ContentClass);
	}
}
