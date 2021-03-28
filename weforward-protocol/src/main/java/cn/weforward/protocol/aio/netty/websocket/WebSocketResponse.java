package cn.weforward.protocol.aio.netty.websocket;

import cn.weforward.protocol.aio.http.HttpHeaders;

/**
 * Websocket下的响应封装
 * 
 * @author liangyi
 *
 */
public class WebSocketResponse extends WebSocketMessage {

	public WebSocketResponse(WebSocketSession invoke, HttpHeaders headers) {
		super(invoke, headers);
		// TODO Auto-generated constructor stub
	}

}
