package cn.weforward.protocol.aio.netty.websocket;

import cn.weforward.protocol.aio.netty.NettyHttpHeaders;

/**
 * Websocket下的响应封装
 * 
 * @author liangyi
 *
 */
public class WebSocketResponse extends WebSocketMessage {

	public WebSocketResponse(WebSocketSession session, NettyHttpHeaders headers) {
		super(session, headers);
	}
}
