package saker.java.testing.agent;

import java.io.File;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;

import saker.build.thirdparty.saker.util.io.JarFileUtils;
import saker.build.thirdparty.saker.util.io.StreamUtils;

public class TestingInstrumentationAgent {
	public static final String JAVA_IO_FILESYSTEM_PROXY_ARGUMENT_INTERNAL_NAME = "java/io/FileSystem";
	public static final String JAVA_IO_FILESYSTEM_PROXY_INTERNAL_NAME = "java/io/IoFileSystemSakerTestProxy";

	public static void premain(String agentArgs, Instrumentation inst) {
		File bootstrappath = new File(agentArgs);
		try {
			inst.appendToBootstrapClassLoaderSearch(JarFileUtils.createMultiReleaseJarFile(bootstrappath));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
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
