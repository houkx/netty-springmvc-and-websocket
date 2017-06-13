package io.nettyspringmvc;

import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.websocket.server.ServerEndpoint;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.nettyspringmvc.api.ServerDef;
import io.nettyspringmvc.api.WsServerEndPoint;

class NettySpringmvcInitializer extends ChannelInitializer<SocketChannel> {

	private final ServerDef serverDef;
	private final Servlet dispatcherServlet;

	NettySpringmvcInitializer(final ServerDef serverDef, String... basePackages) throws ServletException {
		this.serverDef = serverDef;
		MockServletContext servletContext = new MockServletContext();
		MockServletConfig servletConfig = new MockServletConfig(servletContext);

		final AnnotationConfigWebApplicationContext wac = new AnnotationConfigWebApplicationContext();
		// XmlWebApplicationContext wac = new XmlWebApplicationContext();

		// ClassPathXmlApplicationContext wac = new
		// ClassPathXmlApplicationContext();
		wac.setServletContext(servletContext);
		wac.setServletConfig(servletConfig);
		// wac.setConfigLocation("classpath:/servlet-context.xml");
		// wac.setConfigLocations(configLocations);
		// wac.register(WebConfig.class);
		if (basePackages != null && basePackages.length > 0) {
			wac.scan(basePackages);
		}
		wac.addApplicationListener(new ApplicationListener<ContextRefreshedEvent>() {
			@Override
			public void onApplicationEvent(ContextRefreshedEvent paramE) {
				if (serverDef.businessInitFinishedCallback != null) {
					serverDef.businessInitFinishedCallback.run();
				}
				if (serverDef.serverEndPoints != null) {
					String[] wsBeanNames = wac.getBeanNamesForAnnotation(ServerEndpoint.class);
					if (wsBeanNames != null && wsBeanNames.length > 0) {
						Object[] wsBeans = new Object[wsBeanNames.length];
						for (int i = 0; i < wsBeanNames.length; i++) {
							String name = wsBeanNames[i];
							wsBeans[i] = wac.getBean(name);
						}
						Map<String, WsServerEndPoint> map = AnnotaionedWsHandlerParser.serverEndPoints(wsBeans);
						System.out.println("ws map = " + map);
						if (map != null && map.size() > 0) {
							serverDef.serverEndPoints.putAll(map);
						}
					}
				}
			}
		});
		wac.refresh();

		DispatcherServlet dispatcherServlet = new DispatcherServlet(wac);
		dispatcherServlet.init(servletConfig);
		this.dispatcherServlet = dispatcherServlet;
	}

	@Override
	public void initChannel(SocketChannel channel) throws Exception {
		// Create a default pipeline implementation.
		ChannelPipeline pipeline = channel.pipeline();

		// Uncomment the following line if you want HTTPS
		// SSLEngine engine =
		// SecureChatSslContextFactory.getServerContext().createSSLEngine();
		// engine.setUseClientMode(false);
		// pipeline.addLast("ssl", new SslHandler(engine));

		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		pipeline.addLast("handler", new ServletWithWsNettyHandler(serverDef, dispatcherServlet));
	}

	// @Configuration
	// @EnableWebMvc
	// @ComponentScan(basePackages = { "com.mq.webapi_boot" })
	// static class WebConfig extends WebMvcConfigurerAdapter {
	// }

}
