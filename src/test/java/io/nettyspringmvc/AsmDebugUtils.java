package io.nettyspringmvc;

import java.lang.reflect.Method;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import aj.org.objectweb.asm.ClassVisitor;
import aj.org.objectweb.asm.FieldVisitor;
import aj.org.objectweb.asm.MethodVisitor;
import aj.org.objectweb.asm.Opcodes;

/**
 * 
 * @author Houkx
 *
 */
public class AsmDebugUtils {

	public static ClassVisitor cv() {
		return cv(Opcodes.ASM4, null);
	}

	public static ClassVisitor cv(final int api, final ClassVisitor tar) {
		Enhancer enhancer = new Enhancer();
		Class<?> clazz = ClassVisitor.class;
		enhancer.setSuperclass(clazz);
		enhancer.setCallback(new MethodInterceptor() {
			@Override
			public Object intercept(Object o, Method m, Object[] args, MethodProxy proxy) throws Throwable {
				String name = m.getName();
				Object rt = tar == null ? null : m.invoke(tar, args);
				if (name.equals("visitMethod")) {
					System.out.println("\n=======================\n");
					printArgs(args, name);
					MethodVisitor mv = mv(api, (MethodVisitor) rt);
					return mv;
				} else if (name.equals("visitField")) {
					System.out.println("\n-----------------------\n");
					printArgs(args, name);
					FieldVisitor fv = fv(api, (FieldVisitor) rt);
					return fv;
				}
				printArgs(args, name);
				return rt;
			}

		});
		return (ClassVisitor) enhancer.create(new Class[] { int.class, clazz }, new Object[] { api, tar });
	}

	public static FieldVisitor fv(int api, final FieldVisitor tar) {
		Enhancer enhancer = new Enhancer();
		Class<?> clazz = FieldVisitor.class;
		enhancer.setSuperclass(clazz);
		enhancer.setCallback(new MethodInterceptor() {
			@Override
			public Object intercept(Object o, Method m, Object[] args, MethodProxy proxy) throws Throwable {
				String name = m.getName();
				printArgs(args, name);
				Object rt = tar == null ? null : m.invoke(tar, args);
				return rt;
			}

		});
		return (FieldVisitor) enhancer.create(new Class[] { int.class, clazz }, new Object[] { api, tar });
	}

	public static MethodVisitor mv(int api, final MethodVisitor tar) {
		Enhancer enhancer = new Enhancer();
		Class<?> clazz = MethodVisitor.class;
		enhancer.setSuperclass(clazz);
		enhancer.setCallback(new MethodInterceptor() {
			@Override
			public Object intercept(Object o, Method m, Object[] args, MethodProxy proxy) throws Throwable {
				String name = m.getName();
				printArgs(args, name);
				Object rt = tar == null ? null : m.invoke(tar, args);
				return rt;
			}

		});
		return (MethodVisitor) enhancer.create(new Class[] { int.class, clazz }, new Object[] { api, tar });
	}

	private static void printArgs(Object[] args, String name) {
		String str = toString(args);
		System.out.printf("%s: (%s)\n", name, str.substring(1, str.length() - 1));
	}

	private static String toString(Object[] a) {
		if (a == null)
			return "null";

		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0;; i++) {
			if (a[i] != null && a[i].getClass().isArray()) {
				b.append(toString((Object[]) a[i]));
			} else {
				b.append(String.valueOf(a[i]));
			}
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}
}
