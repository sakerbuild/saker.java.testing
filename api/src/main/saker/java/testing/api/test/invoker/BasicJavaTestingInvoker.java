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
package saker.java.testing.api.test.invoker;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

import saker.java.testing.api.test.exc.JavaTestRunnerFailureException;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.classloader.ClassLoaderDataFinder;
import saker.build.thirdparty.saker.util.classloader.MultiClassLoader;
import saker.build.thirdparty.saker.util.classloader.MultiDataClassLoader;
import saker.build.thirdparty.saker.util.io.StreamUtils;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;

/**
 * Basic {@link JavaTestingInvoker} implementation that sets up the {@link ClassLoader}s and other testing related
 * environment.
 * <p>
 * The test invoker will properly initialize the classloaders that can be used to load the user and test classes. It is
 * an abstract class that should be subclassed to specialize how the testing is performed.
 * <p>
 * When a test is executed, the class will <code>synchronize</code> on <code>BasicJavaTestingInvoker.class</code> and
 * acquire an exclusive lock during the invocation of a test case. It will replace the standard I/O fields in
 * {@link System}, and reset the {@linkplain System#getProperties() system properties}.
 * <p>
 * It will also set the {@linkplain Thread#setContextClassLoader(ClassLoader) thread context classloader} to the test
 * classloader. The standard I/O is buffered, and will be set as the
 * {@linkplain JavaTestInvocationResult#setFailureInformation(String) failure information} of the test result.
 * <p>
 * When a test execution is over, the class restores the mocked system fields.
 * <p>
 * Clients should subclass this class.
 */
public abstract class BasicJavaTestingInvoker implements JavaTestingInvoker {
	private ClassLoader userClassLoader;
	private ClassLoader testClassLoader;
	private ClassLoader testRunnerClassLoader;

	/**
	 * Creates a new testing invoker.
	 */
	public BasicJavaTestingInvoker() {
	}

	@Override
	public void initTestRunner(ClassLoader testrunnerclassloader, TestInvokerParameters parameters)
			throws JavaTestRunnerFailureException {
		this.testRunnerClassLoader = testrunnerclassloader;
	}

	@Override
	public void initClassLoaders(ClassLoader userclassloaderparent, ClassLoaderDataFinder[] userclasspath,
			ClassLoader testclassloaderparent, ClassLoaderDataFinder[] testclasspath) {
		userClassLoader = createUserClassLoader(userclassloaderparent, userclasspath);
		testClassLoader = createTestsClassLoader(MultiClassLoader.create(testclassloaderparent,
				ImmutableUtils.asUnmodifiableArrayList(userClassLoader, testRunnerClassLoader)), testclasspath);
	}

	/**
	 * Gets the user classloader.
	 * <p>
	 * The user classloader contains the classes being tested.
	 * 
	 * @return The classloader.
	 */
	protected final ClassLoader getUserClassLoader() {
		return userClassLoader;
	}

	/**
	 * Gets the test classloader.
	 * <p>
	 * The test classloader contains the test case classes.
	 * 
	 * @return The classloader.
	 */
	protected final ClassLoader getTestClassLoader() {
		return testClassLoader;
	}

	/**
	 * Gets the test runner classloader.
	 * <p>
	 * The test runner classloader contains the classes for the used testing framework.
	 * 
	 * @return The classloader.
	 */
	protected final ClassLoader getTestRunnerClassLoader() {
		return testRunnerClassLoader;
	}

	/**
	 * Creates the {@linkplain #getUserClassLoader() user classloader}.
	 * <p>
	 * Clients may override to perform custom initialization.
	 * 
	 * @param parent
	 *            The parent for the user classes. (May be <code>null</code>.) This is the same as passed to
	 *            {@link #initClassLoaders(ClassLoader, ClassLoaderDataFinder[], ClassLoader, ClassLoaderDataFinder[])
	 *            initClassLoaders}.
	 * @param userclasspath
	 *            The data finders for the user classpath.
	 * @return The user classloader.
	 */
	@SuppressWarnings("static-method")
	protected ClassLoader createUserClassLoader(ClassLoader parent, ClassLoaderDataFinder[] userclasspath) {
		if (userclasspath == null || userclasspath.length == 0) {
			return parent;
		}
		MultiDataClassLoader result = new MultiDataClassLoader(parent, userclasspath);
		return result;
	}

