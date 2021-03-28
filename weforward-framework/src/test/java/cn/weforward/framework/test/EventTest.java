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

import org.junit.Before;
import org.junit.Test;

import cn.weforward.protocol.Header;
import cn.weforward.protocol.Request;
import cn.weforward.protocol.Response;
import cn.weforward.protocol.client.ServiceInvoker;
import cn.weforward.protocol.client.ServiceInvokerFactory;
import cn.weforward.protocol.client.ext.RequestInvokeObject;

public class EventTest {

	String gateWayUrl = "http://127.0.0.1:5661/";
	String accessId = "H-0947f4f50120-0947f4f50120";
	String accessKey = "0abdc16ef73b3a02f31b5cc7b67467cfb02e30fb4a32bc9f2e08d12e80f6ee54";

	ServiceInvoker invoker;

	@Before
	public void init() {
		invoker = ServiceInvokerFactory.create("say_hello", gateWayUrl, accessId, accessKey);
	}

	@Test
	public void roundRobin() {
		System.out.println("-----------roundRobin------------");
		RequestInvokeObject invokeObj = new RequestInvokeObject("say");
		invokeObj.putParam("name", "weforward");
		Request req = invoker.createRequest(invokeObj.toDtObject());
		req.getHeader().setChannel(Header.CHANNEL_NOTIFY);
		// req.setMarks(Request.MARK_NOTIFY_BROADCAST);
		// req.setWaitTimeout(10);
		Response resp = invoker.invoke(req);
		System.out.println(resp.getResponseCode() + "/" + resp.getResponseMsg());
		System.out.println(resp.getNotifyReceives());
		System.out.println(resp.getServiceResult());
	}

	@Test
	public void broadcast() {
		System.out.println("-----------broadcast------------");
		RequestInvokeObject invokeObj = new RequestInvokeObject("say");
		invokeObj.putParam("name", "weforward");
		Request req = invoker.createRequest(invokeObj.toDtObject());
		req.getHeader().setChannel(Header.CHANNEL_NOTIFY);
		req.setMarks(Request.MARK_NOTIFY_BROADCAST);
		// req.setWaitTimeout(10);
		Response resp = invoker.invoke(req);
		System.out.println(resp.getResponseCode() + "/" + resp.getResponseMsg());
		System.out.println(resp.getNotifyReceives());
		System.out.println(resp.getServiceResult());
	}
}
