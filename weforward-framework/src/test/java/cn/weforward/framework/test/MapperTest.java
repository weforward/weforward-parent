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
package cn.weforward.framework.test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import cn.weforward.common.util.StringUtil;

public class MapperTest {

	public static Result<String, Double> get() {
		return null;
	}

	@Test
	public void test() throws Exception {
		Method method = MapperTest.class.getMethod("get");

		Class<?> clazz = method.getReturnType();
		Map<String, Class<?>> genericMap = new HashMap<>();

		Type type = method.getGenericReturnType();
		ParameterizedType ptype = (ParameterizedType) type;
		TypeVariable<?>[] typeParams = clazz.getTypeParameters();
		Type[] types = ptype.getActualTypeArguments();
		if (null != types && null != typeParams && types.length == typeParams.length) {
			for (int i = 0; i < types.length; i++) {
				genericMap.put(typeParams[i].getName(), (Class<?>) (types[i]));
			}
		}
		for (Method m : clazz.getMethods()) {
			String name = m.getName();
			if (StringUtil.eq(name, "getClass")) {
				continue;
			}
			if (name.startsWith("get")) {
				Type gtype = m.getGenericReturnType();
				if (gtype instanceof Class<?>) {
					System.out.println(m.getName() + ":" + (Class<?>) gtype + ":" + gtype.getTypeName());
				} else {
					System.out.println(
							m.getName() + ":" + genericMap.get(gtype.getTypeName()) + ":" + gtype.getTypeName());
				}

			}
		}
//		User val = new User();
//		val.setName("u1");
//		val.setAge(BigInteger.valueOf(10));
//		val.setAgeDetail(BigDecimal.valueOf(100.5d));
//
//		User u2 = new User();
//		u2.setName("u2");
//		User u31 = new User();
//		u31.setName("u31");
//		User u32 = new User();
//		u32.setName("u32");
//
//		val.setUser(u2);
//		val.setUsers(Arrays.asList(u31, u32));
//
//		DtBase dt = MappedUtil.toBase(val);
//		System.out.println(dt);
//		User back = MappedUtil.fromBase(User.class, dt);
//		System.out.println(back);
//		List<DocObjectVo> commons = new ArrayList<>();
//		DocObjectVo vo = DocumentMethod.toObject(commons, User.class);
//		print(vo, "");
	}

//	private void print(DocObject vo, String line) {
//		if (null == vo) {
//			return;
//		}
//		System.out.println(line + "name=" + vo.getName());
//		line += '-';
//		for (DocAttribute a : vo.getAttributes()) {
//
//			System.out.println(line + a.getName() + "=" + a.getType());
//			print(a.getDetail(), line);
//		}
//	}

}
