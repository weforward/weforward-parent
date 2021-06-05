package cn.weforward.protocol.aio.netty.websocket;

import java.io.Closeable;
import java.io.IOException;

import cn.weforward.protocol.aio.ClientContext;
import cn.weforward.protocol.aio.ClientHandler;

/**
 * WebSocket通道封装
 * 
 * @author liangcha
 *
 */
public interface WebSocketChannel extends Closeable {
	/**
	 * 发起调用请求
	 * 
	 * @param handler 客户端处理器
	 * @param uri     请求路径
	 * @return 客户端上下文
	 * @throws IOException
	 */
	ClientContext request(ClientHandler handler, String uri) throws IOException;

	/**
	 * 关闭通道
	 */
	void close();
}
