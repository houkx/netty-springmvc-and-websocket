package io.nettyspringmvc;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.nettyspringmvc.api.ServerDef;
import io.nettyspringmvc.api.WsServerEndPoint;

/**
 * 
 * @author Houkx
 *
 */
class ServletWithWsNettyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private static Logger logger = LoggerFactory.getLogger(ServletWithWsNettyHandler.class);
	private final Servlet servlet;

	private final ServletContext servletContext;
	private final ServerDef serverDef;
	private static final NullWsServerEndPoint NULL = new NullWsServerEndPoint();

	ServletWithWsNettyHandler(ServerDef serverDef, Servlet servlet) {
		this.servlet = servlet;
		this.servletContext = servlet.getServletConfig().getServletContext();
		this.serverDef = serverDef;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

		if (!request.decoderResult().isSuccess()) {
			sendError(ctx, BAD_REQUEST);
			return;
		}
		if ("websocket".equalsIgnoreCase((String) request.headers().get("Upgrade"))) {
			if (serverDef.serverEndPoints != null) {
				handleWebSocket(ctx, request);
			} else {
				throw new IllegalAccessException("websocket not support! serverDef.serverEndPoints must not be null!");
			}
			return;
		}
		MockHttpServletRequest servletRequest = createServletRequest(request);
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();

		this.servlet.service(servletRequest, servletResponse);

		HttpResponseStatus status = HttpResponseStatus.valueOf(servletResponse.getStatus());
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
		HttpHeaders respHeaders = response.headers();
		for (String name : servletResponse.getHeaderNames()) {
			respHeaders.add(name, servletResponse.getHeaderValues(name));
		}

		// Write the initial line and the header.
		ctx.write(response);

		InputStream contentStream = new ByteArrayInputStream(servletResponse.getContentAsByteArray());

		// Write the content.
		ChannelFuture writeFuture = ctx.writeAndFlush(new ChunkedStream(contentStream));
		writeFuture.addListener(ChannelFutureListener.CLOSE);
	}

	private MockHttpServletRequest createServletRequest(FullHttpRequest httpRequest) {
		UriComponents uriComponents = UriComponentsBuilder.fromUriString(httpRequest.uri()).build();

		HttpRequestImpl servletRequest = new HttpRequestImpl(servletContext, httpRequest.content());
		servletRequest.setRequestURI(uriComponents.getPath());
		servletRequest.setPathInfo(uriComponents.getPath());
		servletRequest.setMethod(httpRequest.method().name());

		if (uriComponents.getScheme() != null) {
			servletRequest.setScheme(uriComponents.getScheme());
		}
		if (uriComponents.getHost() != null) {
			servletRequest.setServerName(uriComponents.getHost());
		}
		if (uriComponents.getPort() != -1) {
			servletRequest.setServerPort(uriComponents.getPort());
		}
		HttpHeaders headers = httpRequest.headers();
		for (String name : headers.names()) {
			servletRequest.addHeader(name, headers.getAll(name));
		}

		try {
			if (uriComponents.getQuery() != null) {
				String query = UriUtils.decode(uriComponents.getQuery(), "UTF-8");
				servletRequest.setQueryString(query);
			}

			for (Entry<String, List<String>> entry : uriComponents.getQueryParams().entrySet()) {
				for (String value : entry.getValue()) {
					servletRequest.addParameter(UriUtils.decode(entry.getKey(), "UTF-8"),
							UriUtils.decode(value, "UTF-8"));
				}
			}
		} catch (UnsupportedEncodingException ex) {
			// shouldn't happen
		}

		return servletRequest;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		if (ctx.channel().isActive()) {
			sendError(ctx, INTERNAL_SERVER_ERROR);
		}
	}

	private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		ByteBuf content = Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8);
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, content);
		response.headers().add(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
		// Close the connection as soon as the error message is sent.
		ctx.write(response).addListener(ChannelFutureListener.CLOSE);
	}

	/**
	 * handle WebSocket request,then, the the RPC could happen in WebSocket.
	 * 
	 * @param ctx
	 * @param request
	 */
	private void handleWebSocket(final ChannelHandlerContext ctx, FullHttpRequest request) {
		logger.debug("handleWebSocket request: uri={}", request.uri());
		// Handshake
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(request.uri(), null, true);
		WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(request);
		if (handshaker == null) {
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
			return;
		}
		ChannelFutureListener callback = websocketHandshakeListener(ctx, request);
		ChannelFuture future = handshaker.handshake(ctx.channel(), request);
		if (callback != null) {
			future.addListener(callback);
		}
		ChannelPipeline pipe = ctx.pipeline();
		if (pipe.get(WebsocketFrameHandler.class) == null) {
			pipe.addAfter(ctx.name(), "wsFrameHandler",
					new WebsocketFrameHandler(getWsEndpointByUri(request.uri()), handshaker));
			pipe.remove(ctx.name());// Remove current Handler
		}
	}

	private static final AttributeKey<Session> KEY_SESSION = AttributeKey.valueOf("#SESSION");

	private static Session getSession(ChannelHandlerContext ctx) {
		return ctx.channel().attr(KEY_SESSION).get();
	}

	private WsServerEndPoint getWsEndpointByUri(String uri) {
		int qi = uri.indexOf('?');
		if (qi > 0) {
			uri = uri.substring(0, qi);
		}
		WsServerEndPoint rs = serverDef.serverEndPoints.get(uri);
		if (rs == null) {
			rs = NULL;
		}
		return rs;
	}

	protected ChannelFutureListener websocketHandshakeListener(final ChannelHandlerContext ctx,
			final FullHttpRequest request) {
		return new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Attribute<Session> sessionHolder = ctx.channel().attr(KEY_SESSION);
				Session session = sessionHolder.get();
				WsServerEndPoint ep = getWsEndpointByUri(request.uri());
				if (session == null) {
					session = new SessionImpl(ctx, request, ep);
					Session orig = sessionHolder.setIfAbsent(session);
					if (orig != null) {
						session = orig;
					}
				}
				if (future.isSuccess()) {
					ep.onOpen(session);
				} else if (!future.isCancelled()) {
					ep.onError(session, new RuntimeException("fail to HandShake"));
				}
			}
		};
	}

	private static class WebsocketFrameHandler extends MessageToMessageDecoder<WebSocketFrame> {
		final WebSocketServerHandshaker handshaker;
		final WsServerEndPoint wsServerEndPoint;

		WebsocketFrameHandler(WsServerEndPoint wsServerEndPoint, WebSocketServerHandshaker handshaker) {
			this.handshaker = handshaker;
			this.wsServerEndPoint = wsServerEndPoint;
		}

		@Override
		protected void decode(ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out) throws Exception {
			if (frame instanceof CloseWebSocketFrame) {
				handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
				return;
			}
			if (frame instanceof PingWebSocketFrame) {
				out.add(new PongWebSocketFrame(frame.content().retain()));
				return;
			}
			if (frame instanceof PongWebSocketFrame) {
				out.add(new PongWebSocketFrame(frame.content().retain()));
				return;
			}
			if (!(frame instanceof TextWebSocketFrame)) {
				throw new UnsupportedOperationException(
						String.format(" frame type '%s' not supported", frame.getClass().getName()));
			}
			// extract the message
			String message = ((TextWebSocketFrame) frame).text();
			logger.debug("ws '{}' received: {}", ctx.channel(), message);
			wsServerEndPoint.onMessage(message, getSession(ctx));
		}

	}
}
