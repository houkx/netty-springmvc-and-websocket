/**
 * 
 */
package io.nettyspringmvc;

import javax.websocket.Session;

import io.nettyspringmvc.api.WsServerEndPoint;

/**
 * @author Houkx
 *
 */
class NullWsServerEndPoint implements WsServerEndPoint {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.mq.nettyspringmvc.api.WsServerEndPoint#onMessage(java.lang.String,
	 * javax.websocket.Session)
	 */
	@Override
	public void onMessage(String message, Session session) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mq.nettyspringmvc.api.WsServerEndPoint#onOpen(javax.websocket.
	 * Session)
	 */
	@Override
	public void onOpen(Session session) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mq.nettyspringmvc.api.WsServerEndPoint#onClose(javax.websocket.
	 * Session)
	 */
	@Override
	public void onClose(Session session) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mq.nettyspringmvc.api.WsServerEndPoint#onError(javax.websocket.
	 * Session, java.lang.Throwable)
	 */
	@Override
	public void onError(Session session, Throwable thr) {
		// TODO Auto-generated method stub

	}

}
