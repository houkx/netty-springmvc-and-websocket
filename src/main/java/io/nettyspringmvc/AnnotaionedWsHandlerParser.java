/**
 * 
 */
package io.nettyspringmvc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.server.ServerEndpoint;

import io.nettyspringmvc.api.WsServerEndPoint;
import io.nettyspringmvc.util.ClassProxyUtil;
import io.nettyspringmvc.util.ClassProxyUtil.MethodAdapter;
import io.nettyspringmvc.util.MethodAdapterUtil;

/**
 * @ServerEndpoint 注解解析工具类--将注解类的对象转为WsServerEndPoint接口实现
 * @author Houkx
 *
 */
class AnnotaionedWsHandlerParser {

	static Map<String, WsServerEndPoint> serverEndPoints(Object... wsBeans) {
		if (wsBeans != null && wsBeans.length > 0) {
			Map<String, WsServerEndPoint> map = new HashMap<>(wsBeans.length);

			for (Object bean : wsBeans) {
				ServerEndpoint ann = bean.getClass().getAnnotation(ServerEndpoint.class);
				if (ann != null) {
					WsServerEndPoint iface = parseEndpointBean(bean, bean.getClass());
					if (iface != null) {
						map.put(ann.value(), iface);
					}
				}
			}
			return map;
		}
		return null;
	}

	private static WsServerEndPoint parseEndpointBean(Object bean, Class<?> tarClass) {
		// 注解类的对象转换为接口实现
		final Map<String, Method> map = new HashMap<>();
		if (tarClass == null) {
			tarClass = bean.getClass();
		}
		for (Method m : tarClass.getMethods()) {
			Annotation[] anns = m.getAnnotations();
			if (anns != null && anns.length > 0) {
				for (Annotation a : anns) {
					Class<?> clazz = a.annotationType();
					if (clazz == OnOpen.class || clazz == OnMessage.class //
							|| clazz == OnError.class || clazz == OnClose.class) {
						String name = clazz.getSimpleName();
						map.put(Character.toLowerCase(name.charAt(0)) + name.substring(1), m);
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
		Class<?> ifaceClass = WsServerEndPoint.class;
		final Map<String, MethodAdapter> adapterMap = new HashMap<>();
		for (Method method : ifaceClass.getMethods()) {
			if (method.getDeclaringClass() != ifaceClass) {
				continue;
			}
			Method tarMethod = map.get(method.getName());
			if (tarMethod != null) {
				MethodAdapter a = MethodAdapterUtil.adapter(method, tarMethod);
				adapterMap.put(method.getName(), a);
			}
		}
		AtomicReference<String> classNameForReturn = new AtomicReference<String>(null);
		final byte[] bytes = ClassProxyUtil.proxyClass(tarClass.getName(), ifaceClass, classNameForReturn, adapterMap);
		final String newClassName = classNameForReturn.get().replace('/', '.');
		ClassLoader loader = new ClassLoader(AnnotaionedWsHandlerParser.class.getClassLoader()) {
			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				if (name.equals(newClassName)) {
					return defineClass(newClassName, bytes, 0, bytes.length);
				}
				return super.findClass(name);
			}
		};
		try {
			Class<?> newClass = loader.loadClass(newClassName);
			Object o = newClass.getConstructor(tarClass).newInstance(bean);
			return (WsServerEndPoint) o;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
