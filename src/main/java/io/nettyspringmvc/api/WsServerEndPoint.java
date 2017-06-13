/**
 * 
 */
package io.nettyspringmvc.api;

import javax.websocket.Session;

/**
 * Websocket 处理类（由外部程序实现）
 * 
 * @author Houkx
 *
 */
public interface WsServerEndPoint {

	void onMessage(String message, Session session);

	void onOpen(Session session);

	void onClose(Session session);

	void onError(Session session, Throwable thr);
}
