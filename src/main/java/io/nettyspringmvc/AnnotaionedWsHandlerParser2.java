/**
 * 
 */
package io.nettyspringmvc;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import io.nettyspringmvc.api.WsServerEndPoint;

/**
 * @ServerEndpoint 注解解析工具类--将注解类的对象转为WsServerEndPoint接口实现
 * @author Houkx
 *
 */
class AnnotaionedWsHandlerParser2 {

	static Map<String, WsServerEndPoint> serverEndPoints(Object... wsBeans) {
		if (wsBeans != null && wsBeans.length > 0) {
			Map<String, WsServerEndPoint> map = new HashMap<>(wsBeans.length);

			for (Object bean : wsBeans) {
				ServerEndpoint ann = bean.getClass().getAnnotation(ServerEndpoint.class);
				if (ann != null) {
					WsServerEndPoint iface = parseEndpointBean(bean);
					if (iface != null) {
						map.put(ann.value(), iface);
					}
				}
			}
			return map;
		}
		return null;
	}

	private static WsServerEndPoint parseEndpointBean(final Object bean) {
		// 注解类的对象转换为接口实现
		final HashMap<Integer, int[]> map = new HashMap<>();
		Class<?>[] session = new Class[] { Session.class };
		Class<?>[] OnMSG = new Class[] { String.class, Session.class };
		Class<?>[] OnErr = new Class[] { Session.class, Throwable.class };
		for (Method m : bean.getClass().getMethods()) {
			Annotation[] anns = m.getAnnotations();
			if (anns != null && anns.length > 0) {
				Class<?>[] pts = m.getParameterTypes();
				for (Annotation a : anns) {
					Class<?> clazz = a.annotationType();
					if (clazz == OnOpen.class) {// session
						map.put(m.getName().hashCode(), cvt(session, pts));
					} else if (clazz == OnMessage.class) {// string, session
						map.put(m.getName().hashCode(), cvt(OnMSG, pts));
					} else if (clazz == OnError.class) {// session, throwable
						map.put(m.getName().hashCode(), cvt(OnErr, pts));
					} else if (clazz == OnClose.class) {// session
						map.put(m.getName().hashCode(), cvt(session, pts));
					}
				}
				if (map.size() == 4) {
					break;
				}
			}
		}
		if (map.isEmpty()) {
			return null;
		}
		InvocationHandler h = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				if (method.getDeclaringClass().getClassLoader() == null) {
					return method.invoke(bean, args);
				}
				int[] indexArr = map.get(method.getName().hashCode());
				if (indexArr != null) {
					if (indexArr == FULL_MATCH) {
						return method.invoke(bean, args);
					} else {
						Object[] argsNew = new Object[indexArr.length];
						for (int i = 0; i < indexArr.length; i++) {
							int index = indexArr[i];
							argsNew[i] = args[index];
						}
						return method.invoke(bean, argsNew);
					}
				}
				return null;
			}
		};
		return (WsServerEndPoint) Proxy.newProxyInstance(bean.getClass().getClassLoader(),
				new Class[] { WsServerEndPoint.class }, h);
	}

	private static final int[] EMPTY = new int[0];
	private static final int[] FULL_MATCH = new int[0];

	private static int[] cvt(Class<?>[] src, Class<?>[] pts) {
		if (pts == null || pts.length == 0) {
			return EMPTY;
		}
		if (pts.length > src.length) {
			// TODO set matched front and set default for others, or throw
			// Exception
			throw new RuntimeException(new IllegalArgumentException("endpoint method args to many!"));
		}
		int[] r = new int[pts.length];
		boolean fullMatch = true;
		for (int i = 0; i < r.length; i++) {
			boolean find = false;
			int j = 0;
			for (; j < src.length; j++) {
				Class<?> t = src[j];
				if (pts[i].isAssignableFrom(t)) {
					r[i] = j;
					find = true;
					break;
				}
			}
			if (i != j) {
				fullMatch = false;
			}
			if (!find) {
				// TODO set default for r[i], could be default value of the
				// Class( int :0, String:null, etc..)
				r[i] = Short.MAX_VALUE;
			}
		}
		if (pts.length == src.length && fullMatch) {
			return FULL_MATCH;
		}
		return r;
	}
}
