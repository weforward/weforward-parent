package cn.weforward.protocol.aio.netty.websocket;

import cn.weforward.protocol.aio.http.HttpHeaders;

/**
 * Websocket下的请求封装
 * 
 * @author liangyi
 *
 */
public class WebSocketRequest extends WebSocketMessage {

	public WebSocketRequest(WebSocketSession invoke, HttpHeaders headers) {
		super(invoke, headers);
		// TODO Auto-generated constructor stub
	}

}
