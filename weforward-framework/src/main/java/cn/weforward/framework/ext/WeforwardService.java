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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import cn.weforward.common.Destroyable;
import cn.weforward.common.io.OutputStreamStay;
import cn.weforward.common.json.JsonOutputStream;
import cn.weforward.common.restful.RestfulRequest;
import cn.weforward.common.restful.RestfulResponse;
import cn.weforward.common.restful.RestfulService;
import cn.weforward.common.sys.Memory;
import cn.weforward.common.sys.Shutdown;
import cn.weforward.common.sys.VmStat;
import cn.weforward.common.util.ClassUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.common.util.ThreadPool;
import cn.weforward.framework.ApiException;
import cn.weforward.framework.ApiMethod;
import cn.weforward.framework.Authorizer;
import cn.weforward.framework.ResourceDownloader;
import cn.weforward.framework.ResourceHandler;
import cn.weforward.framework.ResourceUploader;
import cn.weforward.framework.Topic;
import cn.weforward.framework.TopicHub;
import cn.weforward.framework.TopicListener;
import cn.weforward.framework.doc.DocObjectProvider;
import cn.weforward.framework.exception.ForwardException;
import cn.weforward.framework.util.HostUtil;
import cn.weforward.framework.util.VersionUtil;
import cn.weforward.metrics.RemoteMeterRegistry;
import cn.weforward.protocol.Access;
import cn.weforward.protocol.AccessLoader;
import cn.weforward.protocol.AsyncResponse;
import cn.weforward.protocol.Header;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.aio.http.HttpHeaderHelper;
import cn.weforward.protocol.aio.http.HttpHeaderOutput;
import cn.weforward.protocol.aio.http.RestfulServer;
import cn.weforward.protocol.aio.netty.NettyHttpServer;
import cn.weforward.protocol.client.util.MappedUtil;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.doc.DocObject;
import cn.weforward.protocol.doc.DocSpecialWord;
import cn.weforward.protocol.exception.AuthException;
import cn.weforward.protocol.exception.SerialException;
import cn.weforward.protocol.exception.WeforwardException;
import cn.weforward.protocol.ext.Producer;
import cn.weforward.protocol.ext.ServiceRuntime;
import cn.weforward.protocol.gateway.ServiceRegister;
import cn.weforward.protocol.gateway.http.HttpServiceRegister;
import cn.weforward.protocol.gateway.vo.ServiceVo;
import cn.weforward.protocol.ops.trace.ServiceTraceToken;
import cn.weforward.protocol.serial.JsonSerialEngine;
import cn.weforward.protocol.support.SimpleProducer;
import cn.weforward.protocol.support.SimpleResponse;
import cn.weforward.protocol.support.datatype.FriendlyObject;
import cn.weforward.protocol.support.datatype.SimpleDtList;
import cn.weforward.protocol.support.datatype.SimpleDtObject;
import cn.weforward.protocol.support.doc.ServiceDocumentVo;
import cn.weforward.trace.RemoteTraceRegistry;
import cn.weforward.trace.TraceRegistry;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 微服务
 * 
 * @author daibo
 *
 */
