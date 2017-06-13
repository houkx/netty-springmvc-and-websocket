/**
 * 
 */
package io.nettyspringmvc;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.MessageHandler.Partial;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.nettyspringmvc.api.WsServerEndPoint;

/**
 * @author Houkx
 *
 */
class SessionImpl implements Session {
	private final ChannelHandlerContext ctx;
	private final FullHttpRequest request;
	private final WsServerEndPoint serverEndPoint;
	private final BasicRemoteImpl basicRemoteImpl;
	private final Map<String, Object> headers;

	SessionImpl(ChannelHandlerContext ctx, FullHttpRequest request, WsServerEndPoint serverEndPoint) {
		this.ctx = ctx;
		this.request = request;
		this.serverEndPoint = serverEndPoint;
		basicRemoteImpl = new BasicRemoteImpl(ctx);
		if (request != null)
			headers = new HttpHeaderMap(request.headers());
		else
			headers = Collections.emptyMap();
	}

	@Override
	public String getId() {
		return ctx.channel().id().asShortText();
	}

	public void sendMessage(String message) {
		ctx.writeAndFlush(new TextWebSocketFrame(message));
	}

	@Override
	public String getQueryString() {
		String queryStr = request.uri();
		if (queryStr != null) {
			queryStr = queryStr.trim();
			if (queryStr.length() > 0 && queryStr.charAt(0) == '/') {
				if (queryStr.length() == 1) {
					queryStr = "";
				} else {
					int wh = queryStr.indexOf('?');
					if (wh > 0) {
						queryStr = queryStr.substring(wh + 1);
					} else {
						queryStr = queryStr.substring(1);
					}
				}
			}
		} else {
			queryStr = "";
		}
		return queryStr;
	}

	@Override
	public URI getRequestURI() {
		return URI.create(request.uri());
	}

	@Override
	public Map<String, Object> getUserProperties() {
		return headers;
	}

	@Override
	public boolean isOpen() {
		return ctx.channel().isOpen();
	}

	@Override
	public void close() throws IOException {
		ctx.close().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					serverEndPoint.onClose(SessionImpl.this);
				}
			}
		});
	}

	@Override
	public void close(CloseReason closeReason) throws IOException {
		close();
	}

	@Override
	public Async getAsyncRemote() {
		// TODO getAsyncRemote()
		return null;
	}

	@Override
	public Basic getBasicRemote() {
		return basicRemoteImpl;
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO getUserPrincipal() --可能有用
		return null;
	}

	@Override
	public void addMessageHandler(MessageHandler handler) throws IllegalStateException {
	}

	@Override
	public <T> void addMessageHandler(Class<T> clazz, Whole<T> handler) {
	}

	@Override
	public <T> void addMessageHandler(Class<T> clazz, Partial<T> handler) {
	}

	@Override
	public WebSocketContainer getContainer() {
		// TODO getContainer()
		return null;
	}

	@Override
	public int getMaxBinaryMessageBufferSize() {
		return 0;
	}

	@Override
	public long getMaxIdleTimeout() {
		return 0;
	}

	@Override
	public int getMaxTextMessageBufferSize() {
		return 0;
	}

	@Override
	public Set<MessageHandler> getMessageHandlers() {
		return null;
	}

	@Override
	public List<Extension> getNegotiatedExtensions() {
		return null;
	}

	@Override
	public String getNegotiatedSubprotocol() {
		return null;
	}

	@Override
	public Set<Session> getOpenSessions() {
		return null;
	}

	@Override
	public Map<String, String> getPathParameters() {
		return null;
	}

	@Override
	public String getProtocolVersion() {
		return null;
	}

	@Override
	public Map<String, List<String>> getRequestParameterMap() {
		return null;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public void removeMessageHandler(MessageHandler handler) {

	}

	@Override
	public void setMaxBinaryMessageBufferSize(int length) {

	}

	@Override
	public void setMaxIdleTimeout(long milliseconds) {
	}

	@Override
	public void setMaxTextMessageBufferSize(int length) {

	}

}

class HttpHeaderMap extends AbstractMap<String, Object> {
	private final HttpHeaders header;

	public HttpHeaderMap(HttpHeaders header) {
		this.header = header;
	}

	public Object get(Object key) {
		if (key != null) {
			return header.getAll(key.toString());
		}
		return null;
	}

	@Override
	public Object put(String key, Object value) {
		return header.set(key, value);
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return null;
	}

}