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
import java.io.InputStreamReader;
import java.io.OutputStream;

import cn.weforward.protocol.aio.ClientContext;
import cn.weforward.protocol.aio.ClientHandler;
import cn.weforward.protocol.aio.ConnectionListener;
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

	WebSocketChannel connect(String url) throws IOException {
		System.out.println("conecting: " + url);
		channel = factory.connect(new ServerHandlerFactory() {
			@Override
			public ServerHandler handle(ServerContext context) throws IOException {
				return new Service(context);
			}
		}, url, new ConnectionListener() {

			@Override
			public void lost(Closeable context) {
				System.out.println("lost:" + context);
			}

			@Override
			public void fail(String url, Throwable cause) {
				System.out.println("fail:" + url);
				if (null != cause) {
					cause.printStackTrace();
				}
			}

			@Override
			public void establish(Closeable context) {
				System.out.println("establish:" + context);
			}
		});
		return channel;
	}

	public static void main(String args[]) throws Exception {
		Test_NettyWebsocket test = new Test_NettyWebsocket();
		test.factory.setSsl(true);

		String cmd;
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		for (;;) {
			System.out.println("输入（q 退出，c 连接ws://127.0.0.1:8080/，p POST测试，t 多次调用测试，其它为连接的URL）：");
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
				ClientContext cc = test.channel.request(new Client(), "/mymethod");
			} else if ('t' == ch) {
			}
		}
		test.factory.close();
		System.out.println("done.");
	}

	static class Client implements ClientHandler {
		@Override
		public void connectFail() {
			// TODO Auto-generated method stub

		}

		@Override
		public void established() {
			// TODO Auto-generated method stub

		}

		@Override
		public void requestCompleted() {
			// TODO Auto-generated method stub

		}

		@Override
		public void requestAbort() {
			// TODO Auto-generated method stub

		}

		@Override
		public void responseHeader() {
			// TODO Auto-generated method stub

		}

		@Override
		public void prepared(int available) {
			// TODO Auto-generated method stub

		}

		@Override
		public void responseCompleted() {
			// TODO Auto-generated method stub

		}

		@Override
		public void responseTimeout() {
			// TODO Auto-generated method stub

		}

		@Override
		public void errorResponseTransferTo(IOException e, Object msg, OutputStream writer) {
			// TODO Auto-generated method stub
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
			System.err.println("requestHeader " + context);
		}

		@Override
		public void prepared(int available) {
			System.err.println("prepared[" + available + "] " + context);
		}

		@Override
		public void requestAbort() {
			System.err.println("requestAbort " + context);
		}

		@Override
		public void requestCompleted() {
			System.err.println("requestCompleted " + context);
		}

		@Override
		public void responseTimeout() {
			System.err.println("responseTimeout " + context);
		}

		@Override
		public void responseCompleted() {
			System.err.println("responseCompleted " + context);
		}

		@Override
		public void errorRequestTransferTo(IOException e, Object msg, OutputStream writer) {
			System.err.println("err " + e + " " + context);
		}
	}

}
