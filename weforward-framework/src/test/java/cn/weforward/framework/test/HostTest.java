package cn.weforward.framework.test;

import org.junit.Test;

import cn.weforward.framework.ext.WeforwardService;

public class HostTest {
	@Test
	public void test() {
		System.out.println(WeforwardService.genHost("10.*"));
	}
}
