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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.weforward.common.io.BytesOutputStream;
import cn.weforward.common.json.JsonUtil;
import cn.weforward.common.restful.RestfulRequest;
import cn.weforward.common.restful.RestfulResponse;
import cn.weforward.common.util.ListUtil;
import cn.weforward.common.util.StringUtil;
import cn.weforward.framework.ApiException;
import cn.weforward.framework.ResourceDownloader;
import cn.weforward.framework.ResourceException;
import cn.weforward.framework.ResourceHandler;
import cn.weforward.framework.ResourceUploader;
import cn.weforward.framework.WeforwardFile;
import cn.weforward.framework.util.WeforwardResourceHelper;
import cn.weforward.framework.web.upload.WebFileUpload;
import cn.weforward.framework.web.upload.WebForm;
import cn.weforward.metrics.WeforwardMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * 资源端点
 * 
 * @author daibo
 *
 */
public class StreamEndPoint {
	/** 日志输出 */
	static final Logger _Logger = LoggerFactory.getLogger(StreamEndPoint.class);

	protected List<ResourceHandler> m_Resuorces;

	protected List<ResourceUploader> m_Uploaders;

	protected List<ResourceDownloader> m_Downloaders;
	/** 当前并发数 */
	private AtomicInteger m_CurrentRequest;
	/** 监控指标 */
	private MeterRegistry m_MeterRegistry;

	public StreamEndPoint() {
		m_Resuorces = new ArrayList<>();
		m_Uploaders = new ArrayList<>();
		m_Downloaders = new ArrayList<>();
		m_CurrentRequest = new AtomicInteger();
	}

	public void setMeterRegistry(MeterRegistry registry) {
		m_MeterRegistry = registry;
		if (null != m_MeterRegistry) {
			Gauge.builder(WeforwardMetrics.STREAM_CURRENT_REQUEST_KEY, m_CurrentRequest, AtomicInteger::doubleValue)
					.strongReference(true).register(m_MeterRegistry);
		}
	}

	public MeterRegistry getMeterRegistry() {
		return m_MeterRegistry;
	}

	public void register(ResourceHandler resources) {
		m_Resuorces.add(resources);
	}

	public void register(ResourceUploader u) {
		m_Uploaders.add(u);
	}

	public void register(ResourceDownloader d) {
		m_Downloaders.add(d);
	}

	// @Override
	public void handle(RestfulRequest request, RestfulResponse response) throws IOException {
		MeterRegistry mr = getMeterRegistry();
		long start = System.currentTimeMillis();
		int code = 0;
		Tags tags = Tags.empty();
		try {
			m_CurrentRequest.incrementAndGet();
			String path = request.getUri();
			if (null != mr) {
				tags = WeforwardMetrics.TagHelper.of(WeforwardMetrics.TagHelper.method(path));
			}
			doHandle(request, response);
		} catch (ResourceException e) {
			response.setHeader("Content-Type", "application/json;charset=utf-8");
			response.setStatus(RestfulResponse.STATUS_OK);
			code = e.getCode();
			error(response, code, e.getMessage());
		} catch (RuntimeException e) {
			_Logger.warn("处理异常," + request.getUri(), e);
			response.setHeader("Content-Type", "application/json;charset=utf-8");
			response.setStatus(RestfulResponse.STATUS_INTERNAL_SERVER_ERROR);
			code = ApiException.CODE_INTERNAL_ERROR;
			error(response, code, e.getMessage());
		} finally {
			long end = System.currentTimeMillis();
			long amount = end - start;
			if (null != mr) {
				tags = tags.and(WeforwardMetrics.TagHelper.code(code));
				mr.timer(WeforwardMetrics.STREAM_REQUEST_KEY, tags).record(amount, TimeUnit.MILLISECONDS);
			}
			m_CurrentRequest.decrementAndGet();
		}
	}

	private void error(RestfulResponse response, int code, String msg) throws IOException {
		// {
		// code:0,//0表示成功，非0表示失败
		// msg:'提示信息'
		// }
		try (OutputStream out = response.openOutput()) {
			StringBuilder sb = new StringBuilder();
			sb.append("{\"code\":");
			sb.append(code);
			sb.append(",\"msg\":\"");
			JsonUtil.escape(msg, sb);
			sb.append("\"}");
			out.write(sb.toString().getBytes("utf-8"));
		}
	}

	private void doHandle(RestfulRequest request, RestfulResponse response) throws IOException {
		String resourceId = request.getParams().get("id");
		String contentType = request.getHeaders().get("Content-Type");
		if (!ListUtil.isEmpty(m_Uploaders) && !StringUtil.isEmpty(contentType)
				&& contentType.contains("multipart/form-data")) {
			// 上传
			WebFileUpload u = new WebFileUpload();
			u.input(request.getContent());
			List<WeforwardFile> files = new ArrayList<>();
			for (int i = 0; i < u.size(); i++) {
				WebForm form = u.get(i);
				String type = form.getContentType();
				if (!StringUtil.isEmpty(type)) {
					// 非空为文件
					files.add(WeforwardResourceHelper.newFile(form.getFileName(), form.getStream()));
				}
			}
			if (files.isEmpty()) {
				response.setStatus(RestfulResponse.STATUS_BAD_REQUEST);
				response.openOutput().close();
				return;
			}

			WeforwardFile[] fs = new WeforwardFile[files.size()];
			fs = files.toArray(fs);
			for (ResourceUploader up : m_Uploaders) {
				boolean isOk = up.saveFile(resourceId, fs);
				if (isOk) {
					response.setStatus(RestfulResponse.STATUS_OK);
					response.openOutput().close();
					return;
				}
			}
		}

		for (ResourceDownloader d : m_Downloaders) {
			// 下载
			WeforwardFile file = d.findFile(resourceId);
			if (null != file) {
				String fileName = file.getName();
				if (StringUtil.isEmpty(fileName)) {
					fileName = "未知";
				}
				// fileName = fileName.replace(",", "，").replace(" ", "");
				fileName = fileName.replace('"', ' ');
				try {
					fileName = new String(fileName.getBytes("gbk"), "iso8859-1");// 文件名称
				} catch (Throwable e) {
				}
				String type = file.getContentType();
				if (!StringUtil.isEmpty(type)) {
					response.setHeader("Content-Type", type);
				} else {
					response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
				}
				response.setStatus(RestfulResponse.STATUS_OK);
				try (InputStream in = file.getStream(); OutputStream out = response.openOutput()) {
					BytesOutputStream.transfer(in, out, 0);
				}
				return;
			}
		}
		for (ResourceHandler r : m_Resuorces) {
			if (r.handle(request, response)) {
				return;
			}
		}
		response.setStatus(RestfulResponse.STATUS_NOT_FOUND);
		response.openOutput().close();
	}

}
