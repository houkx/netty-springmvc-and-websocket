package io.nettyspringmvc.util;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import aj.org.objectweb.asm.ClassWriter;
import aj.org.objectweb.asm.MethodVisitor;
import aj.org.objectweb.asm.Opcodes;
import aj.org.objectweb.asm.Type;


public class ClassProxyUtil {
	public static class MethodAdapter {
		public String method;
		public String desc;
		public int[] argIndexs;
	}

	public static byte[] proxyClass(String tarClass, Class<?> ifaceClass, AtomicReference<String> classNameForReturn) {
		return proxyClass(tarClass, ifaceClass, classNameForReturn, null);
	}

	/**
	 * 产生接口的代理类
	 * 
	 * @param tarClass
	 *            - 目标类名
	 * @param ifaceClass
	 *            - 接口类
	 * @param classNameForReturn
	 *            -- 返回的新类名
	 * @param adapterMap
	 *            -- K=接口方法名,V=目标方法映射信息
	 * @return 新类的字节码
	 */
	public static byte[] proxyClass(String tarClass, Class<?> ifaceClass, AtomicReference<String> classNameForReturn,
			Map<String, MethodAdapter> adapterMap) {
		//
		String classX = tarClass.replace('.', '/');
		String classDesc = 'L' + classX + ';';
		ClassWriter cv = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		// visitField
		final String interfaceName = ifaceClass.getName().replace('.', '/');
		final String fieldName = "target";// 字段名
		final String SUFFIX = "$asmWsImpl";// 新类名的后缀
		String className = classX + SUFFIX;
		classNameForReturn.set(tarClass + SUFFIX);
		cv.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", new String[] { interfaceName });
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
				MethodAdapter a = adapterMap.get(method.getName());
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

	public static final String getMethodDescriptor(Method method) {
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
