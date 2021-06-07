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
package cn.weforward.aio;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import cn.weforward.common.io.CachedInputStream;
import cn.weforward.protocol.aio.ClientContext;
import cn.weforward.protocol.aio.ClientHandler;
import cn.weforward.protocol.aio.ServerContext;
import cn.weforward.protocol.aio.ServerHandler;
import cn.weforward.protocol.aio.ServerHandlerFactory;
import cn.weforward.protocol.aio.netty.NettyHttpServer;
import cn.weforward.protocol.aio.netty.NettyWebsocketFactory;
import cn.weforward.protocol.aio.netty.websocket.WebSocketChannel;

/**
 * 测试 NettyWebsocket
 * 
 * @see NettyHttpServer
 * @author liangyi
 *
 */
public class Test_NettyWebsocket {
	NettyWebsocketFactory factory;
	WebSocketChannel channel;

	Test_NettyWebsocket() {
		factory = new NettyWebsocketFactory();
	}

	void connect(String url) throws IOException {
		System.out.println("conecting: " + url);
		factory.connect(new ServerHandlerFactory() {
			@Override
			public ServerHandler handle(ServerContext context) throws IOException {
				return new Service(context);
			}
		}, url, new NettyWebsocketFactory.Keepalive(5) {
			@Override
			public void establish(Closeable context) {
				System.out.println("establish:" + context);
				channel = (WebSocketChannel) context;
			}
		});
	}

	public static void main(String args[]) throws Exception {
		Test_NettyWebsocket test = new Test_NettyWebsocket();
		test.factory.setSsl(true);

		System.out.println("输入（q 退出，c 连接ws://127.0.0.1:8080/，p POST测试，t 多次调用测试，其它为连接的URL）：");
		String cmd;
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		for (;;) {
			cmd = reader.readLine();
			if (cmd.startsWith("ws://") || cmd.startsWith("wss://")) {
				test.connect(cmd);
				continue;
			}
			if (1 != cmd.length()) {
				continue;
			}

			char ch = cmd.charAt(0);
			if ('q' == ch) {
				break;
			} else if ('c' == ch) {
				test.connect("ws://127.0.0.1:8080/");
			} else if ('r' == ch) {
				// 请求测试
				Client c = new Client();
				ClientContext cc = test.channel.request(c, "/mymethod");
				c.setContext(cc);
				cc.setRequestHeader("Content-Type", "application/json");
				OutputStream out = cc.openRequestWriter();
				out.write("{\"ask\":\"who are you\"}".getBytes("UTF-8"));
				out.close();
			} else if ('t' == ch) {
			}
		}
		test.factory.close();
		System.out.println("done.");
	}

	static class Client implements ClientHandler {
		ClientContext m_Context;

		@Override
		public void connectFail() {
			System.out.println("connectFail " + m_Context);
		}

		public void setContext(ClientContext cc) {
			m_Context = cc;
		}

		@Override
		public void established(ClientContext context) {
			System.out.println("established " + m_Context);
		}

		@Override
		public void requestCompleted() {
			System.out.println("requestCompleted " + m_Context);
		}

		@Override
		public void requestAbort() {
			System.out.println("requestAbort " + m_Context);
		}

		@Override
		public void responseHeader() {
			System.out.println("responseHeader " + m_Context);
		}

		@Override
		public void prepared(int available) {
			System.out.println("prepared " + available);
		}

		@Override
		public void responseCompleted() {
			System.out.println("responseCompleted " + m_Context);
			try (InputStream in = m_Context.getResponseStream()) {
				System.out.println(m_Context.getResponseHeaders());
				System.out.print("msg:");
				System.out.println(CachedInputStream.readString(in, 0, null));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void responseTimeout() {
			System.out.println("responseTimeout " + m_Context);
		}

		@Override
		public void errorResponseTransferTo(IOException e, Object msg, OutputStream writer) {
			System.err.println("error " + m_Context);
			if (null != e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 服务
	 */
	static class Service implements ServerHandler {
		ServerContext context;

		Service(ServerContext context) {
			this.context = context;
		}

		@Override
		public void requestHeader() {
			System.out.println("requestHeader " + context);
		}

		@Override
		public void prepared(int available) {
			System.out.println("prepared[" + available + "] " + context);
		}

		@Override
		public void requestAbort() {
			System.out.println("requestAbort " + context);
		}

		@Override
		public void requestCompleted() {
			System.out.println("requestCompleted " + context);
		}

		@Override
		public void responseTimeout() {
			System.out.println("responseTimeout " + context);
		}

		@Override
		public void responseCompleted() {
			System.out.println("responseCompleted " + context);
		}

		@Override
		public void errorRequestTransferTo(IOException e, Object msg, OutputStream writer) {
			System.err.println("err " + e + " " + context);
			if (null != e) {
				e.printStackTrace();
			}
		}
	}

}
