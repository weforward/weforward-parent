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
package cn.weforward.framework.ext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.ResultPage;
import cn.weforward.framework.ApiException;
import cn.weforward.framework.ExceptionHandler;
import cn.weforward.framework.KeepServiceOrigin;
import cn.weforward.framework.WeforwardResource;
import cn.weforward.framework.WeforwardRole;
import cn.weforward.framework.WeforwardSession;
import cn.weforward.framework.doc.DocPageParams;
import cn.weforward.framework.support.AbstractApiMethod;
import cn.weforward.framework.support.Global;
import cn.weforward.protocol.Header;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.util.MappedUtil;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.ext.ObjectMapperSet;
import cn.weforward.protocol.ops.Role;
import cn.weforward.protocol.ops.User;
import cn.weforward.protocol.support.PageData;
import cn.weforward.protocol.support.datatype.FriendlyObject;

/**
 * 反射的方法
 * 
 * @author daibo
 *
 */
public class ReflectMethod extends AbstractApiMethod {
	/** 忽略关闭异常 */
	private static final Logger _Logger = LoggerFactory.getLogger(ReflectMethod.class);
	/** 方法对象 */
	protected Object m_Methods;
	/** 方法 */
	protected Method m_Method;
	/** 对象映射器集合 */
	protected ObjectMapperSet m_ObjectMapperSet;
	/** 调用前方法 */
	protected List<Method> m_Befores;
	/** 调用后方法 */
	protected List<Method> m_Afters;
	/** 异常调用方法 */
	protected List<Method> m_Exceptions;
	/** 参数提供者 */
	protected List<ArgsProvider> m_ArgsProviders;
	/** 指示网关返回此微服务实例的标识给调用方，后续调用将优先访问此实例 */
	protected boolean m_KeepServiceOrigin;
	/** 允许调用的角色 */
	protected List<Integer> m_AllowRoles;
	/** 不允许调用的角色 */
	protected List<Integer> m_DisallowRoles;

	public static ArgsProvider _GLOBAL_PROVIDER = new ArgsProvider() {
		@Override
		public void before(DtObject params) {
			DtBase base = params.getAttribute(Global.PARAMS_NAME);
			if (null != base) {
				Global.TLS.put(new Global(base));
			}
		}

		@Override
		public Object create(DtObject params, Class<?> clazz) {
			return Global.TLS.get();
		}

		@Override
		public boolean accept(Class<?> clazz) {
			return Global.class.isAssignableFrom(clazz);
		}

		@Override
		public void after(Object v) {

		}
	};

	public ReflectMethod(String name, Object methods, Method method) {
		super(name);
		m_Methods = methods;
		m_Method = method;
		m_ArgsProviders = new ArrayList<>();
		m_ArgsProviders.add(_GLOBAL_PROVIDER);
		Class<?> clazz = methods.getClass();
		m_KeepServiceOrigin = clazz.isAnnotationPresent(KeepServiceOrigin.class)
				|| method.isAnnotationPresent(KeepServiceOrigin.class);
		WeforwardRole roles = method.getAnnotation(WeforwardRole.class);
		if (null == roles) {
			roles = clazz.getAnnotation(WeforwardRole.class);
		}
		if (null != roles) {
			m_AllowRoles = toList(roles.allow());
			m_DisallowRoles = toList(roles.disallow());
		}
	}

	private static List<Integer> toList(int[] allow) {
		if (null == allow) {
			return null;
		}
		List<Integer> list = new ArrayList<>(allow.length);
		for (int v : allow) {
			list.add(v);
		}
		return list;
	}

	public void setObjectMapperSet(ObjectMapperSet m) {
		m_ObjectMapperSet = m;
	}

	/**
	 * 调用前方法
	 * 
	 * @param list 方法列表
	 */
	public void setBefores(List<Method> list) {
		m_Befores = list;
	}

	/**
	 * 调用后方法
	 * 
	 * @param list 方法列表
	 */
	public void setAfters(List<Method> list) {
		m_Afters = list;
	}

	/**
	 * 异常方法
	 * 
	 * @param list 方法列表
	 */
	public void setExceptions(List<Method> list) {
		m_Exceptions = list;
	}

	@Override
	public DtBase handle(String path, DtObject params, Request request, Response response) throws ApiException {
		if (m_KeepServiceOrigin) {
			response.setMarks(Response.MARK_KEEP_SERVICE_ORIGIN);
		}
		Object returnVal;
		try {
			Object v = beforeInvoke(path, params, request, response);
			Object args[] = newArgs(path, params, request, response, m_Method.getParameterTypes(), v);
			if (null == args) {
				returnVal = m_Method.invoke(m_Methods);
			} else {
				returnVal = m_Method.invoke(m_Methods, args);
			}
			returnVal = afterInvoker(path, params, request, response, returnVal);
		} catch (InvocationTargetException e) {
			Throwable target = e.getTargetException();
			try {
				target = exception(target);
			} catch (InvocationTargetException ee) {
				throw new IllegalArgumentException("方法调用失败", ee.getTargetException());
			} catch (IllegalAccessException | IllegalArgumentException ee) {
				throw new IllegalArgumentException("方法调用失败", ee);
			}
			if (target instanceof ApiException) {
				throw (ApiException) target;
			} else if (target instanceof RuntimeException) {
				throw (RuntimeException) target;
			} else {
				throw new IllegalArgumentException("方法调用失败", e);
			}
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new IllegalArgumentException("方法调用失败", e);
		}
		if (null == returnVal) {
			return null;
		}
		if (returnVal instanceof WeforwardResource) {
			WeforwardResource r = (WeforwardResource) returnVal;
			response.setResourceId(r.getId());
			response.setResourceExpire(r.getExpire());
			response.setResourceService(r.getService());
			returnVal = r.getData();
		}
		if (returnVal instanceof DtBase) {
			return (DtBase) returnVal;
		}
		if (returnVal instanceof ResultPage<?>) {
			ResultPage<?> rp = (ResultPage<?>) returnVal;
			Integer pageSize = DocPageParams.tryGetInteger(params, "page_size");
			if (null != pageSize) {
				rp.setPageSize(pageSize);
			}
			Integer page = DocPageParams.tryGetInteger(params, "page");
			if (page != null) {
				rp.gotoPage(page);
			} else {
				rp.gotoPage(1);
			}
			returnVal = new PageData(rp);
		}
		DtBase result = MappedUtil.toBase(returnVal, m_ObjectMapperSet);
		if (returnVal instanceof AutoCloseable) {
			try {
				((AutoCloseable) returnVal).close();
			} catch (Exception e) {
				_Logger.warn("忽略关闭异常", e);
			}
		}
		return result;

	}

