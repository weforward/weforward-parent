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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.util.AntPathPattern;
import cn.weforward.common.util.StringUtil;
import cn.weforward.framework.ApiException;
import cn.weforward.framework.ApiMethod;
import cn.weforward.framework.Authorizer;
import cn.weforward.framework.WeforwardSession;
import cn.weforward.framework.exception.ApiBusinessException;
import cn.weforward.framework.exception.ForwardException;
import cn.weforward.framework.support.AbstractWeforwardEndPoint;
import cn.weforward.framework.util.RequestUtil;
import cn.weforward.metrics.WeforwardMetrics;
import cn.weforward.protocol.Access;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.exception.WeforwardException;
import cn.weforward.protocol.ops.User;
import cn.weforward.protocol.ops.UserService;
import cn.weforward.protocol.ops.trace.SimpleServiceTraceToken;
import cn.weforward.protocol.support.datatype.SimpleDtNumber;
import cn.weforward.protocol.support.datatype.SimpleDtObject;
import cn.weforward.protocol.support.datatype.SimpleDtString;
import cn.weforward.protocol.support.doc.ServiceDocumentVo;
import cn.weforward.trace.ServiceTrace;
import cn.weforward.trace.TraceRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * RPC端点
 * 
 * @author daibo
 *
 */
public class RpcEndPoint extends AbstractWeforwardEndPoint {
	/** 日志记录器 */
	private final static Logger _Logger = LoggerFactory.getLogger(RpcEndPoint.class);
	/** 调整方法集 */
	private HashMap<String, ApiMethod> m_Methods;
	/** Ant表达式的Url集合 */
	private List<String> m_AntUrls = Collections.emptyList();
	/** 项目基本链接 */
	private String m_BasePath = "";
	/** 后缀 */
	private String m_Suffix = "";
	/** 验证器 */
	private Map<String, Authorizer> m_Authorizers;
	/** 文档方法名 */
	private String m_DocumentMethodName;
	/** 文档方法 */
	private DocumentMethod m_DocumentMethod;
	/** 调试方法名 */
	private String m_DebugMethod;
	/** 当前并发数 */
	private AtomicInteger m_CurrentRequest;
	/** 监控指标 */
	private MeterRegistry m_MeterRegistry;
	/** 监控追踪 */
	private TraceRegistry m_TraceRegistry;
	/** 用户服务 */
	private UserService m_UserService;

	/**
	 * 构造
	 */
	public RpcEndPoint() {
		m_Methods = new HashMap<>(10);
		m_Authorizers = new HashMap<>();
		m_CurrentRequest = new AtomicInteger();
	}

	public String getBasePath() {
		return m_BasePath;
	}

	public void setMeterRegistry(MeterRegistry registry) {
		m_MeterRegistry = registry;
		if (null != m_MeterRegistry) {
			Gauge.builder(WeforwardMetrics.RPC_CURRENT_REQUEST_KEY, m_CurrentRequest, AtomicInteger::doubleValue)
					.strongReference(true).register(m_MeterRegistry);
		}
	}

	public MeterRegistry getMeterRegistry() {
		return m_MeterRegistry;
	}

	public void setTraceRegister(TraceRegistry registry) {
		m_TraceRegistry = registry;
	}

	public TraceRegistry getTraceRegistry() {
		return m_TraceRegistry;
	}

	// @Override
	protected Logger getLogger() {
		return _Logger;
	}

	/***
	 * 注册验证器
	 * 
	 * @param kind       验证器的类型
	 * @param authorizer 验证器
	 */
	public void register(String kind, Authorizer authorizer) {
		m_Authorizers.put(StringUtil.toString(kind), authorizer);
	}

	/**
	 * 项目基本路径
	 * 
	 * @param path 路径
	 */
	public void setBasePath(String path) {
		m_BasePath = StringUtil.toString(path);
	}

	/**
	 * 后缀
	 * 
	 * @param suffix 后缀
	 */
	public void setSuffix(String suffix) {
		m_Suffix = StringUtil.toString(suffix);
	}

