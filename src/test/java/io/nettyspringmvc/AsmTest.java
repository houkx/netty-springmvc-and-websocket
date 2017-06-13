/**
 * 
 */
package io.nettyspringmvc;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.websocket.Session;

import org.junit.Test;

import aj.org.objectweb.asm.ClassReader;
import aj.org.objectweb.asm.ClassVisitor;
import aj.org.objectweb.asm.ClassWriter;
import aj.org.objectweb.asm.MethodVisitor;
import aj.org.objectweb.asm.Opcodes;
import aj.org.objectweb.asm.Type;
import io.nettyspringmvc.api.WsServerEndPoint;

/**
 * @author Houkx
 *
 */
public class AsmTest {

	@Test
	public void test_method_desc() throws Exception {
		Class<?> clazz = WsServerEndPoint.class;
		Method method = clazz.getMethod("onMessage", String.class, Session.class);
		Type[] argTypes = Type.getArgumentTypes(method);
		Type returnType = Type.getReturnType(method);
		StringBuilder sb = new StringBuilder(64);
		sb.append('(');
		for (Type t : argTypes) {
			sb.append(t.getDescriptor());
		}
		sb.append(')');
		sb.append(returnType.getDescriptor());
		System.out.println(sb);
	}

	@Test
	public void testVisit() throws Exception {
		String className = "io.nettyspringmvc.WsServerEndPoint_Chat";
		ClassReader cr = new ClassReader(className);
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ClassVisitor classVisitor = AsmDebugUtils.cv(Opcodes.ASM4, cw);
		cr.accept(classVisitor, ClassReader.SKIP_DEBUG);
		FileOutputStream out = new FileOutputStream(className.substring(className.lastIndexOf('.') + 1) + ".class");
		out.write(cw.toByteArray());
		out.close();
	}