	private Throwable exception(Throwable target)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (m_Methods instanceof ExceptionHandler) {
			target = ((ExceptionHandler) m_Methods).exception(target);
		}
		if (null != m_Exceptions) {
			for (Method m : m_Exceptions) {
				Class<?>[] params = m.getParameterTypes();
				if (null == params || params.length != 1) {
					continue;
				}
				Class<?> param = params[0];
				if (!param.isAssignableFrom(target.getClass())) {
					continue;
				}
				Class<?> returnType = m.getReturnType();
				if (returnType == void.class) {
					m.invoke(m_Methods, target);
				} else {
					target = (Throwable) m.invoke(m_Methods, target);
				}
			}
		}
		return target;
	}

	private Object[] newArgs(String path, DtObject params, Request request, Response response, Class<?>[] types,
			Object... objects) {
		if (null == types) {
			return null;
		}
		Object[] args = new Object[types.length];
		for (int i = 0; i < args.length; i++) {
			Class<?> clazz = types[i];
			if (null != objects) {
				Object match = null;
				for (Object val : objects) {
					if (null == val) {
						continue;
					}
					if (val.getClass().isAssignableFrom(clazz)) {
						match = val;
						break;
					}
				}
				if (null != match) {
					args[i] = match;
					break;
				}
			}
			if (FriendlyObject.class.isAssignableFrom(clazz)) {
				args[i] = new FriendlyObject(params);
			} else if (DtObject.class.isAssignableFrom(clazz)) {
				args[i] = params;
			} else if (Header.class.isAssignableFrom(clazz)) {
				args[i] = request.getHeader();
			} else if (String.class.isAssignableFrom(clazz)) {
				args[i] = path;
			} else if (Request.class.isAssignableFrom(clazz)) {
				args[i] = request;
			} else if (Response.class.isAssignableFrom(clazz)) {
				args[i] = response;
			} else {
				boolean accept = false;
				for (ArgsProvider p : m_ArgsProviders) {
					if (null == p || !p.accept(clazz)) {
						continue;
					}
					args[i] = p.create(params, clazz);
					accept = true;
					break;
				}
				if (!accept) {
					args[i] = MappedUtil.fromBase(clazz, null, params, m_ObjectMapperSet);
				}
			}
		}
		return args;
	}

	protected Object beforeInvoke(String path, DtObject params, Request request, Response response)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object v = null;
		for (ArgsProvider p : m_ArgsProviders) {
			p.before(params);
		}
		if (null != m_Befores) {
			for (Method m : m_Befores) {
				if (m.getReturnType() == void.class) {
					m.invoke(m_Methods, newArgs(path, params, request, response, null, m.getParameterTypes(), v));
				} else {
					v = (DtObject) m.invoke(m_Methods,
							newArgs(path, params, request, response, null, m.getParameterTypes(), v));
				}
			}
		}
		return v;

	}

	protected Object afterInvoker(String path, DtObject params, Request request, Response response, Object returnVal)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object v = returnVal;
		if (null != m_Afters) {
			for (Method m : m_Afters) {
				if (m.getReturnType() == void.class) {
					m.invoke(m_Methods, newArgs(path, params, request, response, m.getParameterTypes(), returnVal));
				} else {
					v = m.invoke(m_Methods, newArgs(path, params, request, response, m.getParameterTypes(), returnVal));
				}
			}
		}
		for (ArgsProvider p : m_ArgsProviders) {
			p.after(v);
		}
		return v;

	}

	Class<?> getMethods() {
		return m_Methods.getClass();
	}

	Method getMethod() {
		return m_Method;
	}

	public boolean isAllow(WeforwardSession session) {
		if (null != m_AllowRoles) {
			User user = session.getOperator();
			if (include(user.getRoles(), m_AllowRoles)) {
				return true;
			} else {
				return false;
			}
		}
		if (null != m_DisallowRoles) {
			User user = session.getOperator();
			if (include(user.getRoles(), m_DisallowRoles)) {
				return false;
			} else {
				return true;
			}
		}
		return true;
	}

	private static boolean include(List<Role> roles, List<Integer> roleids) {
		for (Role role : roles) {
			for (int id : roleids) {
				if (role.getId() == id) {
					return true;
				}
			}
		}
		return false;
	}

}