	/**
	 * Creates the {@linkplain #getTestClassLoader() test classloader}.
	 * <p>
	 * Clients may override to perform custom initialization.
	 * 
	 * @param parent
	 *            The parent for the test case classes. (May be <code>null</code>.) This is <b>not</b> the same as in
	 *            {@link #initClassLoaders(ClassLoader, ClassLoaderDataFinder[], ClassLoader, ClassLoaderDataFinder[])
	 *            initClassLoaders}, but a classloader that provides access to the {@linkplain #getUserClassLoader()
	 *            user classloader} and {@linkplain BasicJavaTestingInvoker#getTestRunnerClassLoader() test runner
	 *            classloader} as well.
	 * @param testclasspath
	 *            The data finders for the test classpath.
	 * @return The test classloader.
	 */
	@SuppressWarnings("static-method")
	protected ClassLoader createTestsClassLoader(ClassLoader parent, ClassLoaderDataFinder[] testclasspath) {
		if (testclasspath == null || testclasspath.length == 0) {
			return parent;
		}
		MultiDataClassLoader result = new MultiDataClassLoader(parent, testclasspath);
		return result;
	}

	@Override
	public final JavaTestInvocationResult invokeTest(JavaTestingFileProvider fileprovider,
			TestInvocationParameters parameters) throws JavaTestRunnerFailureException {
		Thread currentthread = Thread.currentThread();
		ClassLoader prevcontextcl = currentthread.getContextClassLoader();
		currentthread.setContextClassLoader(testClassLoader);
		try (UnsyncByteArrayOutputStream stdout = new UnsyncByteArrayOutputStream();
				UnsyncByteArrayOutputStream stderr = new UnsyncByteArrayOutputStream();
				PrintStream stdoutps = new PrintStream(stdout);
				PrintStream stderrps = new PrintStream(stderr)) {
			synchronized (BasicJavaTestingInvoker.class) {
				Properties prevprops = System.getProperties();
				PrintStream prevstdout = System.out;
				PrintStream prevstderr = System.err;
				InputStream prevstdin = System.in;
				System.setOut(stdoutps);
				System.setErr(stderrps);
				System.setIn(StreamUtils.nullInputStream());
				//reset the properties
				System.setProperties(null);
				try {
					JavaTestInvocationResult result = invokeTestImpl(fileprovider, parameters);
					if (!result.isSuccessful()) {
						StringBuilder failureinfo = new StringBuilder();
						if (!stdout.isEmpty()) {
							failureinfo.append("    ----- Std out -----     \n");
							failureinfo.append(stdout.toString());
							failureinfo.append("\n");
						}
						if (!stderr.isEmpty()) {
							failureinfo.append("    ----- Std err -----     \n");
							failureinfo.append(stderr.toString());
							failureinfo.append("\n");
						}
						if (failureinfo.length() > 0) {
							//append trailing marker so the reader knows where the end of the given output is
							failureinfo.append("    -----   END   -----     \n");
						}
						result.setFailureInformation(failureinfo.toString());
					}
					return result;
				} finally {
					System.setOut(prevstdout);
					System.setErr(prevstderr);
					System.setIn(prevstdin);
					System.setProperties(prevprops);
				}
			}
		} finally {
			currentthread.setContextClassLoader(prevcontextcl);
		}
	}

	@Override
	public void close() throws IOException {
		this.userClassLoader = null;
		this.testClassLoader = null;
		this.testRunnerClassLoader = null;
		JavaTestingInvoker.super.close();
	}

	/**
	 * Invokes the test.
	 * <p>
	 * This method is called by {@link #invokeTest(JavaTestingFileProvider, TestInvocationParameters)} after the
	 * environment has been initialized for the test case.
	 * <p>
	 * The arguments are the same as for the {@link #invokeTest(JavaTestingFileProvider, TestInvocationParameters)
	 * invokeTest} method.
	 * 
	 * @param fileprovider
	 *            The testing file provider. (May be <code>null</code>.)
	 * @param parameters
	 *            The invocation parameters for the test case. The test class name can be retrieved using
	 *            {@link TestInvocationParameters#getTestClassName()}.
	 * @return The test invocation result.
	 * @throws JavaTestRunnerFailureException
	 *             If the test runner encountered a failure during the invocation of the test.
	 */
	protected abstract JavaTestInvocationResult invokeTestImpl(JavaTestingFileProvider fileprovider,
			TestInvocationParameters parameters) throws JavaTestRunnerFailureException;

}
