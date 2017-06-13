/**
 * 
 */
package io.nettyspringmvc.util;

import java.lang.reflect.Method;

import io.nettyspringmvc.util.ClassProxyUtil.MethodAdapter;

public class MethodAdapterUtil {

	public static MethodAdapter adapter(Method ifaceMethod, Method tarMethod) {
		MethodAdapter a = new MethodAdapter();
		a.desc = ClassProxyUtil.getMethodDescriptor(tarMethod);
		a.method = tarMethod.getName();
		a.argIndexs = cvt(ifaceMethod.getParameterTypes(), tarMethod.getParameterTypes());
		return a;
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
					r[i] = j;
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
}
