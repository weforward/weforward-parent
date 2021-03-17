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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import cn.weforward.common.restful.RestfulRequest;
import cn.weforward.common.restful.RestfulResponse;
import cn.weforward.common.restful.RestfulService;
import cn.weforward.common.sys.Shutdown;
import cn.weforward.common.util.SimpleKvPair;
import cn.weforward.common.util.StringUtil;
import cn.weforward.common.util.ThreadPool;
import cn.weforward.framework.web.upload.WebFileUpload;
import cn.weforward.framework.web.upload.WebForm;
import cn.weforward.protocol.aio.http.RestfulServer;
import cn.weforward.protocol.aio.netty.NettyHttpServer;

/**
 * 测试web文件上传
 * 
 * @author liangyi
 *
 */
public class Upload implements RestfulService {

	@Override
	public void precheck(RestfulRequest request, RestfulResponse response) throws IOException {
	}

	@Override
	public void service(RestfulRequest request, RestfulResponse response) throws IOException {
		OutputStream out = null;
		try {
			// upload(request.getContent());
			upload2(request.getContent());
			out = response.openOutput();
			out.write("ok".getBytes());
			out.close();
			out = null;
		} finally {
			if (null != out) {
				response.close();
			}
		}
	}

	@Override
	public void timeout(RestfulRequest request, RestfulResponse response) throws IOException {
		// TODO Auto-generated method stub
	}

	public void restful() throws IOException {
		RestfulServer restfulServer = new RestfulServer(new Upload());
		// restfulServer.setProxyIps("127.0.0.1");
		// restfulServer.setAllowIps("127.0.0.1;192.168.0.0-192.168.0.100");
		restfulServer.setQuickHandle(true);
		ThreadPool tp = new ThreadPool(10, "test");
		Shutdown.register(tp);
		restfulServer.setExecutor(tp);

		NettyHttpServer httpServer;
		httpServer = new NettyHttpServer(8080);
		httpServer.setName("ww");
		httpServer.setMaxHttpSize(100 * 1024 * 1024);
		// httpServer.setIdle(30);
		httpServer.setHandlerFactory(restfulServer);
		httpServer.start();
		System.out.println("'q' key stop");
		while ('q' != System.in.read()) {
		}
		httpServer.close();
		tp.shutdown();
		System.out.println("done.");
	}

	private void dump(WebFileUpload fileUpload) throws IOException {
		// 枚举所有表单项
		for (int i = 0; i < fileUpload.size(); i++) {
			WebForm form = fileUpload.get(i);
			System.out.print((1 + i) + ".");
			System.out.println(form);
			if (StringUtil.isEmpty(form.getFileName())) {
				System.out.println(form.getString());
				System.out.println("------------------|");
			}
		}
	}

	public void upload2(InputStream in) throws IOException {
		File localFile = new File("stream.bin");
		File dir = localFile.getParentFile();
		if (null != dir) {
			dir.mkdirs();
		}
		try (FileOutputStream out = new FileOutputStream(localFile)) {
			WebFileUpload fileUpload = new WebFileUpload(
					Arrays.asList(SimpleKvPair.valueOf("file", out)));
			// 处理上传流
			fileUpload.input(in);
			dump(fileUpload);
		}
	}

	public void upload(InputStream in) throws IOException {
		WebFileUpload fileUpload = new WebFileUpload(
				Arrays.asList(SimpleKvPair.valueOf("file", new File("test-upload.bin"))));
		// /fileUpload.savefullpath="v:\\tmp\\haha";
		// 处理上传流
		fileUpload.input(in);
		dump(fileUpload);
	}

	public static void main(String[] args) throws Exception {
		Upload test = new Upload();
		// 上传文件测试
		test.restful();

		// // 由文件装入测试
		// FileInputStream in = new FileInputStream("v:\\test_swf.bin");
		// test.upload(in);
	}

}
