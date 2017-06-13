/**
 * 
 */
package io.nettyspringmvc;

import java.io.FileNotFoundException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockRequestDispatcher;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * @author Houkx
 *
 */
class RequestDispatcherImpl extends MockRequestDispatcher {
	private final String resource;
	private final Log logger = LogFactory.getLog(getClass());

	public RequestDispatcherImpl(String resource) {
		super(resource);
		this.resource = resource;
	}

	@Override
	public void forward(ServletRequest request, ServletResponse response) {
		Assert.notNull(request, "Request must not be null");
		Assert.notNull(response, "Response must not be null");
		if (response.isCommitted()) {
			throw new IllegalStateException("Cannot perform forward - response is already committed");
		}
		MockHttpServletResponse resp = getMockHttpServletResponse(response);
		resp.setForwardedUrl(this.resource);
		// TODO cache for static file
		PathMatchingResourcePatternResolver resl = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
		try {
			Resource res = resl
					.getResource("classpath:static" + (resource.charAt(0) == '/' ? resource : '/' + resource));
			if (res != null) {
				StreamUtils.copy(res.getInputStream(), resp.getOutputStream());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("MockRequestDispatcher: forwarding to [" + this.resource + "]");
		}
	}
}
