package io.nettyspringmvc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.nettyspringmvc.api.ServerDef;

/**
 * 
 * @author Houkx
 *
 */
public class ServerBootstap {
	private EventLoopGroup group;

	/**
	 * 启动服务
	 * 
	 * @param def
	 *            - 服务定义
	 * @param springScanPkgs
	 *            - 执行 Spring类扫描的目录
	 * @throws Exception
	 */
	public void start(ServerDef def, String... springScanPkgs) throws Exception {
		if (springScanPkgs == null || springScanPkgs.length == 0) {
			StackTraceElement[] stack = Thread.currentThread().getStackTrace();
			String className = null;
			for (int i = stack.length - 1; i > 0 && className == null; i--) {
				StackTraceElement e = stack[i];
				if (e.getMethodName().equals("main")) {
					className = e.getClassName();
				} else if (className == null && e.getMethodName().equals("start")
						&& e.getClassName().endsWith(".ServerBootstap")) {
					className = stack[i + 1].getClassName();
				}
			}
			String packageName = className.substring(0, className.lastIndexOf('.'));
			System.out.println("use auto detect packageName: " + packageName); 
			springScanPkgs = new String[] { packageName };
			// throw new IllegalArgumentException("must specify packages for
			// spring's scan.");
		}
		long start = System.currentTimeMillis();
		//
		ChannelHandler business = new NettySpringmvcInitializer(def, springScanPkgs);
		//
		EventLoopGroup group = new NioEventLoopGroup();
		this.group = group;
		// Configure the server.
		final ServerBootstrap bootstrap = new ServerBootstrap()//
				.group(group)//
				.channel(NioServerSocketChannel.class)//
				.localAddress(def.port)//
				.option(ChannelOption.SO_BACKLOG, Integer.parseInt(System.getProperty("so.BACKLOG", "512")))
				// .option(ChannelOption.SO_KEEPALIVE,
				// Boolean.parseBoolean(System.getProperty("so.KEEPALIVE",
				// "true")))
				// .option(ChannelOption.SO_LINGER,
				// Integer.parseInt(System.getProperty("so.LINGER", "0")))
				.option(ChannelOption.SO_REUSEADDR, Boolean.parseBoolean(System.getProperty("so.REUSEADDR", "true")))//
				.childHandler(business);

		// Bind and start to accept incoming connections.
		try {
			ChannelFuture future = bootstrap.bind().sync();
			long end = System.currentTimeMillis();
			System.out.printf(">>> Server started at port:%d , %d ms .... <<< \n", def.port, (end - start));
			future.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			group.shutdownGracefully().sync();
		}
	}

	public void stop() throws InterruptedException {
		group.shutdownGracefully().sync();
	}
}
