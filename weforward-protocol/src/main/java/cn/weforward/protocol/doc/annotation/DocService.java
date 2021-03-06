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
package cn.weforward.protocol.doc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 描述一个微服务的注解
 * 
 * @author zhangpengji
 *
 */
@Documented
@Target(value = ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DocService {

	/**
	 * 微服务的名称
	 */
	String name();

	/**
	 * 微服务的版本
	 */
	String version();

	/**
	 * 微服务说明
	 */
	String description() default "";

	/**
	 * 修改记录。
	 * <p>
	 * 取值指向一个文本文件，文件采用csv格式，文件的每一行表示一个修改记录。
	 * <p>
	 * 默认情况读取当前包路径下的{service_name}.modify.csv文件
	 */
	String modifyPath() default "";

	/**
	 * 对象列表
	 */
	Class<?>[] objects() default {};

	/**
	 * 对象所在包路径。如：cn.weforward.user
	 */
	String[] objectPackage() default {};

	/**
	 * 描述微服务状态码的类，通常继承cn.weforward.protocol.ext.CommonServiceCodes，并添加其他状态码
	 * 
	 */
	Class<?> statusCodeClass() default Void.class;
}