	protected DtBase handle(Request request, Response response) throws IOException, WeforwardException, ApiException {
		long start = System.currentTimeMillis();
		int code = 0;
		MeterRegistry mr = getMeterRegistry();
		TraceRegistry tr = getTraceRegistry();
		String traceToken = request.getTraceToken();
		Tags tags = Tags.empty();
		try {
			m_CurrentRequest.incrementAndGet();
			String path = RequestUtil.getMethod(request);
			if (null != mr) {
				tags = WeforwardMetrics.TagHelper.of(WeforwardMetrics.TagHelper.method(path));
			}
			Access access = request.getAccess();
			String kind = null;
			if (null != access) {
				kind = Access.Helper.getKind(access.getAccessId());
			}
			SimpleSession session = new SimpleSession(request);
			if (null != m_UserService && StringUtil.eq(Access.KIND_USER, kind)) {
				User user = m_UserService.getUserByAccess(request.getAccess().getAccessId());
				session.bindOperator(user);
			}
			WeforwardSession.TLS.putSession(session);
			Authorizer a = m_Authorizers.get(StringUtil.toString(kind));
			if (null != a) {
				a.auth(request);
			}
			return doHandle(kind, path, session, request, response);
		} catch (ApiException e) {
			code = e.getCode();
			throw e;
		} catch (ForwardException e) {
			code = 0;// 转换算成功吧
			throw e;
		} catch (RuntimeException e) {
			code = ApiException.CODE_INTERNAL_ERROR;
			throw e;
		} finally {
			long end = System.currentTimeMillis();
			long amount = end - start;
			if (null != mr) {
				tags = tags.and(WeforwardMetrics.TagHelper.code(code));
				mr.timer(WeforwardMetrics.RPC_REQUEST_KEY, tags).record(amount, TimeUnit.MILLISECONDS);
			}
			if (null != tr && !StringUtil.isEmpty(traceToken)) {
				SimpleServiceTraceToken token = SimpleServiceTraceToken.valueOf(traceToken);
				tr.register(ServiceTrace.newTrace(token.getSpanId(), token.getParentId(), token.getTraceId(), start,
						amount, tags));
			}
			m_CurrentRequest.decrementAndGet();
			WeforwardSession.TLS.putSession(null);
		}
	}

	/* 注册方法 */
	protected void register(ApiMethod method) {
		if (null == method) {
			return;
		}
		String key = m_BasePath + method.getName() + m_Suffix;
		if (isAntUrl(key)) {
			List<String> old = m_AntUrls;
			List<String> list;
			if (null == old) {
				list = Collections.singletonList(key);
			} else {
				list = new ArrayList<>(old.size() + 1);
				list.addAll(old);
				list.add(key);
			}
			m_AntUrls = list;
		}
		if (method instanceof DocumentMethod) {
			m_DocumentMethodName = key;
			m_DocumentMethod = (DocumentMethod) method;
		}
		if (method instanceof DebugMethod) {
			m_DebugMethod = key;
		}
		if (_Logger.isDebugEnabled()) {
			_Logger.debug("reigster method " + key);
		}
		ApiMethod old = m_Methods.put(key, method);
		if (null != old) {
			_Logger.error("存在同名的方法:" + key + "，" + old + "被替换");
		}
	}

	Map<String, ApiMethod> getMethods() {
		return m_Methods;
	}

	String getDocumentMethod() {
		return m_DocumentMethodName;
	}

	ServiceDocumentVo getServiceDocument() {
		return null == m_DocumentMethod ? null : m_DocumentMethod.getServiceDocument();
	}

	String getDebugMethod() {
		return m_DebugMethod;
	}

	private DtBase doHandle(String kind, String path, WeforwardSession session, Request request, Response response)
			throws ApiException {
		ApiMethod method = m_Methods.get(path);
		if (null == method && !m_AntUrls.isEmpty()) {
			for (String ant : m_AntUrls) {
				if (AntPathPattern.match(ant, path)) {
					method = m_Methods.get(ant);
					break;
				}
			}
		}
		if (null == method) {
			throw new ApiException(ApiException.CODE_METHOD_NOT_EXISTS, "[" + path + "(" + kind + ")]方法不存在");
		}
		String mkind = method.getKind();
		if (!StringUtil.isEmpty(mkind) && !StringUtil.eq(mkind, kind)) {
			throw ApiException.METHOD_KIND_NO_MATCH;
		}
		if (!method.isAllow(session)) {
			throw ApiException.AUTH_FAILED;
		}
		DtObject params = RequestUtil.getParams(request);
		return method.handle(path, params, request, response);
	}

	protected static DtObject toResult(ApiException e) {
		SimpleDtObject m = new SimpleDtObject();
		m.put("code", SimpleDtNumber.valueOf(e.getCode()));
		m.put("msg", SimpleDtString.valueOf(e.getMessage()));
		if (e instanceof ApiBusinessException) {
			m.put("business_code", SimpleDtNumber.valueOf(((ApiBusinessException) e).getBusinessCode()));
		}
		return m;
	}

	protected static DtObject toResult(int code, String message, DtBase content) {
		SimpleDtObject m = new SimpleDtObject();
		m.put("code", SimpleDtNumber.valueOf(code));
		m.put("msg", SimpleDtString.valueOf(message));
		if (null != content) {
			m.put("content", content);
		}
		return m;
	}

}
