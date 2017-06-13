/**
 * 
 */
package io.nettyspringmvc;

import javax.websocket.Session;

import com.app.demo.ws.WebSocketDemo;

import io.nettyspringmvc.api.WsServerEndPoint;

/**
 * @author Houkx
 *
 */
public class WsServerEndPoint_Chat implements WsServerEndPoint {
	private final WebSocketDemo target;

	public WsServerEndPoint_Chat(WebSocketDemo target) {
		this.target = target;
	}

	@Override
	public void onMessage(String message, Session session) {
		target.onMessage(message, session);
	}

	@Override
	public void onOpen(Session session) {
		target.onOpen(session);
	}

	@Override
	public void onClose(Session session) {
		target.onClose(session);
	}

	@Override
	public void onError(Session session, Throwable error) {
		target.onError(session, error);
	}

}
