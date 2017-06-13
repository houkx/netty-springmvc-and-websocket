/**
 * 
 */
package io.nettyspringmvc.api;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务定义的构造者
 * 
 * @author Houkx
 *
 */
public class ServerDefBuilder {
	private int port = 8080;
	private int bossThreads;
	private int workThreads;
	private Map<String, WsServerEndPoint> serverEndPoints = new HashMap<>();
	private Runnable businessInitFinishedCallback;

	public static ServerDefBuilder builder() {
		return new ServerDefBuilder();
	}

	public ServerDefBuilder setServerEndPoints(Map<String, WsServerEndPoint> serverEndPoints) {
		this.serverEndPoints = serverEndPoints;
		return this;
	}

	public ServerDefBuilder setPort(int port) {
		this.port = port;
		return this;
	}

	public ServerDefBuilder setBossThreads(int bossThreads) {
		this.bossThreads = bossThreads;
		return this;
	}

	public ServerDefBuilder setWorkThreads(int workThreads) {
		this.workThreads = workThreads;
		return this;
	}

	public ServerDefBuilder setBusinessInitFinishedCallback(Runnable businessInitFinishedCallback) {
		this.businessInitFinishedCallback = businessInitFinishedCallback;
		return this;
	}

	public ServerDef build() {
		return new ServerDef(port, bossThreads, workThreads, serverEndPoints, businessInitFinishedCallback);
	}
}
