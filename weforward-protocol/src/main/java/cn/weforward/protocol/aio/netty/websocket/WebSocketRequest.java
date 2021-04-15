package cn.weforward.protocol.aio.netty.websocket;

import cn.weforward.protocol.aio.netty.NettyHttpHeaders;

/**
 * Websocket下的请求封装
 * 
 * @author liangyi
 *
 */
public class WebSocketRequest extends WebSocketMessage {

	public WebSocketRequest(WebSocketSession session, NettyHttpHeaders headers) {
		super(session, headers);
	}

}