public class WeforwardService
		implements TopicHub, AccessLoader, RestfulService, Destroyable, ApplicationContextAware, BeanPostProcessor {
	/** 日志 */
	protected static final Logger _Logger = LoggerFactory.getLogger(WeforwardService.class);
	/** 用于心跳的定时器 */
	protected final static Timer _Timer = new Timer("WeforwardService-Timer", true);

	/** Access加载器 */
	protected AccessLoader m_AccessLoader;
	/** 访问id */
	protected String m_AccessId;
	/** 访问key */
	protected String m_AccessKey;
	/** 主机域名 */
	protected String m_Host;
	/** 编号 */
	protected String m_No;
	/** 版本号 */
	protected String m_Version;
	/** 兼容版本号 */
	protected String m_CompatibleVersion;
	/** 实现版本号 */
	protected String m_ImplementationVersion;
	/** 运行实例id */
	protected String m_RunningId = System.getenv("RUNNING_ID");
	/** 服务链接 */
	protected String m_ServicesUrl;
	/** 心跳间隔 */
	protected int m_HeartbeatPeriod;
	/** 服务登记 */
	protected ServiceRegister m_ServiceRecorder;
	/** 方法注册器 */
	protected MethodsRegister m_MethodsRegister;
	/** RPC端点 */
	protected RpcEndPoint m_RpcEndpoint;
	/** 流数据端点 */
	protected StreamEndPoint m_StreamEndpoint;
	/** 服务描述 */
	protected String m_Description = "";
	/** 服务修改文件路径 */
	protected String m_ModifyPath = "";
	/** 服务对象类 */
	protected List<Class<?>> m_ObjectClasses;
	/** 服务对象类包路径 */
	protected List<String> m_ObjectPackages;
	/** 服务状态码类 */
	protected List<Class<?>> m_StatusCodeClasses = new ArrayList<>();
	/** 服务文件对象类 */
	protected List<DocObject> m_Objects = new ArrayList<>();
	/** 服务文件对象类供应商 */
	protected List<DocObjectProvider> m_ObjectProviders = new ArrayList<>();
	/** 微服务文档的特殊名词 */
	protected List<DocSpecialWord> m_DocSpecialWords = new ArrayList<>();
	/** 数据制作器 */
	protected Producer m_Producer;
	/** 业务线程池 */
	protected Executor m_Executor;
	/** 心跳定时任务 */
	protected TimerTask m_HbTask;
	/** 设置记录超过最大消耗时间的请求 */
	protected int m_ElapseTime;
	/** 请求内容的最大字节数 */
	protected int m_RequestMaxSize;
	/** 是否启用转换发 */
	protected boolean m_ForwardEnable;
	/** 是否显示文档 */
	protected boolean m_ShowDocEnable;
	/** 启动时间 */
	protected long m_StartTime;
	/** HTTP server */
	protected NettyHttpServer m_HttpServer;
	/** Restful包装 */
	protected RestfulServer m_RestfulServer;
	/** 监听器 */
	protected Map<String, List<TopicListenerWrap<?>>> m_Listeners = new HashMap<>();

	protected List<ApplicationContextAware> m_ChildAwares = new ArrayList<>();

	public WeforwardService(String name, String host, int port) throws Exception {
		this(name, host, port, null);
	}

	public WeforwardService(String name, String host, int port, String path) throws Exception {
		this(name, host, port, path, 50);
	}

	/**
	 * 构建
	 * 
	 * @param name    服务名
	 * @param host    主机域名
	 * @param port    端口
	 * @param path    项目基本路径
	 * @param threads 业务处理线程数
	 * @throws Exception 异常
	 */
	public WeforwardService(String name, String host, int port, String path, int threads) throws Exception {
		m_HttpServer = new NettyHttpServer(port);
		m_HttpServer.setName(name);
		m_HttpServer.setGzipEnabled(true);
		m_StartTime = System.currentTimeMillis();
		m_Host = genHost(host);
		m_Producer = new SimpleProducer(this);
		m_RpcEndpoint = new RpcEndPoint();
		m_RpcEndpoint.setBasePath(path);
		m_StreamEndpoint = new StreamEndPoint();
		m_RestfulServer = new RestfulServer(this);
		m_HttpServer.setHandlerFactory(m_RestfulServer);
		if (threads > 0) {
			setExecutor(new ThreadPool(threads, name));
		}
		setElapseTime(10000);
		onInit();
		m_HttpServer.start();
		setHeartbeatPeriod(60);
		Shutdown.register(this);
	}

	/* 初始方法 */
	protected void onInit() {
	}

	public void setExecutor(Executor executor) {
		m_Executor = executor;
		m_RestfulServer.setExecutor(executor);
	}

	/**
	 * 设置指标监控链接
	 * 
	 * @param url 链接
	 * @throws MalformedURLException 链接异常
	 */
	public void setMeterRegistryUrl(String url) throws MalformedURLException {
		RemoteMeterRegistry registry = new RemoteMeterRegistry(url);
		registry.setServiceName(getName());
		registry.setServiceNo(getNo());
		setMeterRegistry(registry);
	}

	/**
	 * 设置指标监控
	 * 
	 * @param registry 注册表
	 */
	public void setMeterRegistry(MeterRegistry registry) {
		MeterRegistry old = m_RpcEndpoint.getMeterRegistry();
		if (null != old) {
			old.close();
		}
		m_RpcEndpoint.setMeterRegistry(registry);
		m_StreamEndpoint.setMeterRegistry(registry);
	}

	/**
	 * 设置追踪监控链接
	 * 
	 * @param url 链接
	 * @throws MalformedURLException 链接异常
	 */
	public void setTraceRegisterUrl(String url) throws MalformedURLException {
		RemoteTraceRegistry registry = new RemoteTraceRegistry(url);
		registry.setServiceName(getName());
		registry.setServiceNo(getNo());
		setTraceRegister(registry);
	}

	/**
	 * 设置追踪监控
	 * 
	 * @param registry 注册表
	 */
	public void setTraceRegister(TraceRegistry registry) {
		TraceRegistry old = m_RpcEndpoint.getTraceRegistry();
		if (null != old) {
			old.close();
		}
		m_RpcEndpoint.setTraceRegister(registry);
	}

	/**
	 * 开启更快（调用请求数据未接收完）进入业务处理，这同时需要指定独立的业务线程池才会生效
	 * 
	 * @param enabled 是否开启
	 */
	public void setQuickHandle(boolean enabled) {
		m_RestfulServer.setQuickHandle(enabled);
	}

	/**
	 * 开启调试模式
	 * 
	 * @param enabled 是否开启
	 */
	public void setDebugEnabled(boolean enabled) {
		if (null != m_HttpServer) {
			m_HttpServer.setDebugEnabled(enabled);
		}
	}

	/**
	 * 开启内置方法
	 * 
	 * @param enabled 是否开启
	 */
	public void setInnerMethodEnabled(boolean enabled) {
		if (enabled) {
			aware(new DebugMethod(this));
			aware(new VersionMethod(this, Arrays.asList(getClass().getName())));
			aware(new DocumentMethod(this));
		}
	}

	private void aware(ApiMethod methods) {
		if (methods instanceof ApplicationContextAware) {
			m_ChildAwares.add((ApplicationContextAware) methods);
		}
	}

	/**
	 * 开始方法发现
	 * 
	 * @param enabled
	 */
	public void setMethodsAwareEnabled(boolean enabled) {
		if (enabled) {
			m_MethodsRegister = new MethodsRegister(this);
		} else {
			m_MethodsRegister = null;
		}
	}

	/**
	 * 设置最大的http内容大小
	 * 
	 * @param maxHttpSize http内容大小
	 */
	public void setMaxHttpSize(int maxHttpSize) {
		if (null != m_HttpServer) {
			m_HttpServer.setMaxHttpSize(maxHttpSize);
		}
	}

	/**
	 * 设置记录超过最大消耗时间的请求
	 * 
	 * @param mills 毫秒数
	 */
	public void setElapseTime(int mills) {
		m_ElapseTime = mills;
	}

	/**
	 * 是否启用显示文档
	 * 
	 * @param enable 开启/关闭
	 */
	public void setShowDocEnable(boolean enable) {
		m_ShowDocEnable = enable;
	}

	/**
	 * 是否启用转换发
	 * 
	 * @param enable 开启/关闭
	 */
	public void setForwardEnable(boolean enable) {
		m_ForwardEnable = enable;
	}

	/**
	 * 请求内容的最大字节数
	 * 
	 * @param max 字节数
	 */
	public void setRequestMaxSize(int max) {
		m_RequestMaxSize = max;
	}

	public void setGzipEnabled(boolean enabled) {
		if (null != m_HttpServer) {
			m_HttpServer.setGzipEnabled(enabled);
		}
	}

	public void setGzipMinSize(int minSize) {
		if (null != m_HttpServer) {
			m_HttpServer.setGzipMinSize(minSize);
		}
	}

	public void setIdle(int secs) {
		if (null != m_HttpServer) {
			m_HttpServer.setIdle(secs);
		}
	}

	public String getName() {
		if (null != m_HttpServer) {
			return m_HttpServer.getName();
		}
		return null;
	}

	public int getPort() {
		if (null != m_HttpServer) {
			return m_HttpServer.getPort();
		}
		return 0;
	}

	public String getHost() {
		return m_Host;
	}

	/**
	 * 设置心跳间隔
	 * 
	 * @param seconds 间隔时间（秒）
	 */
	public void setHeartbeatPeriod(int seconds) {
		m_HeartbeatPeriod = seconds;
		if (null != m_HbTask) {
			m_HbTask.cancel();
		}
		if (seconds > 0) {
			// 执行心跳动作
			final Runnable runner = new Runnable() {
				volatile boolean pending = false;

				@Override
				public void run() {
					if (pending) {
						// 上次的心跳未执行完？
						return;
					}
					pending = true;
					try {
						register();
					} finally {
						pending = false;
					}
				}
			};
			m_HbTask = new TimerTask() {
				@Override
				public void run() {
					if (m_HeartbeatPeriod < 1) {
						cancel();
						return;
					}
					Executor executor = m_Executor;
					if (null != executor) {
						// 调度到线程池执行
						try {
							executor.execute(runner);
							return;
						} catch (OutOfMemoryError e) {
							// 内存不够，直接先略过
							return;
						} catch (RuntimeException e) {
							_Logger.warn("忽略执行出错", e);
						}
					}
					// 直接在定时器的线程中执行（会阻塞其它任务）
					runner.run();
				}
			};
			// 1/2心跳一次
			int period = seconds * (1000 / 2);
			int delay = (period > 10 * 1000) ? 10 * 1000 : period;
			_Timer.schedule(m_HbTask, delay, period);
		}
	}

	/**
	 * 设置编号
	 * 
	 * @param no 编号
	 */
	public void setNo(String no) {
		m_No = no;
		if (StringUtil.eq(m_No, "x00ff")) {
			setShowDocEnable(true);// 默认模式下显示
		}
	}

	/**
	 * 获取编号
	 * 
	 * @return 编号
	 */
	public String getNo() {
		return m_No;
	}

	/**
	 * 设置版本
	 * 
	 * @param v 版本
	 */
	public void setVersion(String v) {
		m_Version = v;
	}

	/**
	 * 获取版本
	 * 
	 * @return 版本
	 */
	public String getVersion() {
		if (null != m_Version) {
			return m_Version;
		}
		String v = VersionUtil.getMainVersionByJar(getClass());
		if (StringUtil.isEmpty(v)) {
			v = VersionUtil.getMainVersionByPom();
		}
		if (StringUtil.isEmpty(v)) {
			v = "1.0";
		}
		m_Version = v;
		return m_Version;
	}

	/**
	 * 设置兼容版本
	 * 
	 * @param version 版本
	 */
	public void setCompatibleVersion(String version) {
		m_CompatibleVersion = version;
	}

	/**
	 * 获取兼容版本
	 * 
	 * @return 版本
	 */
	public String getCompatibleVersion() {
		return m_CompatibleVersion;
	}

	/**
	 * 获取实现版本
	 * 
	 * @return 版本
	 */
	public String getImplementationVersion() {
		if (null != m_ImplementationVersion) {
			return m_ImplementationVersion;
		}
		String v = VersionUtil.getImplementationVersionByJar(getClass());
		if (StringUtil.isEmpty(v)) {
			v = VersionUtil.getImplementationVersionByPom();
		}
		if (StringUtil.isEmpty(v)) {
			v = "";
		}
		m_ImplementationVersion = v;
		return m_ImplementationVersion;
	}

	public long getStartTime() {
		return m_StartTime;
	}

	public long getUpTime() {
		return System.currentTimeMillis() - m_StartTime;
	}

	/***
	 * 文档方法
	 * 
	 * @return 方法名
	 */
	private String getDocumentMethod() {
		return m_RpcEndpoint.getDocumentMethod();
	}

	/**
	 * 调试方法
	 * 
	 * @return 方法名
	 */
	private String getDebugMethod() {
		return m_RpcEndpoint.getDebugMethod();
	}

	Map<String, ApiMethod> getMethods() {
		return m_RpcEndpoint.getMethods();
	}

	public static String genHost(String host) {
		if (null == host) {
			return null;
		}
		if (StringUtil.eq(host, "*")) {
			host = HostUtil.getServiceIp(null);
			_Logger.info("自动获取IP:" + host);
		} else if (host.endsWith("*")) {
			host = HostUtil.getServiceIp(host.substring(0, host.length() - 1));
			_Logger.info("自动获取IP:" + host);
		}
		return host;
	}

	/**
	 * 设置描述
	 * 
	 * @param desc 描述
	 */
	public void setDescription(String desc) {
		m_Description = desc;
	}

	/**
	 * 获取描述
	 * 
	 * @return 描述
	 */
	public String getDescription() {
		return m_Description;
	}

	/**
	 * 设置修改路径
	 * 
	 * @param path 修改路径
	 */
	public void setModifyPath(String path) {
		m_ModifyPath = path;
	}

	/**
	 * 获取修改路径
	 * 
	 * @return 修改路径
	 */
	public String getModifyPath() {
		return m_ModifyPath;
	}

	/**
	 * 设置实例id
	 * 
	 * @param id 实例id
	 */
	public void setRunningId(String id) {
		m_RunningId = id;
	}

	/**
	 * 获取实例id
	 * 
	 * @return 实例id
	 */
	public String getRunningId() {
		return m_RunningId;
	}

	/**
	 * 设置对象列表
	 * 
	 * @param classes 对象列表
	 */
	public void setObjectNameList(List<String> classes) {
		List<Class<?>> list = new ArrayList<>(classes.size());
		for (String className : classes) {
			try {
				list.add(Class.forName(className));
			} catch (ClassNotFoundException e) {
				_Logger.warn("忽略加载[" + className + "]类异常", e);
			}
		}
		m_ObjectClasses = list;
	}

	/**
	 * 对象列表
	 * 
	 * @return 对象列表
	 */
	public List<Class<?>> getObjectClasses() {
		if (null == m_ObjectClasses) {
			return Collections.emptyList();
		}
		return m_ObjectClasses;
	}

	/**
	 * 对象所在包路径
	 * 
	 * @param list 对象所在包路径列表
	 */
	public void setObjectPackages(List<String> list) {
		m_ObjectPackages = list;
	}

	/**
	 * 对象所在包路径。如：cn.weforward.user
	 * 
	 * @return 对象所在包路径列表
	 */
	public List<String> getObjectPackages() {
		if (null == m_ObjectPackages) {
			return Collections.emptyList();
		}
		return m_ObjectPackages;
	}

	/**
	 * 状态类
	 * 
	 * @param className 类名
	 */
	public void setStatusCodeClassName(String className) {
		try {
			setStatusCodeClass(Class.forName(className));
		} catch (ClassNotFoundException e) {
			_Logger.warn("忽略加载[" + className + "]类异常", e);
		}
	}

	/**
	 * 状态类
	 * 
	 * @param clazz 类
	 */
	public void setStatusCodeClass(Class<?> clazz) {
		m_StatusCodeClasses = Collections.singletonList(clazz);
	}

	/**
	 * 添加状态类
	 * 
	 * @param clazz 类
	 */
	public void addStatusCodeClass(Class<?> clazz) {
		m_StatusCodeClasses.add(clazz);
	}

	/**
	 * 状态类
	 * 
	 * @return 状态类
	 */
	public List<Class<?>> getStatusCodeClasses() {
		return m_StatusCodeClasses;
	}

	/**
	 * 获取文档对象
	 * 
	 * @return 对象列表
	 */
	public List<DocObject> getObjects() {
		return m_Objects;
	}

	/**
	 * 获取文档对象供应商
	 * 
	 * @return 对象供应商列表
	 */
	public List<DocObjectProvider> getObjectProviders() {
		return m_ObjectProviders;
	}

	/**
	 * 添加特殊名词
	 * 
	 * @param word 特殊名词
	 */
	public void addDocSpecialWord(DocSpecialWord word) {
		m_DocSpecialWords.add(word);
	}

	/**
	 * 设置特殊名词
	 * 
	 * @param words 特殊名词
	 */
	public void setDocSpecialWords(List<DocSpecialWord> words) {
		m_DocSpecialWords = words;
	}

	/**
	 * 获取特殊名词
	 * 
	 * @return 特殊名词
	 */
	public List<DocSpecialWord> getDocSpecialWords() {
		return m_DocSpecialWords;
	}

	/**
	 * 设置未有凭证的验证器
	 * 
	 * @param authorizer 验证器
	 */
	public void setNoneAuthorizer(Authorizer authorizer) {
		m_RpcEndpoint.register("", authorizer);
	}

	/**
	 * 设置用户验证器
	 * 
	 * @param authorizer 验证器
	 */
	public void setUserAuthorizer(Authorizer authorizer) {
		m_RpcEndpoint.register(Access.KIND_USER, authorizer);
	}

	/**
	 * 设置服务验证器
	 * 
	 * @param authorizer 验证器
	 */
	public void setServiceAuthorizer(Authorizer authorizer) {
		m_RpcEndpoint.register(Access.KIND_SERVICE, authorizer);
	}

	/**
	 * 设置网关链接
	 * 
	 * @param url 链接
	 */
	public void setServicesUrl(String url) {
		m_ServicesUrl = url;
	}

	/**
	 * 设置访问凭证
	 * 
	 * @param aid 凭证
	 */
	public void setAccessId(String aid) {
		m_AccessId = aid;
		initAccessLoader();
	}

	/**
	 * 设置访问凭证
	 * 
	 * @param akey 凭证
	 */
	public void setAccessKey(String akey) {
		m_AccessKey = akey;
		initAccessLoader();
	}

	/* 初始化访问凭证 */
	private void initAccessLoader() {
		if (!StringUtil.isEmpty(m_AccessId) && !StringUtil.isEmpty(m_AccessKey)) {
			m_AccessLoader = new AccessLoader.Single(m_AccessId, m_AccessKey);
		}
	}

	/**
	 * 设置话题监听器
	 * 
	 * @param ls 话题监听
	 */
	public void setTopicListeners(List<TopicListener<?>> ls) {
		if (null == ls) {
			return;
		}
		for (TopicListener<?> l : ls) {
			subscribe(l);
		}
	}

	@Override
	public Access getValidAccess(String accessId) {
		if (null == m_AccessLoader) {
			return null;
		}
		return m_AccessLoader.getValidAccess(accessId);
	}

	/**
	 * 注册/心跳到网关
	 */
	protected void register() {
		if (null == m_ServiceRecorder) {
			if (StringUtil.isEmpty(m_ServicesUrl) || StringUtil.isEmpty(m_AccessId)
					|| StringUtil.isEmpty(m_AccessKey)) {
				return;
			}
			m_ServiceRecorder = new HttpServiceRegister(m_ServicesUrl, m_AccessId, m_AccessKey);
		}
		ServiceVo info = new ServiceVo();
		info.no = m_No;
		info.domain = m_Host;
		info.port = getPort();
		info.name = getName();
		info.version = getVersion();
		info.compatibleVersion = getCompatibleVersion();
		info.buildVersion = getImplementationVersion();
		info.documentMethod = getDocumentMethod();
		info.debugMethod = getDebugMethod();
		info.runningId = getRunningId();
		info.requestMaxSize = m_RequestMaxSize;
		if (m_ForwardEnable) {
			info.marks |= ServiceVo.MARK_FORWARD_ENABLE;
		}
		if (m_HeartbeatPeriod > 0) {
			info.heartbeatPeriod = m_HeartbeatPeriod;
		}
		try {
			ServiceRuntime runtime = new ServiceRuntime();
			VmStat.refresh();
			Memory mem = VmStat.getMemory();
			runtime.memoryMax = mem.getMax();
			runtime.memoryAlloc = mem.getAlloc();
			runtime.memoryUsable = mem.getUsable();
			runtime.gcFullCount = mem.getGcCount();
			runtime.gcFullTime = mem.getGcTime();
			runtime.threadCount = VmStat.getThreadCount();
			runtime.cpuUsageRate = VmStat.getProcessCpuLoad();
			runtime.timestamp = System.currentTimeMillis();
			runtime.startTime = getStartTime();
			runtime.upTime = getUpTime();
			m_ServiceRecorder.registerService(info, runtime);
			if (_Logger.isTraceEnabled()) {
				_Logger.trace("心跳:" + this + ",ver:" + getVersion() + ",build-ver:" + getImplementationVersion());
			}
		} catch (Throwable e) {
			_Logger.warn(this + " 注册异常", e);
		}
	}

	/**
	 * 注销
	 */
	protected void unregister() {
		if (null == m_ServiceRecorder) {
			m_ServiceRecorder = new HttpServiceRegister(m_ServicesUrl, m_AccessId, m_AccessKey);
		}
		ServiceVo info = new ServiceVo();
		info.no = m_No;
		info.domain = m_Host;
		info.port = getPort();
		info.name = getName();
		info.version = getVersion();
		info.documentMethod = getDocumentMethod();
		info.buildVersion = getImplementationVersion();
		info.runningId = System.getenv("RUNNING_ID");
		info.requestMaxSize = m_RequestMaxSize;
		if (m_ForwardEnable) {
			info.marks |= ServiceVo.MARK_FORWARD_ENABLE;
		}
		if (m_HeartbeatPeriod > 0) {
			info.heartbeatPeriod = m_HeartbeatPeriod;
		}
		try {
			m_ServiceRecorder.unregisterService(info);
			_Logger.info("unregister service");
		} catch (Throwable e) {
			_Logger.warn(this + " 注销异常", e);
		}
	}

	/**
	 * 注册方法
	 * 
	 * @param method 方法名
	 */
	public void registerMethod(ApiMethod method) {
		m_RpcEndpoint.register(method);
	}

	/**
	 * 注册文档对象
	 * 
	 * @param o 对象
	 */
	public void registerObject(DocObject o) {
		m_Objects.add(o);
	}

	/**
	 * 注册文档对象供应商
	 * 
	 * @param p 供应商
	 */
	public void registerObjectProvider(DocObjectProvider p) {
		m_ObjectProviders.add(p);
	}

	/**
	 * 注册资源
	 * 
	 * @param handler 处理者
	 */
	public void registerResources(ResourceHandler handler) {
		m_StreamEndpoint.register(handler);
	}

	/**
	 * 注册资源
	 * 
	 * @param downloader 下载者
	 */
	public void registerResources(ResourceDownloader downloader) {
		m_StreamEndpoint.register(downloader);
	}

	/**
	 * 注册资源
	 * 
	 * @param uploader 上传者
	 */
	public void registerResources(ResourceUploader uploader) {
		m_StreamEndpoint.register(uploader);
	}

	@Override
	public void destroy() {
		unregister();
		m_HttpServer.close();
	}

	@Override
	public void precheck(RestfulRequest request, RestfulResponse response) throws IOException {
		String verb = request.getVerb();
		if ("OPTIONS".equals(verb)) {
			/*
			 * 可能是跨域的预检请求（preflight request）
			 * 
			 * @see https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Access_control_CORS
			 */
			if (!StringUtil.isEmpty(request.getHeaders().get("Access-Control-Request-Method"))) {
				response.setHeader("Access-Control-Allow-Origin", "*");
				response.setHeader("Access-Control-Allow-Methods", "POST");
				response.setHeader("Access-Control-Allow-Headers",
						"Authorization,Content-Type,Content-Encoding,WF-Tag,WF-Noise,WF-content-sign,HY-Tag,HY-Noise,User-Agent,X-Requested-With,Accept,Accept-Encoding");
				// 减少预检请求的次数
				response.setHeader("Access-Control-Max-Age", "3600");
				response.setStatus(RestfulResponse.STATUS_OK);
			} else {
				response.setStatus(RestfulResponse.STATUS_BAD_REQUEST);
			}
			response.openOutput().close();
			return;
		}
		if (!"POST".equals(verb) && !"GET".equals(verb)) {
			// 只支持POST及GET
			response.setStatus(RestfulResponse.STATUS_METHOD_NOT_ALLOWED);
			response.openOutput().close();
			return;
		}

	}

	@Override
	public void service(final RestfulRequest request, RestfulResponse response) throws IOException {
		String path = request.getUri();
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers",
				"Authorization,Content-Type,Content-Encoding,WF-Tag,WF-Noise,WF-content-sign,HY-Tag,HY-Noise,User-Agent,X-Requested-With,Accept,Accept-Encoding");
		final Header reqHeader = new Header(getName());
		HttpHeaderHelper.fromHttpHeaders(request.getHeaders(), reqHeader);
		String channel = reqHeader.getChannel();
		if (StringUtil.eq(channel, Header.CHANNEL_STREAM)) {
			// 处理数据流
			m_StreamEndpoint.handle(request, response);
			return;
		}

		if (null == reqHeader.getAuthType()) {
			// 非weforward请求
			if (m_ShowDocEnable && path.startsWith("/__wf_doc/")) {
				ServiceDocumentVo vo = m_RpcEndpoint.getServiceDocument();
				response.setHeader("Content-Type", "application/json;charset=utf-8");
				if (null == vo) {
					response.setStatus(RestfulResponse.STATUS_NOT_FOUND);
					response.openOutput().close();
				} else {
					response.setStatus(RestfulResponse.STATUS_OK);
					List<ServiceDocumentVo> vos = Collections.singletonList(vo);
					SimpleDtObject result = new SimpleDtObject();
					result.put("docs", SimpleDtList.toDtList(vos, ServiceDocumentVo.MAPPER));
					try (OutputStream out = response.openOutput()) {
						JsonOutputStream jos = new JsonOutputStream(out);
						JsonSerialEngine.formatObject(result, jos);
					}
				}
				return;
			}
			response.setStatus(RestfulResponse.STATUS_BAD_REQUEST);
			response.openOutput().close();
			return;
		}
		Request wfrequest;
		try (InputStream content = request.getContent()) {
			wfrequest = m_Producer.fetchRequest(reqHeader, content);
		} catch (SerialException | AuthException | IOException e) {
			_Logger.warn("解析请求异常", e);
			response.setStatus(RestfulResponse.STATUS_BAD_REQUEST);
			response.openOutput().close();
			return;
		}
		long start = Long.MIN_VALUE;
		if (m_ElapseTime > 0 || getLogger().isDebugEnabled()) {
			start = System.currentTimeMillis();
		}
		response.setStatus(RestfulResponse.STATUS_OK);
		WeResponse hyresponse;
		hyresponse = new WeResponse(response, reqHeader);
		String traceToken = "";
		try {
			traceToken = wfrequest.getTraceToken();
			if (!StringUtil.isEmpty(traceToken)) {
				ServiceTraceToken.TTT.put(traceToken);
			}
			int timeout = wfrequest.getWaitTimeout();
			if (timeout <= 0 || timeout > 2000000) {
				// 置为默认的60-10秒超时值
				timeout = 50;
			}
			if (timeout > 1) {
				timeout = (timeout - 1) * 1000;
				hyresponse.setResponseTimeout(timeout);
			}
			DtBase content;
			if (StringUtil.eq(channel, "topic")) {
				// 主题消息广播
				FriendlyObject invoke = FriendlyObject.valueOf(wfrequest.getServiceInvoke());
				FriendlyObject params = invoke.getFriendlyObject("params");
				String topic = params.getString("topic");
				List<TopicListenerWrap<?>> list = m_Listeners.get(topic);
				if (null != list) {
					TopicWrap topicwrap = new TopicWrap(params);
					for (TopicListenerWrap<?> w : list) {
						w.onReceive(topicwrap, topicwrap.getContent());
					}
				}
				content = null;
			} else {
				content = m_RpcEndpoint.handle(wfrequest, hyresponse);
				if (hyresponse.isAsync()) {
					// 异步模式，由业务稍迟响应
					return;
				}
			}
			hyresponse.setServiceResult(RpcEndPoint.toResult(0, null, content));
		} catch (ForwardException e) {
			if (_Logger.isTraceEnabled()) {
				_Logger.trace("forwardTo: " + getLogDetail(reqHeader), e.getForwardTo(), e);
			}
			hyresponse.setForwardTo(e.getForwardTo());
			hyresponse.setResponseCode(WeforwardException.CODE_SERVICE_FORWARD);
			hyresponse.setResponseMsg(e.getMessage());
		} catch (ApiException e) {
			if (_Logger.isTraceEnabled()) {
				_Logger.trace(" request:{}", request, e);
			}
			hyresponse.setServiceResult(RpcEndPoint.toResult(e));
		} catch (WeforwardException e) {
			if (WeforwardException.CODE_UNREADY == e.getCode()) {
				response.setStatus(RestfulResponse.STATUS_SERVICE_UNAVAILABLE);
				getLogger().warn(getLogDetail(reqHeader) + "\n" + e.toString());
			} else if (WeforwardException.CODE_ACCESS_ID_INVALID == e.getCode()) {
				getLogger().warn(getLogDetail(reqHeader) + "\n" + e.toString());
			} else {
				getLogger().error(getLogDetail(reqHeader), e);
			}
			hyresponse = createErrorResponse(response, reqHeader, e);
		} catch (Throwable e) {
			getLogger().error(reqHeader.getLogDetail() + " traceToken:" + traceToken, e);
			hyresponse = createErrorResponse(response, reqHeader, e);
		} finally {
			if (Long.MIN_VALUE != start) {
				start = System.currentTimeMillis() - start;
				if (start > m_ElapseTime) {
					if (getLogger().isWarnEnabled()) {
						getLogger().warn("[use " + start + "ms] " + getLogDetail(reqHeader));
					}
				} else if (getLogger().isDebugEnabled()) {
					getLogger().debug("[use " + start + "ms] " + getLogDetail(reqHeader));
				}
			}
		}
		try {
			// 马上响应
			hyresponse.complete();
		} catch (IOException e) {
			getLogger().warn(getLogDetail(reqHeader), e);
		}
	}

	/**
	 * 输出详细信息，用于日志记录
	 * 
	 * @return
	 */
	private static String getLogDetail(Header header) {
		return "{s:" + header.getService() + ",acc:" + header.getAccessId() + ",at:" + header.getAuthType() + ",t:"
				+ ServiceTraceToken.TTT.get() + "}";
	}

	@Override
	public void timeout(RestfulRequest request, RestfulResponse response) throws IOException {
		response.setStatus(RestfulResponse.STATUS_OK);
		Header reqHeader = new Header(getName());
		HttpHeaderHelper.fromHttpHeaders(request.getHeaders(), reqHeader);
		WeResponse hyresponse = createErrorResponse(response, reqHeader, null);
		hyresponse.setResponseCode(WeforwardException.CODE_SERVICE_TIMEOUT);
		hyresponse.complete();
	}

	protected WeResponse createErrorResponse(RestfulResponse rsp, Header header, Throwable error) {
		Header respHeader = new Header(header.getService());
		String contentType = header.getContentType();
		if (StringUtil.isEmpty(contentType)) {
			contentType = Header.CONTENT_TYPE_JSON;
		}
		respHeader.setContentType(contentType);
		String charset = header.getCharset();
		if (StringUtil.isEmpty(charset)) {
			charset = Header.CHARSET_UTF8;
		}
		respHeader.setCharset(charset);
		respHeader.setAuthType(Header.AUTH_TYPE_NONE);
		WeResponse resp = new WeResponse(rsp);
		resp.setHeader(respHeader);
		if (error instanceof WeforwardException) {
			resp.setResponseCode(((WeforwardException) error).getCode());
			resp.setResponseMsg(error.getMessage());
		} else if (null != error) {
			resp.setResponseCode(WeforwardException.CODE_UNDEFINED);
			resp.setResponseMsg(error.toString());
		} else {
			resp.setResponseCode(WeforwardException.CODE_UNDEFINED);
		}
		return resp;
	}

	protected Logger getLogger() {
		return _Logger;
	}

	/**
	 * 异步响应支持封装
	 * 
	 * @author liangyi
	 *
	 */
	protected class WeResponse extends SimpleResponse implements AsyncResponse, Producer.Output, HttpHeaderOutput {
		protected RestfulResponse m_RestfulResponse;
		protected boolean m_Async;
		protected int m_ResponseTimeout;
		protected OutputStream m_ResponseOutput;

		public WeResponse(RestfulResponse rsp) {
			m_RestfulResponse = rsp;
		}

		public WeResponse(RestfulResponse rsp, Header reqHeader) {
			m_RestfulResponse = rsp;
			// 补充header
			Header respHeader = new Header(reqHeader.getService());
			respHeader.setContentType(reqHeader.getContentType());
			respHeader.setCharset(reqHeader.getCharset());
			respHeader.setAuthType(reqHeader.getAuthType());
			respHeader.setAccessId(reqHeader.getAccessId());
			setHeader(respHeader);
		}

		public boolean isAsync() {
			return m_Async;
		}

		@Override
		public void setServiceResult(int code, String message, DtBase content) {
			setServiceResult(RpcEndPoint.toResult(code, message, content));
		}

		@Override
		public void setAsync() throws IOException {
			m_Async = true;
		}

		@Override
		public void setResponseTimeout(int millis) throws IOException {
			m_ResponseTimeout = millis;
			m_RestfulResponse.setResponse(millis);
		}

		@Override
		public int getResponseTimeout() {
			return m_ResponseTimeout;
		}

		@Override
		public void complete() throws IOException {
			// 先使用暂留缓存输出序列化内容
			OutputStream out = OutputStreamStay.Wrap.wrap(m_RestfulResponse.openOutput());
			try {
				((OutputStreamStay) out).stay();
				m_Producer.make(this, out);
				// 然后输出HTTP头
				HttpHeaderHelper.outHeaders(getHeader(), this);
				// 再刷出内容
				out.flush();
			} catch (Throwable e) {
				// 居然在输出的时候出错，尴尬了
				// 只能直接关闭（取消）响应
				m_RestfulResponse.close();
				out = null;
				if (e instanceof IOException) {
					throw (IOException) e;
				}
				_Logger.error(e.toString(), e);
			} finally {
				if (null != out) {
					out.close();
				}
				m_RestfulResponse = null;
			}

		}

		@Override
		public void put(String name, String value) throws IOException {
			m_RestfulResponse.setHeader(name, value);
		}

		@Override
		public void writeHeader(Header header) throws IOException {
			HttpHeaderHelper.outHeaders(getHeader(), this);
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return m_ResponseOutput;
		}
	}

	protected class TopicWrap implements Topic {

		protected FriendlyObject m_Params;

		protected TopicWrap(FriendlyObject params) {
			m_Params = params;
		}

		@Override
		public String getId() {
			return m_Params.getString("id");
		}

		@Override
		public String getTopic() {
			return m_Params.getString("topic");
		}

		@Override
		public String getTag() {
			return m_Params.getString("tag");
		}

		@Override
		public String getDeliver() {
			return m_Params.getString("deliver");
		}

		public DtBase getContent() {
			return m_Params.getBase("content");
		}

		@Override
		public String toString() {
			return getTopic() + ":" + getId();
		}

	}

	protected class TopicListenerWrap<E> implements TopicListener<DtBase> {

		public TopicListener<E> m_Listener;

		private Class<?> m_ContentClass;

		public TopicListenerWrap(TopicListener<E> l) {
			m_Listener = l;
		}

		@Override
		public String getTopic() {
			return m_Listener.getTopic();
		}

		@Override
		public void onReceive(Topic topic, DtBase content) {
			m_Listener.onReceive(topic, toContent(content));
		}

		private E toContent(DtBase content) {
			if (null == m_ContentClass) {
				m_ContentClass = ClassUtil.find(m_Listener.getClass(), TopicListener.class, "E");
			}
			return MappedUtil.fromBase(m_ContentClass, content);
		}

	}

	@Override
	public synchronized <E> void subscribe(TopicListener<E> l) {
		String name = l.getTopic();
		List<TopicListenerWrap<?>> list = m_Listeners.get(name);
		if (null == list) {
			list = new ArrayList<>();
		}
		for (TopicListenerWrap<?> wrap : list) {
			if (wrap.m_Listener == l) {
				return;// 已存在
			}
		}
		list.add(new TopicListenerWrap<E>(l));
		m_Listeners.put(name, list);
	}

	@Override
	public synchronized <E> void unSubscribe(TopicListener<E> l) {
		String name = l.getTopic();
		List<TopicListenerWrap<?>> list = m_Listeners.get(name);
		if (null == list) {
			return;
		}
		List<TopicListenerWrap<?>> news = new ArrayList<>();
		for (TopicListenerWrap<?> w : list) {
			if (w.m_Listener == l) {
				continue;
			}
			news.add(w);
		}
		m_Listeners.put(name, news);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		List<ApplicationContextAware> awares = m_ChildAwares;
		for (ApplicationContextAware aware : awares) {
			aware.setApplicationContext(applicationContext);
		}

	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		MethodsRegister r = m_MethodsRegister;
		if (null != r) {
			r.register(bean);
		}
		return bean;
	}

	@Override
	public String toString() {
		return getHost() + ":" + getPort() + "/" + getName();
	}

}
