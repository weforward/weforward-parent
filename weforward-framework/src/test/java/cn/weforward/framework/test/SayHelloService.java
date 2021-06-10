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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import cn.weforward.common.io.BytesOutputStream;
import cn.weforward.common.util.BackgroundExecutor;
import cn.weforward.common.util.TaskExecutor;
import cn.weforward.framework.ApiException;
import cn.weforward.framework.ApiMethod;
import cn.weforward.framework.ResourceDownloader;
import cn.weforward.framework.ResourceUploader;
import cn.weforward.framework.WeforwardFile;
import cn.weforward.framework.WeforwardSession;
import cn.weforward.framework.exception.ForwardException;
import cn.weforward.framework.ext.DebugMethod;
import cn.weforward.framework.ext.DocumentMethod;
import cn.weforward.framework.ext.WeforwardService;
import cn.weforward.framework.support.AbstractApiMethod;
import cn.weforward.framework.util.WeforwardResourceHelper;
import cn.weforward.protocol.AsyncResponse;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.ServiceInvoker;
import cn.weforward.protocol.client.ServiceInvokerFactory;
import cn.weforward.protocol.client.ext.RequestInvokeObject;
import cn.weforward.protocol.datatype.DataType;
import cn.weforward.protocol.datatype.DtBase;
import cn.weforward.protocol.datatype.DtObject;
import cn.weforward.protocol.doc.DocAttribute;
import cn.weforward.protocol.support.CommonServiceCodes;
import cn.weforward.protocol.support.datatype.FriendlyObject;
import cn.weforward.protocol.support.datatype.SimpleDtString;
import cn.weforward.protocol.support.doc.DocAttributeVo;
import cn.weforward.protocol.support.doc.DocMethodVo;
import cn.weforward.protocol.support.doc.ServiceDocumentVo;

public class SayHelloService {

	String gateWayUrl = "http://127.0.0.1:5661/";
	String accessId = "H-0947f4f50120-0947f4f50120";
	String accessKey = "0abdc16ef73b3a02f31b5cc7b67467cfb02e30fb4a32bc9f2e08d12e80f6ee54";

	String host = "192.168.0.32";
	int port = 20004;
	String no = "x0004";
	WeforwardService service;
	TaskExecutor executor;

	AtomicInteger count = new AtomicInteger();

	@Test
	public void main() throws Exception {
		service = new WeforwardService("say_hello", host, port, "", 0);
		service.setNo(no);
		service.setGatewayUrl(gateWayUrl);
		service.setAccessId(accessId);
		service.setAccessKey(accessKey);
		service.setForwardEnable(true);
		service.setMaxHttpSize(10 * 1024 * 1024);

		service.registerMethod(say);
		service.registerMethod(pressure);
		service.registerMethod(upload);
		service.registerMethod(onUploadComplete);
		service.registerMethod(new MyDocumentMethod(service));
		service.registerMethod(new MyDebugMethod(service));

		service.registerResources(downloader);
		service.registerResources(uploader);
		service.setDebugEnabled(true);

		executor = new BackgroundExecutor(2, 10, 10000);

		Thread.sleep(Long.MAX_VALUE);
	}

	ApiMethod say = new AbstractApiMethod("say") {

		@Override
		public DtBase handle(String path, DtObject _params, Request request, Response response) throws ApiException {
			if (WeforwardSession.TLS.isSupportForward()) {
				throw new ForwardException("");
			}
			response.setResourceId("1.jpg");
			response.setResourceExpire((System.currentTimeMillis() / 1000) + 3600);
			FriendlyObject params = new FriendlyObject(_params);
			return SimpleDtString.valueOf(no + ": hello " + params.getString("name"));
		}
	};

