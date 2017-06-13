/**
 * 
 */
package io.nettyspringmvc;

import java.io.IOException;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

public class ChatRoom {
	/**
	 * 连接建立成功调用的方法
	 * 
	 * @param session
	 *            可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
	 */
	@OnOpen
	public void onOpen(Session session) {
		System.out.println("有新连接加入！session=" + session);
	}

	/**
	 * 收到客户端消息后调用的方法
	 * 
	 * @param message
	 *            客户端发送过来的消息
	 * @param session
	 *            可选的参数
	 */
	@OnMessage
	public void onMessage(String message, Session session) {
		System.out.println("来自客户端的消息:" + message);
		try {
			String msg = "来自Server的消息:" + message.toUpperCase();
			session.getBasicRemote().sendText(msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 发生错误时调用
	 * 
	 * @param session
	 * @param error
	 */
	@OnError
	public void on_error() {
		System.out.println("发生错误");
	}
//
//	/**
//	 * 连接关闭调用的方法
//	 */
//	@OnClose
//	public void onClose(Session session) {
//		System.out.println("有一连接关闭！session=" + session);
//	}
}
