/**
 * 
 */
package io.nettyspringmvc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;

import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StreamUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

/**
 * @author houkx
 *
 */
class HttpRequestImpl extends MockHttpServletRequest {
	private final ByteBuf content;
	private static final ServletInputStream EMPTY_SERVLET_INPUT_STREAM = new DelegatingServletInputStream(
			StreamUtils.emptyInput());
	private static final BufferedReader EMPTY_BUFFERED_READER = new BufferedReader(new StringReader(""));

	public HttpRequestImpl(ServletContext servletContext, ByteBuf content) {
		super(servletContext);
		this.content = content;
	}

	@Override
	public int getContentLength() {
		return (this.content != null ? this.content.readableBytes() : -1);
	}

	@Override
	public ServletInputStream getInputStream() {
		if (this.content != null) {
			return new DelegatingServletInputStream(new ByteBufInputStream(this.content));
		} else {
			return EMPTY_SERVLET_INPUT_STREAM;
		}
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return new RequestDispatcherImpl(path);
	}

	@Override
	public BufferedReader getReader() throws UnsupportedEncodingException {
		if (this.content != null) {
			InputStream sourceStream = new ByteBufInputStream(this.content);
			String characterEncoding = getCharacterEncoding();
			Reader sourceReader = (characterEncoding != null) ? //
					new InputStreamReader(sourceStream, characterEncoding) //
					: new InputStreamReader(sourceStream);//
			return new BufferedReader(sourceReader);
		} else {
			return EMPTY_BUFFERED_READER;
		}
	}
}
