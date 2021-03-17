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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;

import cn.weforward.common.util.ClassUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.common.util.TransList;
import cn.weforward.framework.ApiException;
import cn.weforward.framework.ApiMethod;
import cn.weforward.framework.InnerApiMethod;
import cn.weforward.framework.doc.DocMethods;
import cn.weforward.framework.doc.DocObjectProvider;
import cn.weforward.framework.support.AbstractApiMethod;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.StatusCode;
import cn.weforward.protocol.datatype.DataType;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.datatype.DtString;
import cn.weforward.protocol.doc.DocAttribute;
import cn.weforward.protocol.doc.DocObject;
import cn.weforward.protocol.doc.DocSpecialWord;
import cn.weforward.protocol.doc.annotation.DocMethod;
import cn.weforward.protocol.doc.annotation.DocParameter;
import cn.weforward.protocol.doc.annotation.DocReturn;
import cn.weforward.protocol.doc.annotation.DocService;
import cn.weforward.protocol.ext.ObjectMapper;
import cn.weforward.protocol.support.NamingConverter;
import cn.weforward.protocol.support.datatype.FriendlyObject;
import cn.weforward.protocol.support.doc.DocAttributeVo;
import cn.weforward.protocol.support.doc.DocMethodVo;
import cn.weforward.protocol.support.doc.DocModifyVo;
import cn.weforward.protocol.support.doc.DocObjectVo;
import cn.weforward.protocol.support.doc.DocSpecialWordVo;
import cn.weforward.protocol.support.doc.DocStatusCodeVo;
import cn.weforward.protocol.support.doc.ServiceDocumentVo;

/**
 * 文档方法
 * 
 * @author daibo
 *
 */
public class DocumentMethod extends AbstractApiMethod implements InnerApiMethod {
	/** 日志 */
	private static Logger _Logger = LoggerFactory.getLogger(DocumentMethod.class);
	/** 服务 */
	protected WeforwardService m_Service;
	/** 文档内容 */
	protected ServiceDocumentVo m_Info;

	private static ConcurrentHashMap<GenericClass, DocObjectVo> OBJECTMAP = new ConcurrentHashMap<>();

	private static Comparator<DocStatusCodeVo> _STATUS_BY_CODE = new Comparator<DocStatusCodeVo>() {

		@Override
		public int compare(DocStatusCodeVo o1, DocStatusCodeVo o2) {
			return o1.code - o2.code;
		}
	};

	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public DocumentMethod(WeforwardService service) {
		this(service, "");
	}

	public DocumentMethod(WeforwardService service, String basepath) {
		super(StringUtil.toString(basepath) + "_doc");
		service.registerMethod(this);
		m_Service = service;
	}

	@Override
	public DtBase handle(String path, DtObject params, Request request, Response response) throws ApiException {
		DtString op = params.getString("op");
		if (null != op && StringUtil.eq("clear", op.toString())) {
			m_Info = null;
		}
		return ServiceDocumentVo.MAPPER.toDtObject(getServiceDocument());

	}

	public ServiceDocumentVo getServiceDocument() {
		if (null == m_Info) {
			synchronized (this) {
				m_Info = init();
			}
		}
		return m_Info;
	}

