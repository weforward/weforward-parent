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
package cn.weforward.trace.ext;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.json.JsonUtil;
import cn.weforward.common.util.RingBuffer;
import cn.weforward.common.util.StringUtil;
import cn.weforward.trace.Trace;
import cn.weforward.trace.TraceRegistry;
import cn.weforward.trace.WeforwardTrace;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.Tag;

/**
 * 抽象登陆实现
 * 
 * @author daibo
 *
 */
public abstract class AbstractTraceRegistry implements TraceRegistry, Runnable {
	/** 日志记录器 */
	protected static final Logger _Logger = LoggerFactory.getLogger(AbstractTraceRegistry.class);
	/** 缓存的vo */
	protected RingBuffer<Trace> m_Items;
	/** 对象锁 */
	final Object m_Lock = new Object();
	/** 处理间隔 */
	protected long m_Interval = 1000l;
	/** 最大量 */
	protected long m_MaxSupport = 100;
	/** 远程url */
	protected List<URL> m_Urls;
	/** 默认标签 */
	protected List<Tag> m_Tags;
	/** 线程 */
	protected Thread m_Thread;

	public AbstractTraceRegistry() {
		m_Tags = new ArrayList<>();
		start();
	}

	/**
	 * 服务id
	 * 
	 * @param id 服务id
	 */
	public void setServiceId(String id) {
		setCommonTags(WeforwardTrace.LABEL_SERVICE_ID, id);
	}

	/**
	 * 服务编号
	 * 
	 * @param no 服务编号
	 */
	public void setServiceNo(String no) {
		setCommonTags(WeforwardTrace.LABEL_SERVICE_NO, no);
	}

	/**
	 * 服务名
	 * 
	 * @param name 服务名
	 */
	public void setServiceName(String name) {
		setCommonTags(WeforwardTrace.LABEL_SERVICE_NAME, name);
	}

	public void setCommonTags(String key, String value) {
		m_Tags.add(new ImmutableTag(key, value));

	}

	public List<Tag> getCommonTags() {
		return m_Tags;
	}

	public void setInterval(int interval) {
		m_Interval = interval;
	}

	public void setMaxSupport(int max) {
		m_MaxSupport = max;
	}

	public void start() {
		m_Items = new RingBuffer<>(1024);
		Thread thread = new Thread(this, "trace-publisher");
		thread.setDaemon(true);
		m_Thread = thread;
		thread.start();
	}

	public void stop() {
		synchronized (m_Lock) {
			m_Thread = null;
			m_Lock.notify();
		}
	}

	@Override
	public void register(Trace trace) {
		if (null == m_Thread) {
			return;// 线程没开始不允许加元素
		}
		m_Items.offer(trace);
	}

	public void close() {
		stop();
	}

	@Override
	public void run() {
		while (null != m_Thread) {
			try {
				do {
					send();
					synchronized (m_Lock) {
						try {
							m_Lock.wait(m_Interval);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							// 结束运行
							return;
						}
						if (null == m_Thread) {
							// 结束运行
							return;
						}
					}
				} while (null != m_Thread);
			} catch (Throwable e) {
				try {
					addError("运行出错", e);
				} catch (Throwable error) {
					// 不处理
				}
			}
			synchronized (this) {
				try {
					wait(10 * 1000);
				} catch (InterruptedException e) {
					return;
				}
			}
		}

	}

	protected void send() {
		try {
			Trace vo;
			while (null != (vo = m_Items.remove())) {
				send(vo);
			}
		} catch (Exception e) {
			addError("Error occurred while sending Remote.", e);
		}
	}

	protected abstract void send(Trace vo) throws Exception;

	protected void addError(String msg, Throwable e) {
		if (_Logger.isDebugEnabled()) {
			_Logger.debug(msg, e);
		}

	}

	/* 输出数据 */
	protected void out(Appendable sb, Trace vo) throws IOException {
		sb.append('{');
		sb.append("\"id\":\"");
		JsonUtil.escape(vo.getId(), sb);
		sb.append("\",");
		sb.append("\"parentId\":\"");
		JsonUtil.escape(vo.getParentId(), sb);
		sb.append("\",");
		sb.append("\"traceId\":\"");
		JsonUtil.escape(vo.getTraceId(), sb);
		sb.append("\",");
		sb.append("\"timestamp\":");
		sb.append(String.valueOf(vo.getTimestamp()));
		sb.append(",");
		sb.append("\"duration\":");
		sb.append(String.valueOf(vo.getDuration()));
		sb.append(",");
		sb.append("\"kind\":\"");
		JsonUtil.escape(vo.getKind(), sb);
		sb.append("\",");
		sb.append("\"tags\":{");
		Iterable<Tag> tags = vo.getTags();
		if (null != tags) {
			boolean first = true;
			for (Tag tag : tags) {
				if (null == tag) {
					continue;
				}
				if (first) {
					first = false;
				} else {
					sb.append(',');
				}
				sb.append("\"");
				JsonUtil.escape(tag.getKey(), sb);
				sb.append("\":\"");
				JsonUtil.escape(tag.getValue(), sb);
				sb.append("\"");
			}
			List<Tag> commons = getCommonTags();
			for (int i = 0; i < commons.size(); i++) {
				Tag tag = commons.get(i);
				if (null == tag) {
					continue;
				}
				if (exists(tags, tag)) {
					continue;
				}
				if (first) {
					first = false;
				} else {
					sb.append(',');
				}
				sb.append("\"");
				JsonUtil.escape(tag.getKey(), sb);
				sb.append("\":\"");
				JsonUtil.escape(tag.getValue(), sb);
				sb.append("\"");
			}
		}
		sb.append("}");
		sb.append("}");

	}

	protected static boolean exists(Iterable<Tag> tags, Tag tag) {
		for (Tag t : tags) {
			if (null == t) {
				continue;
			}
			if (StringUtil.eq(t.getKey(), tag.getKey())) {
				return true;
			}
		}
		return false;
	}

}
