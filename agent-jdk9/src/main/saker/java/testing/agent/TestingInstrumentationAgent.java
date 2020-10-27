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
package saker.java.testing.agent;

import java.io.File;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import saker.build.thirdparty.saker.util.io.JarFileUtils;
import saker.build.thirdparty.saker.util.io.StreamUtils;

public class TestingInstrumentationAgent {
	public static final String JAVA_IO_FILESYSTEM_PROXY_ARGUMENT_INTERNAL_NAME = "java/lang/Object";
	public static final String JAVA_IO_FILESYSTEM_PROXY_INTERNAL_NAME = "saker/java/testing/bootstrapagent/java/io/IoFileSystemSakerTestProxy";

	public static void premain(String agentArgs, Instrumentation inst) throws Throwable {
		File bootstrappath = new File(agentArgs);
		try {
			inst.appendToBootstrapClassLoaderSearch(JarFileUtils.createMultiReleaseJarFile(bootstrappath));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		//need to open the java.io class so the JVM doesn't throw an exception in case of --illegal-access=deny
		Map<String, Set<Module>> extraopens = new TreeMap<>();
		extraopens.put("java.io",
				Collections.singleton(Class
						.forName("saker.java.testing.bootstrapagent.java.io.IoFileSystemSakerTestProxy", false, null)
						.getModule()));
		inst.redefineModule(File.class.getClass().getModule(), Collections.emptySet(), Collections.emptyMap(),
				extraopens, Collections.emptySet(), Collections.emptyMap());

		UserClassFileTransformer usertransformer = new UserClassFileTransformer();
		inst.addTransformer(usertransformer, true);
		for (Class<?> c : inst.getAllLoadedClasses()) {
			if (inst.isModifiableClass(c)) {
				try {
					inst.retransformClasses(c);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		loadStackOverFlowInstrumentationProtection();
	}

	private static void loadStackOverFlowInstrumentationProtection() {
		//when a StackOverFlowError happens, and the JVM still has some unloaded classes that needs to be loaded in order to throw the error,
		//then those classes will be loaded on top of the stack
		//that will immediately cause another stackoverflow, so those classes will not be transformed
		//in order to prevent this, we trigger the loading of these classes
		//these were determined by testing on Windows, but probably should work on other operating systems too

		//the JVM logs this error in case it happens
		//*** java.lang.instrument ASSERTION FAILED ***: "!errorOutstanding" with message transform method call failed at JPLISAgent.c line: 844
		@SuppressWarnings("unused")
		Class<?> c = InterruptedIOException.class;
		new StackOverflowError().printStackTrace(new PrintStream(StreamUtils.nullOutputStream()));
	}
}
