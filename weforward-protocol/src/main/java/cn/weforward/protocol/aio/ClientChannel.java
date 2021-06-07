package cn.weforward.protocol.aio;

import java.io.Closeable;
import java.io.IOException;

/**
 * 客户端发起的通道封装
 * 
 * @author liangcha
 *
 */
public interface ClientChannel extends Closeable {
	/**
	 * 发起调用请求
	 * 
	 * @param handler 客户端处理器
	 * @param uri     请求路径
	 * @param verb    动作
	 * @return 客户端上下文
	 * @throws IOException
	 */
	ClientContext request(ClientHandler handler, String uri, String verb) throws IOException;

	/**
	 * 关闭通道
	 */
	void close();
}
