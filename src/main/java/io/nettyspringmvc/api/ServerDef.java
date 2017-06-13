/**
 * 
 */
package io.nettyspringmvc.api;

import java.util.Map;

/**
 * 服务定义
 * 
 * @author Houkx
 *
 */
public class ServerDef {
	/**
	 * 服务绑定的端口
	 */
	public final int port;
	/**
	 * boss 线程数
	 */
	public final int bossThreads;
	/**
	 * worker 线程数
	 */
	public final int workThreads;
	/**
	 * 业务初始化完毕的回调--方便做业务启动时的新旧切换，减少部署的延迟
	 */
	public final Runnable businessInitFinishedCallback;

	// TODO 基本参数定义，如超过多长时间断线
	/**
	 * uri <-> WsServerEndPoint 的映射，用来处理 Websocket回调.
	 */
	public final Map<String, WsServerEndPoint> serverEndPoints;

	public ServerDef(int port, int bossThreads, int workThreads, Map<String, WsServerEndPoint> serverEndPoints, Runnable businessStartedCallback) {
		this.port = port;
		this.bossThreads = bossThreads;
		this.workThreads = workThreads;
		this.serverEndPoints = serverEndPoints;
		this.businessInitFinishedCallback = businessStartedCallback;
	}

}
