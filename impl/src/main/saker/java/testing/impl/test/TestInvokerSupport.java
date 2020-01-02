/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
