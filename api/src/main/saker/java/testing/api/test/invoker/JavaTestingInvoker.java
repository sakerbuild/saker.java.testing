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

import java.io.Closeable;
import java.io.IOException;

import saker.java.testing.api.test.exc.JavaTestRunnerFailureException;
import saker.build.thirdparty.saker.util.classloader.ClassLoaderDataFinder;
import saker.build.thirdparty.saker.util.classloader.MultiClassLoader;
import saker.build.thirdparty.saker.util.classloader.MultiDataClassLoader;

/**
 * Handles one or multiple Java test invocations.
 * <p>
 * The {@link JavaTestingInvoker} interface is the manager object that executes the Java test invocations. At the start
 * of testing it is initialized, then asked to run each test case, and closed at the end of testing.
 * <p>
 * The test invoker is initialized with the {@link ClassLoader}s that make the classes being part of the testing
 * accessible to the test invoker. The test invoker can decide to manage the test execution in an implementation
 * dependent way.
 * <p>
 * The test invoker is considered to be an overall environment provider during the testing. It is a stateful object and
 * test implementations may access various aspects of the test invoker. The test invoker should be accessible to the
 * test case classes.
 * <p>
 * Clients may implement this interface. However, they are recommended to extend one of the basic test invoker API
 * classes that integrate with the incremental testing instrumentation of the build task. See
 * {@link BasicInstrumentationJavaTestInvoker}.
 * <p>
 * Implementations must have a public no-arg constructor.
 */
public interface JavaTestingInvoker extends Closeable {
	/**
	 * Initializes the test runner.
	 * <p>
	 * This method is called first as part of the initialization.
	 * <p>
	 * The method should perform the operations that required in order to locate the test runner that will execute the
	 * tests. This can be done in an implementation dependent way.
	 * <p>
	 * In common implementations, the method will attempt to load a class that will be called with the test classes as
	 * its input in the {@link #invokeTest(JavaTestingFileProvider, TestInvocationParameters)} method. <br>
	 * One example for this may be that is to find the entry point of the testing framework that contains a
	 * <code>main</code> method. That <code>main</code> method is caleld for each test in
	 * {@link #invokeTest(JavaTestingFileProvider, TestInvocationParameters) invokeTest}.
	 * 
	 * @param testrunnerclassloader
	 *            The {@link ClassLoader} that is used to load the test runner classes. May be the same as the
	 *            classloader of <code>this</code> testing invoker.
	 * @param parameters
	 *            The test invoker parameters that are passed by the user as configuration.
	 * @throws JavaTestRunnerFailureException
	 *             If the initialization failed-
	 */
	public void initTestRunner(ClassLoader testrunnerclassloader, TestInvokerParameters parameters)
			throws JavaTestRunnerFailureException;

	/**
	 * Requests the test invoker to perform the initialization of the {@link ClassLoader}s for the user and test
	 * classpath.
	 * <p>
	 * This method is called after {@link #initTestRunner(ClassLoader, TestInvokerParameters)}.
	 * <p>
	 * The user class loader is the one that contains the classes being tested. The test class loader is the one that
	 * contains the test cases.
	 * <p>
	 * The relations between the constructed classloaders may be implementation dependent for the test invoker. However,
	 * we recommend that the user classloader is constructed with the specified parent as its parent, and the test
	 * classloader is constructed with the specified parent, the user classloader, and the test runner classloader as
	 * its parent. The {@link BasicJavaTestingInvoker} class initializes them this way.
	 * 
	 * @param userclassloaderparent
	 *            The classloader that should be the part of the parents of the user classpath. (May be
	 *            <code>null</code>.)
	 * @param userclasspath
	 *            The data finders for the user classpath.
	 * @param testclassloaderparent
	 *            The classloader that should be the paret of the parents of the test classpath. (May be
	 *            <code>null</code>.)
	 * @param testclasspath
	 *            The data finders for the test classpath.
	 * @see MultiDataClassLoader
	 * @see MultiClassLoader
	 */
	public void initClassLoaders(ClassLoader userclassloaderparent, ClassLoaderDataFinder[] userclasspath,
			ClassLoader testclassloaderparent, ClassLoaderDataFinder[] testclasspath);

	/**
	 * Invokes a test case.
	 * <p>
	 * This method is called after the initialization was performed.
	 * <p>
	 * The test invoker should invoke the test class specified with the given parameters. It may do so in an
	 * implementation dependent manner.
	 * <p>
	 * The file provider parameter can be used by test case instrumentation to request files that are being accessed by
	 * the test cases. The {@link BasicInstrumentationJavaTestInvoker} subclass handles it appropriately.
	 * <p>
	 * The result of the test is returned from this function. Exceptions thrown by the test cases shouldn't be relayed
	 * to the caller. The test result object should contain any failure information. It can also be used to report the
	 * dependencies of the test case.
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
	public JavaTestInvocationResult invokeTest(JavaTestingFileProvider fileprovider,
			TestInvocationParameters parameters) throws JavaTestRunnerFailureException;

	/**
	 * Closes this test invoker.
	 * <p>
	 * This method is called when no more test are executed using this invoker.
	 * <p>
	 * The test invoker should clean up any possible resources that were allocated by the test cases.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public default void close() throws IOException {
	}
}
