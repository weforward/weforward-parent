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

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import cn.weforward.common.util.StringUtil;
import cn.weforward.protocol.Header;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.ServiceInvoker;
import cn.weforward.protocol.client.ServiceInvokerFactory;
import cn.weforward.protocol.client.ext.RequestInvokeObject;
import cn.weforward.protocol.support.datatype.FriendlyObject;

public class SayHelloClient {

	String gateWayUrl = "http://127.0.0.1:5661/";
	String accessId = "H-0947f4f50120-0947f4f50120";
	String accessKey = "0abdc16ef73b3a02f31b5cc7b67467cfb02e30fb4a32bc9f2e08d12e80f6ee54";

	ServiceInvoker invoker;

	@Before
	public void init() {
		invoker = ServiceInvokerFactory.create("say_hello", gateWayUrl, accessId, accessKey);
	}

	@Test
	public void say() {
		RequestInvokeObject invokeObj = new RequestInvokeObject("say");
		invokeObj.putParam("name", "weforward");
		Request req = invoker.createRequest(invokeObj.toDtObject());
		// req.setWaitTimeout(3600);
		Response resp = invoker.invoke(req);
		System.out.println(resp.getResponseCode() + "/" + resp.getResponseMsg());
		System.out.println(resp.getResourceUrl());
		System.out.println(resp.getServiceResult());
	}

	// @Test
	public void broadcast() {
		RequestInvokeObject invokeObj = new RequestInvokeObject("say");
		invokeObj.putParam("name", "weforward");
		Request req = invoker.createRequest(invokeObj.toDtObject());
		req.getHeader().setChannel(Header.CHANNEL_NOTIFY);
		req.setMarks(Request.MARK_NOTIFY_BROADCAST);
		Response resp = invoker.invoke(req);
		System.out.println(resp.getResponseCode() + "/" + resp.getResponseMsg());
		System.out.println(resp.getResourceUrl());
		System.out.println(resp.getServiceResult());
	}

	// @Test
	public void pressure() {
		StringBuilder sb = new StringBuilder(64 * 1024);
		Random rd = new Random();
		for (int i = 0; i < 10000; i++) {
			sb.append(rd.nextInt());
		}
		String arg1 = sb.toString();
		System.out.println(arg1.length());
		int threadCount = 250;
		int round = 20;
		AtomicInteger success = new AtomicInteger();
		AtomicInteger fail = new AtomicInteger();
		AtomicInteger threadRemaind = new AtomicInteger(threadCount);
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < threadCount; i++) {
			new Thread(String.valueOf(i)) {
				public void run() {
					int _round = round;
					while (_round-- > 0) {
						RequestInvokeObject invokeObj = new RequestInvokeObject("pressure");
						invokeObj.putParam("arg1", arg1);
						Request req = invoker.createRequest(invokeObj.toDtObject());
						// req.setWaitTimeout(10);
						Response resp;
						try {
							resp = invoker.invoke(req);
						} catch (Exception e) {
							e.printStackTrace();
							fail.incrementAndGet();
							continue;
						}
						if (0 != resp.getResponseCode()) {
							fail.incrementAndGet();
							continue;
						}
						FriendlyObject result = FriendlyObject.valueOf(resp.getServiceResult());
						if (0 != result.getInt("code")) {
							fail.incrementAndGet();
							continue;
						}
						if (StringUtil.toString(result.getString("content")).length() == arg1.length()) {
							success.incrementAndGet();
						} else {
							fail.incrementAndGet();
						}

						if (_round % 3 == 0) {
							long use = (System.currentTimeMillis() - startTime);
							System.out.println("s:" + success.get() + ",f:" + fail + ",use:" + use + ",tps:"
									+ (success.get() * 1000 / use));
						}
					}

					threadRemaind.decrementAndGet();
					synchronized (SayHelloClient.this) {
						SayHelloClient.this.notify();
					}
				};
			}.start();
		}

		while (true) {
			if (threadRemaind.get() > 0) {
				synchronized (this) {
					try {
						wait(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				continue;
			}
			break;
		}
		long use = (System.currentTimeMillis() - startTime);
		System.out
				.println("s:" + success.get() + ",f:" + fail + ",use:" + use + ",tps:" + (success.get() * 1000 / use));
	}

	// @Test
	public void upload() {
		RequestInvokeObject invokeObj = new RequestInvokeObject("upload");
		Request req = invoker.createRequest(invokeObj.toDtObject());
		// req.setWaitTimeout(10);
		Response resp = invoker.invoke(req);
		System.out.println(resp.getResponseCode() + "/" + resp.getResponseMsg());
		System.out.println(resp.getResourceUrl());
		System.out.println(resp.getServiceResult());
	}
}
