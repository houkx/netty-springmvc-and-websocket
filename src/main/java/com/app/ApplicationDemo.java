/**
 * 
 */
package com.app;

import io.nettyspringmvc.ServerBootstap;
import io.nettyspringmvc.api.ServerDefBuilder;

/**
 * @author houkx
 *
 */
public class ApplicationDemo {

	/**
	 * http and ws demo
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		int port = 8080;
		new ServerBootstap().start(ServerDefBuilder.builder()//
				.setPort(port)//
				.build()//
		/* , "com.app.demo" //--注解扫描的包, 默认为当前包 */
		);
	}

}
