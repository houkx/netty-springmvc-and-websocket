/**
 * 
 */
package io.nettyspringmvc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;

import javax.websocket.EncodeException;
import javax.websocket.RemoteEndpoint.Basic;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * @author Houkx
 *
 */
class BasicRemoteImpl implements Basic {
	private final ChannelHandlerContext ctx;
	private boolean batchAllowed;

	public BasicRemoteImpl(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.websocket.RemoteEndpoint.Basic#sendText(java.lang.String)
	 */
	@Override
	public void sendText(String text) throws IOException {
		writeBuf(new TextWebSocketFrame(text));
	}

	private void writeBuf(Object buf) {
		sendObj(buf, batchAllowed);
	}

	@Override
	public void sendText(String partialMessage, boolean isLast) throws IOException {
		sendObj(new TextWebSocketFrame(partialMessage), isLast);
	}

	@Override
	public void flushBatch() throws IOException {
		if (batchAllowed) {
			ctx.flush();
		}
	}

	@Override
	public void setBatchingAllowed(boolean allowed) throws IOException {
		batchAllowed = allowed;
	}

	@Override
	public boolean getBatchingAllowed() {
		return batchAllowed;
	}

	@Override
	public void sendPing(ByteBuffer applicationData) throws IOException, IllegalArgumentException {
		writeBuf(new PingWebSocketFrame(Unpooled.copiedBuffer(applicationData)));
	}

	@Override
	public void sendPong(ByteBuffer applicationData) throws IOException, IllegalArgumentException {
		writeBuf(new PongWebSocketFrame(Unpooled.copiedBuffer(applicationData)));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.websocket.RemoteEndpoint.Basic#sendBinary(java.nio.ByteBuffer)
	 */
	@Override
	public void sendBinary(ByteBuffer data) throws IOException {
		writeBuf(new TextWebSocketFrame(Unpooled.copiedBuffer(data)));
	}

	@Override
	public void sendBinary(ByteBuffer partialByte, boolean isLast) throws IOException {
		sendObj(new TextWebSocketFrame(Unpooled.copiedBuffer(partialByte)), isLast);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.websocket.RemoteEndpoint.Basic#sendObject(java.lang.Object)
	 */
	@Override
	public void sendObject(Object data) throws IOException, EncodeException {
		if (data instanceof ByteBuffer) {
			sendBinary((ByteBuffer) data);
		} else {
			sendText(data.toString());
		}
	}

	private void sendObj(Object buf, boolean refresh) {
		if (refresh) {
			ctx.write(buf).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
		} else {
			ctx.writeAndFlush(buf).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.websocket.RemoteEndpoint.Basic#getSendStream()
	 */
	@Override
	public OutputStream getSendStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.websocket.RemoteEndpoint.Basic#getSendWriter()
	 */
	@Override
	public Writer getSendWriter() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