	private ServiceDocumentVo init() {
		ServiceDocumentVo info = new ServiceDocumentVo();
		info.name = m_Service.getName();
		info.version = m_Service.getVersion();
		info.description = m_Service.getDescription();
		String modifiespath = m_Service.getModifyPath();
		// 通用对象
		List<Class<?>> objects = new ArrayList<>();
		for (Class<?> clazz : m_Service.getObjectClasses()) {
			objects.add(clazz);
		}
		for (String packageName : m_Service.getObjectPackages()) {
			for (Class<?> clazz : ClassUtil.getClasses(packageName)) {
				objects.add(clazz);
			}
		}

		DocService doc = ClassUtils.getUserClass(m_Service).getAnnotation(DocService.class);
		if (null != doc) {
			info.name = StringUtil.isEmpty(doc.name()) ? info.name : doc.name();
			info.version = StringUtil.isEmpty(doc.version()) ? info.version : doc.version();
			info.description = StringUtil.isEmpty(doc.description()) ? info.description : doc.description();
			modifiespath = StringUtil.isEmpty(doc.modifyPath()) ? modifiespath : doc.modifyPath();
			for (Class<?> clazz : doc.objects()) {
				objects.add(clazz);
			}
			for (String packageName : doc.objectPackage()) {
				for (Class<?> clazz : ClassUtil.getClasses(packageName)) {
					objects.add(clazz);
				}
			}
		}
		if (StringUtil.isEmpty(modifiespath)) {
			modifiespath = info.name + ".modify.csv";
		}
		info.modifies = toModifies(modifiespath);

		List<DocObjectVo> commons = new ArrayList<>();
		for (Class<?> clazz : objects) {
			commons.add(toObject(commons, clazz, null));
		}
		for (DocObject o : m_Service.getObjects()) {
			commons.add(DocObjectVo.valueOf(o));
		}
		for (DocObjectProvider p : m_Service.getObjectProviders()) {
			commons.add(DocObjectVo.valueOf(p.get()));
		}
		List<DocMethodVo> methods = new ArrayList<>();
		for (Map.Entry<String, ApiMethod> e : m_Service.getMethods().entrySet()) {
			String name = e.getKey();
			ApiMethod method = e.getValue();
			DocMethodVo vo = toMethod(commons, name, method);
			if (null == vo) {
				continue;
			}
			methods.add(vo);
		}
		Collections.sort(commons);
		info.objects = commons;

		Collections.sort(methods);
		info.methods = methods;

		List<DocStatusCodeVo> codes = new ArrayList<>();
		if (null != doc) {
			codes.addAll(toStatusCodes(doc.statusCodeClass()));
		}
		for (Class<?> clazz : m_Service.getStatusCodeClasses()) {
			codes.addAll(toStatusCodes(clazz));
		}
		Collections.sort(codes, _STATUS_BY_CODE);
		info.statusCodes = codes;

		info.specialWords = new TransList<DocSpecialWordVo, DocSpecialWord>(m_Service.getDocSpecialWords()) {

			@Override
			protected DocSpecialWordVo trans(DocSpecialWord src) {
				return new DocSpecialWordVo(src);
			}
		};
		return info;
	}

