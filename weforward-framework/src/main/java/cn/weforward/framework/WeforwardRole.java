package cn.weforward.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色规则
 * 
 * @author daibo
 *
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface WeforwardRole {
	/**
	 * 允许调用方法的角色
	 * 
	 * @return 角色id列表
	 */
	int[] allow() default {};

	/**
	 * 不允许调用方法的角色
	 * 
	 * @return 角色id列表
	 */
	int[] disallow() default {};
}
