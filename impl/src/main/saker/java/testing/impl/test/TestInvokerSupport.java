package saker.java.testing.impl.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import saker.build.util.classloader.WildcardFilteringClassLoader;
import saker.build.thirdparty.saker.rmi.connection.MethodTransferProperties;
import saker.build.thirdparty.saker.rmi.connection.RMITransferProperties;
import saker.build.thirdparty.saker.rmi.connection.RMITransferProperties.Builder;
import saker.build.thirdparty.saker.rmi.io.writer.RMIObjectWriteHandler;
import saker.build.thirdparty.saker.util.ReflectUtils;
import saker.build.thirdparty.saker.util.classloader.ClassLoaderUtil;

public class TestInvokerSupport {
	private TestInvokerSupport() {
		throw new UnsupportedOperationException();
	}

	public static ClassLoader getTestingUserClassLoaderParent()
			throws InvocationTargetException, IllegalAccessException {
		return ClassLoaderUtil.getPlatformClassLoaderParent();
	}

	public static RMITransferProperties getTestInvokerRMITransferProperties() {
		Method getuserclparentmethod = ReflectUtils.getMethodAssert(TestInvokerSupport.class,
				"getTestingUserClassLoaderParent");
		MethodTransferProperties getuserclmethodprops = MethodTransferProperties.builder(getuserclparentmethod)
				.returnWriter(RMIObjectWriteHandler.remote()).build();

		Method wildcardfilterclmethod = ReflectUtils.getMethodAssert(WildcardFilteringClassLoader.class, "create",
				ClassLoader.class, CharSequence[].class);
		MethodTransferProperties wildcardfilterclmethodprops = MethodTransferProperties.builder(wildcardfilterclmethod)
				.returnWriter(RMIObjectWriteHandler.remote()).build();

		Builder builder = RMITransferProperties.builder();
		builder.add(wildcardfilterclmethodprops);
		builder.add(getuserclmethodprops);
		return builder.build();
	}
}