	/* 转换修改说明 */
	private List<DocModifyVo> toModifies(String path) {
		ClassPathResource res = new ClassPathResource(path);
		if (!res.exists()) {
			return Collections.emptyList();
		}
		List<DocModifyVo> list = new ArrayList<>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(res.getInputStream()));
			String line;
			while (null != (line = reader.readLine())) {
				String arr[] = line.split(",");
				DocModifyVo vo = new DocModifyVo();
				vo.author = arr[0];
				synchronized (FORMAT) {
					vo.date = FORMAT.parse(arr[1]);
				}
				vo.content = arr[2];
				list.add(vo);
			}
		} catch (Exception e) {
			_Logger.warn("忽略读取异常", e);
		} finally {
			if (null != reader) {
				try {
					reader.close();
				} catch (IOException e) {
					_Logger.warn("忽略关闭失败", e);
				}
			}
		}
		return list;
	}

	/* 转换方法 */
	private DocMethodVo toMethod(List<DocObjectVo> commons, String name, ApiMethod method) {
		if (method instanceof InnerApiMethod) {
			return null;// 忽略内部方法
		}
		DocMethodVo vo = new DocMethodVo();
		DocMethod docmethod;
		DocParameter docparameter;
		DocReturn docreturn;
		Class<?> parameterClass = null;
		Map<String, Type> parameterGenericMap = new HashMap<>();
		Class<?> returnClass;
		Map<String, Type> returnGenericMap = new HashMap<>();
		int index = 0;
		if (method instanceof ReflectMethod) {
			Method m = ((ReflectMethod) method).getMethod();
			Class<?> clazz = ((ReflectMethod) method).getMethods();
			DocMethods docMethods = clazz.getAnnotation(DocMethods.class);
			if (null != docMethods) {
				index = docMethods.index();
			}
			docmethod = m.getAnnotation(DocMethod.class);
			docparameter = m.getAnnotation(DocParameter.class);
			docreturn = m.getAnnotation(DocReturn.class);
			Class<?>[] classes = m.getParameterTypes();
			Type[] typeclasses = m.getGenericParameterTypes();
			if (null != classes) {
				for (int i = 0; i < classes.length; i++) {
					Class<?> loopclazz = classes[i];
					if (isBaseType(loopclazz)) {
						continue;
					}
					parameterClass = loopclazz;
					parameterGenericMap = getGenericMap(parameterGenericMap, parameterClass, typeclasses[i]);
					break;
				}
			}
			returnClass = m.getReturnType();
			returnGenericMap = getGenericMap(returnGenericMap, returnClass, m.getGenericReturnType());
		} else {
			Method m;
			try {
				m = method.getClass().getMethod("handle", String.class, DtObject.class, Request.class, Response.class);
			} catch (NoSuchMethodException | SecurityException e) {
				_Logger.warn("忽略获取方法异常", e);
				return null;
			}
			docmethod = m.getAnnotation(DocMethod.class);
			docparameter = m.getAnnotation(DocParameter.class);
			docreturn = m.getAnnotation(DocReturn.class);
			parameterClass = null;
			returnClass = null;
		}
		if (null == docmethod) {
			return null;
		}
		vo.name = convention(StringUtil.isEmpty(docmethod.name()) ? name : docmethod.name());
		vo.title = docmethod.title();
		vo.description = docmethod.description();
		vo.index = index + docmethod.index();
		if (null != docparameter) {
			List<DocAttributeVo> list = new ArrayList<>();
			for (cn.weforward.protocol.doc.annotation.DocAttribute a : docparameter.value()) {
				list.add(toAttribute(commons, a, null));
			}
			Collections.sort(list);
			vo.params = list;
		} else {
			DocObjectVo detail = getDetail(commons, parameterClass, parameterGenericMap);
			if (null != detail) {
				vo.params = detail.attributes;
			} else {
				vo.params = Collections.emptyList();
			}
		}
		DocAttributeVo returnvo = new DocAttributeVo();
		if (null != docreturn) {
			if (Void.class != docreturn.type()) {
				returnClass = docreturn.type();
			}
			if (Void.class != docreturn.component()) {
				// Iterable<E>,Collection<E>,List<E>,ResultPage<E>
				returnGenericMap.put("E", docreturn.component());
			}
			returnvo.description = docreturn.description();
			if (docreturn.necessary()) {
				returnvo.marks |= DocAttribute.MARK_NECESSARY;
			}
			returnvo.example = docreturn.example();
		}
		returnvo.type = getType(returnClass);
		if (returnClass.isArray()) {
			returnvo.component = getType(returnClass.getComponentType());
		} else if (Iterable.class.isAssignableFrom(returnClass)) {
			returnvo.component = getType(returnGenericMap.get("E"));
		}
		if (null != docreturn && docreturn.value().length > 0) {
			DocObjectVo detail = new DocObjectVo();
			detail.description = returnvo.description;
			List<DocAttributeVo> attrs = new ArrayList<>();
			for (cn.weforward.protocol.doc.annotation.DocAttribute a : docreturn.value()) {
				attrs.add(toAttribute(commons, a, null));
			}
			Collections.sort(attrs);
			detail.attributes = attrs;
			returnvo.detail = detail;
		} else {
			DocObjectVo detail = getDetail(commons, returnClass, returnGenericMap);
			if (StringUtil.isEmpty(returnvo.description) && null != detail) {
				returnvo.description = detail.description;
			}
			returnvo.detail = detail;
		}
		vo.returns = Arrays.asList(returnvo);
		return vo;
	}

	/* 转换状态码 */
	@SuppressWarnings("unchecked")
	private List<DocStatusCodeVo> toStatusCodes(Class<?> statusCodeClass) {
		if (null == statusCodeClass) {
			return Collections.emptyList();
		}
		Method getCodes;
		try {
			getCodes = statusCodeClass.getMethod("getCodes");
		} catch (NoSuchMethodException | SecurityException e) {
			_Logger.warn("获取方法出错", e);
			return Collections.emptyList();
		}
		List<StatusCode> returnVal;
		try {
			returnVal = (List<StatusCode>) getCodes.invoke(null);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			_Logger.warn("获取方法出错", e);
			return Collections.emptyList();
		}
		List<DocStatusCodeVo> list = new ArrayList<>();
		for (StatusCode code : returnVal) {
			if (isInnerCode(code)) {
				continue;
			}
			DocStatusCodeVo vo = new DocStatusCodeVo();
			vo.code = code.code;
			vo.message = code.msg;
			list.add(vo);
		}
		return list;
	}

	private static DocObjectVo getDetail(List<DocObjectVo> commons, Class<?> type, Map<String, Type> genericMap) {
		if (null == type) {
			return null;
		}
		if (isBaseType(type)) {
			return null;// 基础类型
		}
		Type component = null;
		if (type.isArray()) {
			component = type.getComponentType();
		}
		if (Iterable.class.isAssignableFrom(type) && null != genericMap) {
			component = genericMap.get("E");
		}
		if (null != component) {
			if (isBaseType(component)) {
				return null;
			}
			String componentType = getType(component);
			for (DocObjectVo vo : commons) {
				if (StringUtil.eq(vo.name, componentType)) {
					return null;// 通用对象
				}
			}
			if (component instanceof Class<?>) {
				return toObject(commons, (Class<?>) component, genericMap);
			} else {
				return null;
			}
		}
		String typeName = getType(type);
		for (DocObjectVo vo : commons) {
			if (StringUtil.eq(vo.name, typeName)) {
				return null;// 通用对象
			}
		}
		return toObject(commons, type, genericMap);
	}

	/* 转换对象 */
	public static DocObjectVo toObject(List<DocObjectVo> commons, Class<?> clazz, Map<String, Type> genericMap) {
		GenericClass key = new GenericClass(clazz, genericMap);
		DocObjectVo vo = OBJECTMAP.get(key);
		if (null != vo) {
			return vo;
		}
		vo = new DocObjectVo();
		DocObjectVo old = OBJECTMAP.putIfAbsent(key, vo);
		if (null != old) {
			return old;
		}
		List<DocObjectVo> history = new ArrayList<>();
		history.addAll(commons);
		history.add(vo);
		cn.weforward.protocol.doc.annotation.DocObject doc = clazz
				.getAnnotation(cn.weforward.protocol.doc.annotation.DocObject.class);
		if (null == doc) {
			vo.name = clazz.getSimpleName();
		} else {
			vo.name = StringUtil.isEmpty(doc.name()) ? clazz.getSimpleName() : doc.name();
			vo.description = doc.description();
			vo.index = doc.index();
		}
		List<DocAttributeVo> vos = getAttributes(history, clazz, genericMap);
		Collections.sort(vos);
		vo.attributes = vos;
		return vo;
	}

	/* 获取属性 */
	private static List<DocAttributeVo> getAttributes(List<DocObjectVo> commons, Class<?> clazz,
			Map<String, Type> genericMap) {
		List<DocAttributeVo> vos = new ArrayList<>();
		Class<?> loop = clazz;
		while (null != loop && loop != Object.class) {
			for (Field f : loop.getDeclaredFields()) {
				cn.weforward.protocol.doc.annotation.DocAttribute attr = f
						.getAnnotation(cn.weforward.protocol.doc.annotation.DocAttribute.class);
				if (null == attr) {
					continue;
				}
				DocAttributeVo vo = toAttribute(commons, attr, genericMap);
				if (StringUtil.isEmpty(vo.name)) {
					String name = f.getName();
					if (name.startsWith("m_")) {
						name = Character.toLowerCase(name.charAt(2)) + name.substring(3);
					}
					vo.name = convention(name);
				}
				if (StringUtil.isEmpty(vo.type)) {
					Class<?> returnType = f.getType();
					Type genericReturnType = f.getGenericType();
					Map<String, Type> myGenericMap = getGenericMap(genericMap, returnType, genericReturnType);
					if (returnType != genericReturnType) {
						String typeName = genericReturnType.getTypeName();
						Type type = myGenericMap.get(typeName);
						if (type instanceof Class<?>) {
							returnType = (Class<?>) type;
						} else if (type instanceof ParameterizedType) {
							Type raw = ((ParameterizedType) type).getRawType();
							if (raw instanceof Class<?>) {
								returnType = (Class<?>) raw;
								myGenericMap = getGenericMap(myGenericMap, returnType, type);
							}
						}
					}
					vo.type = getType(returnType);
					if (returnType.isArray()) {
						vo.component = getType(returnType.getComponentType());
					} else if (Iterable.class.isAssignableFrom(returnType)) {
						vo.component = getType(myGenericMap.get("E"));
					}
					vo.detail = getDetail(commons, returnType, myGenericMap);
				}
				vos.add(vo);
			}
			loop = loop.getSuperclass();
		}
		if (null != clazz) {
			for (Method m : clazz.getMethods()) {
				cn.weforward.protocol.doc.annotation.DocAttribute attr = m
						.getAnnotation(cn.weforward.protocol.doc.annotation.DocAttribute.class);
				if (null == attr) {
					continue;
				}
				DocAttributeVo vo = toAttribute(commons, attr, genericMap);
				if (StringUtil.isEmpty(vo.name)) {
					String name = m.getName();
					if ((name.startsWith("get") && name.length() > 3)
							|| (name.startsWith("set") && name.length() > 3)) {
						name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
					} else if (name.startsWith("is") && name.length() > 2) {
						name = Character.toLowerCase(name.charAt(2)) + name.substring(3);
					}
					vo.name = convention(name);
				}
				if (StringUtil.isEmpty(vo.type)) {
					Class<?> returnType = m.getReturnType();
					Type genericReturnType = m.getGenericReturnType();
					Map<String, Type> myGenericMap = getGenericMap(genericMap, returnType, genericReturnType);
					if (returnType != genericReturnType) {
						String typeName = genericReturnType.getTypeName();
						Type type = myGenericMap.get(typeName);
						if (type instanceof Class<?>) {
							returnType = (Class<?>) type;
						} else if (type instanceof ParameterizedType) {
							Type raw = ((ParameterizedType) type).getRawType();
							if (raw instanceof Class<?>) {
								returnType = (Class<?>) raw;
								myGenericMap = getGenericMap(myGenericMap, returnType, type);
							}
						}
					}
					vo.type = getType(returnType);
					if (returnType.isArray()) {
						vo.component = getType(returnType.getComponentType());
					} else if (Iterable.class.isAssignableFrom(returnType)) {
						vo.component = getType(myGenericMap.get("E"));
					}
					vo.detail = getDetail(commons, returnType, myGenericMap);
				}
				vos.add(vo);
			}
		}
		return vos;
	}

	/* 转换属性 */
	private static DocAttributeVo toAttribute(List<DocObjectVo> commons,
			cn.weforward.protocol.doc.annotation.DocAttribute a, Map<String, Type> genericMap) {
		DocAttributeVo avo = new DocAttributeVo();
		avo.name = convention(a.name());
		avo.type = getType(a.type());
		avo.index = a.index();
		avo.component = getType(a.component());
		avo.description = a.description();
		avo.example = a.example();
		if (a.necessary()) {
			avo.marks |= DocAttribute.MARK_NECESSARY;
		}
		Class<?> component = a.component();
		if (null != component && Void.class != component) {
			if (null == genericMap) {
				genericMap = new HashMap<String, Type>();
			}
			genericMap.put("E", a.component());
		}
		avo.detail = getDetail(commons, a.type(), genericMap);
		return avo;
	}

	// 基础类型
	private static boolean isBaseType(Type clazzType) {
		Class<?> type = null;
		if (clazzType instanceof Class<?>) {
			type = (Class<?>) clazzType;
		} else {
			return false;
		}
		return type.isPrimitive() || String.class.isAssignableFrom(type) || Number.class.isAssignableFrom(type)
				|| CharSequence.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)
				|| Date.class.isAssignableFrom(type) || DtBase.class.isAssignableFrom(type)
				|| FriendlyObject.class.isAssignableFrom(type) || ObjectMapper.class.isAssignableFrom(type);
	}

	/* 获取类型 */
	private static String getType(Type clazzType) {
		Class<?> type = null;
		if (clazzType instanceof Class<?>) {
			type = (Class<?>) clazzType;
		} else if (clazzType instanceof ParameterizedType) {
			return getType(((ParameterizedType) clazzType).getRawType());
		}
		if (type == Void.class || type == null) {
			return null;
		}
		if (BigInteger.class.isAssignableFrom(type) || BigDecimal.class.isAssignableFrom(type)) {
			return DataType.STRING.value;
		}
		if (Number.class.isAssignableFrom(type) || type == short.class || type == int.class || type == float.class
				|| type == double.class || type == long.class || type == byte.class) {
			return DataType.NUMBER.value;
		}
		if (CharSequence.class.isAssignableFrom(type)) {
			return DataType.STRING.value;
		}
		if (Boolean.class.isAssignableFrom(type) || type == boolean.class) {
			return DataType.BOOLEAN.value;
		}
		if (Date.class.isAssignableFrom(type)) {
			return DataType.DATE.value;
		}
		if (type.isArray()) {
			return DataType.LIST.value;
		}
		if (Collection.class.isAssignableFrom(type)) {
			return DataType.LIST.value;
		}
		cn.weforward.protocol.doc.annotation.DocObject v = type
				.getAnnotation(cn.weforward.protocol.doc.annotation.DocObject.class);
		if (null != v && !StringUtil.isEmpty(v.name())) {
			return v.name();
		}
		return type.getSimpleName();
	}

	/* 是否为内部码 */
	private static boolean isInnerCode(StatusCode code) {
		return code.code < 100000;
	}

	/*
	 * 转换约定名称
	 */
	private static String convention(String name) {
		return NamingConverter.camelToWf(name);
	}

	/*
	 * 获取泛型类
	 */
	private static Map<String, Type> getGenericMap(Map<String, Type> genericMap, Class<?> clazz, Type classType) {
		HashMap<String, Type> myGenericMap = new HashMap<>();
		if (null != genericMap) {
			myGenericMap.putAll(genericMap);
		}
		if (classType instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) classType;
			TypeVariable<?>[] typeParams = clazz.getTypeParameters();
			Type[] types = ptype.getActualTypeArguments();
			if (null != types && null != typeParams && types.length == typeParams.length) {
				for (int i = 0; i < types.length; i++) {
					Type type = types[i];
					myGenericMap.put(typeParams[i].getName(), type);
				}
			}
		}
		return myGenericMap;
	}

	private static class GenericClass {
		private Class<?> m_Clazz;
		private Map<String, Type> m_GenericMap;

		public GenericClass(Class<?> clazz, Map<String, Type> genericMap) {
			m_Clazz = clazz;
			m_GenericMap = genericMap;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof GenericClass) {
				GenericClass c = (GenericClass) obj;
				if (!m_Clazz.equals(c.getClass())) {
					return false;
				}
				if (null == m_GenericMap) {
					return null == c.m_GenericMap;
				}
				if (null == c.m_GenericMap) {
					return false;
				}
				return m_GenericMap.equals(c.m_GenericMap);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return m_Clazz.hashCode();
		}
	}
}
