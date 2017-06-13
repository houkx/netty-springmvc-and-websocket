package io.nettyspringmvc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.junit.Test;

import io.nettyspringmvc.api.WsServerEndPoint;
import io.nettyspringmvc.util.ClassProxyUtil;
import io.nettyspringmvc.util.ClassProxyUtil.MethodAdapter;
import io.nettyspringmvc.util.MethodAdapterUtil;

public class AsmAnnTest {

	@Test
	public void test_wsAnn() throws Exception {
		final HashMap<String, Object> mapLog = new HashMap<>();
		ChatRoom bean = new ChatRoom() {
			public void onOpen(Session session) {
				mapLog.put("onOpen", 1);
			}

			public void onMessage(String message, Session session) {
				System.out.println("收到消息：" + message);
				mapLog.put("onMessage", 2);
			}

			public void onError(Session session, Throwable thr) {
				mapLog.put("onError", 2);
			}

			public void onError(/* Session session, */Throwable error) {
				mapLog.put("onError", 2);
			}

			public void on_error() {
				mapLog.put("onError", 2);
			}

			public void onClose(Session session) {
				mapLog.put("onClose", 1);
			}
		};
		WsServerEndPoint i = parseEndpointBean(bean, ChatRoom.class);
		i.onOpen(null);
		i.onMessage("hi", null);
		i.onError(null, new Exception());
		i.onClose(null);
		System.out.println("mapLog = " + mapLog);
	}

	private static WsServerEndPoint parseEndpointBean(Object bean, Class<?> tarClass) throws IOException {
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
		File file = new File(newClassName.substring(newClassName.lastIndexOf('.') + 1) + ".class");
		if (file.getParentFile() != null && !file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		FileOutputStream out = new FileOutputStream(file);
		out.write(bytes);
		out.close();
		System.out.println("写入文件：" + file);
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