	@Test
	public void testCreateClass() throws Exception {
		// String className = "com.miqtech.demo.ws.ChatWebsocketRoom";
		Class<?> tarClass = ChatRoom.class;
		String className = tarClass.getName();
		AtomicReference<String> classNameForReturn = new AtomicReference<String>(null);
		Class<?> ifaceClass = WsServerEndPoint.class;
		final byte[] bytes = proxyClass(className, ifaceClass, classNameForReturn);

		final String newClassName = classNameForReturn.get().replace('/', '.');
		System.out.println("newClassName = " + newClassName);
		File file = new File(newClassName.substring(newClassName.lastIndexOf('.') + 1) + ".class");
		if (file.getParentFile() != null && !file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				if (name.equals(newClassName)) {
					return defineClass(newClassName, bytes, 0, bytes.length);
				}
				return super.findClass(name);
			}
		};
		Class<?> newClass = loader.loadClass(newClassName);
		final HashMap<String, Object> mapLog = new HashMap<>();
		ChatRoom initargs = new ChatRoom() {

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

			public void onClose(Session session) {
				mapLog.put("onClose", 1);
			}
		};
		Object o = newClass.getConstructor(tarClass).newInstance(initargs);
		System.out.println("新对象:" + o + ", 构造方法：" + newClass.getConstructor(tarClass));
		WsServerEndPoint i = (WsServerEndPoint) o;
		i.onOpen(null);
		i.onMessage("hi", null);
		i.onError(null, null);
		i.onClose(null);
		//
		System.out.println("mapLog = " + mapLog);
		FileOutputStream out = new FileOutputStream(file);
		out.write(bytes);
		out.close();
	}

	private static class ClassAdapter {
		String method;
		String desc;
		int[] argIndexs;

	}

	private static int[] cvt(Class<?>[] src, Class<?>[] pts) {
		if (pts == null || pts.length == 0) {
			return new int[0];
		}
		if (pts.length > src.length) {
			// TODO set matched front and set default for others, or throw
			// Exception
			throw new RuntimeException(new IllegalArgumentException("endpoint method args to many!"));
		}
		int[] r = new int[pts.length];
		for (int i = 0; i < r.length; i++) {
			boolean find = false;
			int j = 0;
			for (; j < src.length; j++) {
				Class<?> t = src[j];
				if (pts[i].isAssignableFrom(t)) {
					r[i] = j + 1;
					find = true;
					break;
				}
			}
			if (!find) {
				// TODO set default for r[i], could be default value of the
				// Class( int :0, String:null, etc..)
				r[i] = Short.MAX_VALUE;
			}
		}
		return r;
	}

	/**
	 * 
	 * @param tarClass
	 * @param ifaceClass
	 * @param classNameForReturn
	 * @param adapter
	 *            -- K=接口方法名,V=目标方法映射信息
	 * @return
	 */
	private byte[] proxyClass(String tarClass, Class<?> ifaceClass, AtomicReference<String> classNameForReturn) {
		return proxyClass(tarClass, ifaceClass, classNameForReturn, null);
	}

	private byte[] proxyClass(String tarClass, Class<?> ifaceClass, AtomicReference<String> classNameForReturn,
			Map<String, ClassAdapter> adapterMap) {
		//
		String classX = tarClass.replace('.', '/');
		String classDesc = 'L' + classX + ';';
		ClassWriter cv = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		// visitField
		final String interfaceName = ifaceClass.getName().replace('.', '/');
		final String fieldName = "target";
		final String SUFFIX = "$asmWsImpl";
		String className = classX + SUFFIX;
		classNameForReturn.set(tarClass + SUFFIX);
		cv.visit(51, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", new String[] { interfaceName });
		cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fieldName, classDesc, null, null).visitEnd();
		// constructor
		{
			MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", '(' + classDesc + ")V", null, null);
			mv.visitCode();
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitVarInsn(Opcodes.ALOAD, 1);
			mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, classDesc);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		if (adapterMap == null || adapterMap.isEmpty()) {
			for (Method method : ifaceClass.getMethods()) {
				if (method.getDeclaringClass() != ifaceClass) {
					continue;
				}
				String methodName = method.getName();
				String methodDesc = getMethodDescriptor(method);
				MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methodName, methodDesc, null, null);
				mv.visitCode();
				mv.visitVarInsn(Opcodes.ALOAD, 0);
				mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, classDesc);
				final int argCount = 1 + method.getParameterTypes().length;
				String methodName_tar = methodName;
				String methodDesc_tar = methodDesc;
				for (int i = 1; i < argCount; i++) {
					mv.visitVarInsn(Opcodes.ALOAD, i);
				}
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classX, methodName_tar, methodDesc_tar, false);
				mv.visitInsn(Opcodes.RETURN);
				mv.visitMaxs(argCount, argCount);
				mv.visitEnd();
			}
		} else {
			for (Method method : ifaceClass.getMethods()) {
				if (method.getDeclaringClass() != ifaceClass) {
					continue;
				}
				MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), getMethodDescriptor(method),
						null, null);
				mv.visitCode();
				final int maxStack;
				ClassAdapter a = adapterMap.get(method.getName());
				if (a != null && a.argIndexs != null) {
					mv.visitVarInsn(Opcodes.ALOAD, 0);
					mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, classDesc);
					maxStack = 1 + a.argIndexs.length;
					String methodName_tar = a.method;
					String methodDesc_tar = a.desc;
					for (int i : a.argIndexs) {
						mv.visitVarInsn(Opcodes.ALOAD, i + 1);
					}
					mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classX, methodName_tar, methodDesc_tar, false);
				} else {
					maxStack = 0;
				}

				mv.visitInsn(Opcodes.RETURN);
				mv.visitMaxs(maxStack, 1 + method.getParameterTypes().length);
				mv.visitEnd();
			}
		}
		cv.visitEnd();
		return cv.toByteArray();
	}

	private static final String getMethodDescriptor(Method method) {
		Type[] argTypes = Type.getArgumentTypes(method);
		Type returnType = Type.getReturnType(method);
		StringBuilder sb = new StringBuilder(128);
		sb.append('(');
		for (Type t : argTypes) {
			sb.append(t.getDescriptor());
		}
		sb.append(')');
		sb.append(returnType.getDescriptor());
		return sb.toString();
	}
}