	ApiMethod pressure = new AbstractApiMethod("pressure") {

		@Override
		public DtBase handle(String path, DtObject _params, Request reqeuest, Response response) throws ApiException {
			int c = count.incrementAndGet();
			System.out.println(no + ":" + c);
			final AsyncResponse asyncResponse = (AsyncResponse) response;
			try {
				asyncResponse.setAsync();
			} catch (IOException e) {
				throw new ApiException(ApiException.CODE_INTERNAL_ERROR, e);
			}
			Runnable worker = new Runnable() {
				@Override
				public void run() {
					FriendlyObject params = new FriendlyObject(_params);
					asyncResponse.setServiceResult(0, "", SimpleDtString.valueOf(params.getString("arg1")));
					try {
						asyncResponse.complete();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			executor.execute(worker, 0, 1000);
			return null;
			// FriendlyObject params = new FriendlyObject(_params);
			// return SimpleDtString.valueOf(params.getString("arg1"));
		}
	};

	ApiMethod upload = new AbstractApiMethod("upload") {

		@Override
		public DtBase handle(String path, DtObject _params, Request request, Response response) throws ApiException {
			String resourceService = "file";
			ServiceInvoker invoker = ServiceInvokerFactory.create(resourceService, gateWayUrl, accessId, accessKey);
			RequestInvokeObject req = new RequestInvokeObject("/pic/add_picture");
			req.putParam("reference", "say_hello_test");
			req.putParam("callbackUri", "event://say_hello");
			Response resp = invoker.invoke(req.toDtObject());
			if (0 != resp.getResponseCode()) {
				throw new ApiException(CommonServiceCodes.INTERNAL_ERROR.code,
						resp.getResponseCode() + "/" + resp.getResponseMsg());
			}
			DtObject result = resp.getServiceResult();
			int code = FriendlyObject.getInt(result, "code");
			if (0 != code) {
				String msg = FriendlyObject.getString(result, "msg");
				throw new ApiException(CommonServiceCodes.INTERNAL_ERROR.code, code + "/" + msg);
			}
			DtObject resutlContent = result.getObject("content");
			String resId = FriendlyObject.getString(resutlContent, "id");
			long resExpire = FriendlyObject.getLong(resutlContent, "expire");
			// return WeforwardResourceHelper.valueOf(resId, resExpire, resourceService,
			// null);
			response.setResourceId(resId);
			response.setResourceExpire((System.currentTimeMillis() / 1000) + resExpire);
			response.setResourceService(resourceService);
			return null;
		}
	};

	ApiMethod onUploadComplete = new AbstractApiMethod("on_upload_complete") {

		@Override
		public DtBase handle(String path, DtObject params, Request reqeuest, Response response) throws ApiException {
			System.out.println("上传成功：" + params);
			return null;
		}
	};

	ResourceDownloader downloader = new ResourceDownloader() {

		@Override
		public WeforwardFile findFile(String resourceId) throws IOException {
			if ("1.jpg".equals(resourceId)) {
				InputStream in = new FileInputStream("1.jpg");
				return WeforwardResourceHelper.newFile("1.jpg", in);
			}
			return null;
		}

	};

	ResourceUploader uploader = new ResourceUploader() {

		@Override
		public boolean saveFile(String resourceId, WeforwardFile... files) throws IOException {
			if (!"1.jpg".equals(resourceId)) {
				return false;
			}
			for (int i = 0; i < files.length; i++) {
				WeforwardFile f = files[i];
				FileOutputStream fout = new FileOutputStream("u" + i + "_" + f.getName() + ".jpg");
				InputStream in = f.getStream();
				BytesOutputStream.transfer(in, fout, 0);
				fout.close();
				in.close();
			}
			return true;
		}

	};

	static class MyDocumentMethod extends DocumentMethod {

		public MyDocumentMethod(WeforwardService service) {
			super(service);
		}

		@Override
		public DtBase handle(String path, DtObject params, Request request, Response response) throws ApiException {
			ServiceDocumentVo doc = new ServiceDocumentVo();
			doc.name = "test";
			doc.version = "1.0";
			doc.description = "仅用于测试";
			List<DocMethodVo> methods = new ArrayList<>();
			DocMethodVo method = new DocMethodVo();
			method.name = "test_method";
			method.description = "一个测试的方法";
			List<DocAttributeVo> ps = new ArrayList<DocAttributeVo>();
			DocAttributeVo p1 = new DocAttributeVo();
			p1.name = "param1";
			p1.type = DataType.STRING.value;
			p1.description = "参数1";
			p1.marks = DocAttribute.MARK_NECESSARY;
			ps.add(p1);
			DocAttributeVo p2 = new DocAttributeVo();
			p2.name = "param2";
			p2.type = DataType.NUMBER.value;
			p2.description = "参数2";
			ps.add(p2);
			List<DocAttributeVo> rs = new ArrayList<DocAttributeVo>();
			DocAttributeVo r1 = new DocAttributeVo();
			r1.name = "result1";
			r1.type = DataType.STRING.value;
			r1.description = "返回值1";
			r1.marks = DocAttribute.MARK_NECESSARY;
			rs.add(r1);
			method.params = ps;
			method.returns = rs;
			methods.add(method);
			doc.methods = methods;
			doc.modifies = Collections.emptyList();
			return ServiceDocumentVo.MAPPER.toDtObject(doc);
		}
	};

	static class MyDebugMethod extends DebugMethod {

		public MyDebugMethod(WeforwardService service) {
			super(service);
		}

	}
}
