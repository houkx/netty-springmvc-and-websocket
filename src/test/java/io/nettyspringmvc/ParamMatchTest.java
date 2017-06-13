/**
 * 
 */
package io.nettyspringmvc;

import java.util.Arrays;
import java.util.Map;

import javax.websocket.Session;

import org.junit.Assert;
import org.junit.Test;

import com.app.demo.ws.WebSocketDemo;

import io.nettyspringmvc.api.WsServerEndPoint;

/**
 * @author Houkx
 *
 */
public class ParamMatchTest {

	@Test
	public void test_a() {
		Object bean = new WebSocketDemo();
		Map<String, WsServerEndPoint> map = AnnotaionedWsHandlerParser.serverEndPoints(bean);
		System.out.println(map);
	}

	@Test
	public void testParam_match() {
		final Class<?>[] src = new Class<?>[] { String.class, Session.class };
		//
		Class<?>[] pts = new Class<?>[] { String.class, Session.class };
		Assert.assertTrue("must same when full same", Arrays.equals(cvt(src, pts), new int[] { 0, 1 }));
		pts = new Class<?>[] { Session.class, String.class };
		Assert.assertTrue("must same when any order", Arrays.equals(cvt(src, pts), new int[] { 1, 0 }));
		pts = new Class<?>[] {};
		Assert.assertTrue("must null", cvt(src, pts) == null);
		pts = new Class<?>[] { String.class };
		Assert.assertTrue("must match", Arrays.equals(cvt(src, pts), new int[] { 0 }));
		pts = new Class<?>[] { Session.class };
		Assert.assertTrue("must match", Arrays.equals(cvt(src, pts), new int[] { 1 }));
	}

	int[] cvt(Class<?>[] src, Class<?>[] pts) {
		if (pts == null || pts.length == 0) {
			return null;
		}
		if (pts.length <= src.length) {
			int[] r = new int[pts.length];
			for (int i = 0; i < r.length; i++) {
				boolean find = false;
				for (int j = 0; j < src.length; j++) {
					Class<?> t = src[j];
					if (pts[i].isAssignableFrom(t)) {
						r[i] = j;
						find = true;
						break;
					}
				}
				if (!find) {
					// TODO set default for r[i]
				}
			}
			return r;
		}
		return null;
	}

}
