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

import java.util.Collections;
import java.util.Objects;

import cn.weforward.common.util.StringUtil;
import cn.weforward.trace.Trace;
import io.micrometer.core.instrument.Tag;

/**
 * 抽象追踪实现
 * 
 * @author daibo
 *
 */
public class AbstractTrace implements Trace {

	protected String m_Id;

	protected String m_ParentId;

	protected String m_TraceId;

	protected long m_Timestamp;

	protected long m_Duration;

	protected String m_Kind;

	protected Iterable<Tag> m_Tags;

	protected AbstractTrace(String id, String parentId, String traceId, long timestamp, long duration, String kind,
			Iterable<Tag> tags) {
		Objects.requireNonNull(id, "id不能为空");
		Objects.requireNonNull(traceId, "traceId不能为空");
		Objects.requireNonNull(kind, "kind不能为空");
		m_Id = id;
		m_ParentId = parentId;
		m_TraceId = traceId;
		m_Timestamp = timestamp;
		m_Duration = duration;
		m_Kind = kind;
		m_Tags = null == tags ? Collections.emptyList() : tags;
	}

	@Override
	public String getId() {
		return m_Id;
	}

	@Override
	public String getParentId() {
		return m_ParentId;
	}

	@Override
	public String getTraceId() {
		return m_TraceId;
	}

	@Override
	public long getTimestamp() {
		return m_Timestamp;
	}

	@Override
	public long getDuration() {
		return m_Duration;
	}

	@Override
	public String getKind() {
		return m_Kind;
	}

	@Override
	public Iterable<Tag> getTags() {
		return m_Tags;
	}

	@Override
	public String getTag(String key) {
		for (Tag t : getTags()) {
			if (null == t) {
				continue;
			}
			if (StringUtil.eq(key, t.getKey())) {
				return t.getValue();
			}
		}
		return null;
	}

}
