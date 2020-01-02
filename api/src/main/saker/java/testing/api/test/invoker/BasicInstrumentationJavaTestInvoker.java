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

import java.lang.reflect.InvocationTargetException;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import saker.java.testing.api.test.exc.JavaTestRunnerFailureException;
import saker.java.testing.bootstrapagent.InstrumentationData;
import saker.java.testing.bootstrapagent.InstrumentationProcessExitRequestedException;
import saker.java.testing.bootstrapagent.TestFileRequestor;

/**
 * {@link BasicJavaTestingInvoker} subclass that integrates with the incremental instrumentation of the Java testing
 * build task.
 * <p>
 * The test invoker will handle the instrumentation and dependency information retrieval of the invoked test cases.
 * <p>
 * The test invocation can be performed in {@link #runTest(TestInvocationParameters)}, and implementations are not
 * required to specially handle the test results.
 * <p>
 * Clients should extend this class.
 */
public abstract class BasicInstrumentationJavaTestInvoker extends BasicJavaTestingInvoker {
	/**
	 * Creates a new testing invoker.
	 */
	public BasicInstrumentationJavaTestInvoker() {
	}

	@Override
	protected final JavaTestInvocationResult invokeTestImpl(JavaTestingFileProvider fileprovider,
			TestInvocationParameters parameters) throws JavaTestRunnerFailureException {
		JavaTestInvocationResult result;
		Integer exitcode = null;
		boolean successful = false;
		NavigableSet<String> dependentclasses;
		NavigableMap<String, NavigableSet<String>> listeddirectories;

		synchronized (InstrumentationData.TESTING_INSTRUMENTATION_LOCK) {
			InstrumentationData.resetClassLoggerInstrumentation();
			if (fileprovider != null) {
				TestFileRequestor.init(fileprovider::requestFileRead, fileprovider::requestFileWrite,
						fileprovider::requestFileList, fileprovider::requestClassLoaderResource);
			} else {
				TestFileRequestor.clear();
			}
			try {
				try {
					runTestImpl(parameters);
					successful = true;
				} catch (InvocationTargetException e) {
					Throwable targetexc = e.getTargetException();
					if (targetexc instanceof InstrumentationProcessExitRequestedException) {
						exitcode = ((InstrumentationProcessExitRequestedException) targetexc).getResultCode();
						targetexc.printStackTrace();
					} else {
						targetexc.printStackTrace();
					}
				} catch (InstrumentationProcessExitRequestedException e) {
					exitcode = e.getResultCode();
				}
				dependentclasses = new TreeSet<>(InstrumentationData.getDependentClasses());
				listeddirectories = new TreeMap<>(TestFileRequestor.LISTED_DIRECTORY_CONTENTS);
			} finally {
				TestFileRequestor.clear();
			}
		}
		result = new JavaTestInvocationResult(successful);
		result.setDependentClasses(dependentclasses);
		result.setExitCode(exitcode);
		result.setSuccessful(successful);
		result.setListedDirectories(listeddirectories);
		return result;
	}

	private void runTestImpl(TestInvocationParameters parameters) throws JavaTestRunnerFailureException,
			InvocationTargetException, InstrumentationProcessExitRequestedException {
		runTest(parameters);
	}

	/**
	 * Executes the test with the given parameters.
	 * <p>
	 * The method should succeed if and only if the test case is considered successful, and throw
	 * {@link InvocationTargetException} if the test case failed.
	 * <p>
	 * You can load the test class with the name {@link TestInvocationParameters#getTestClassName()} from the
	 * {@link #getTestClassLoader()}.
	 * 
	 * @param parameters
	 *            The test parameters.
	 * @throws JavaTestRunnerFailureException
	 *             If the test runner encountered a failure during the invocation of the test.
	 * @throws InvocationTargetException
	 *             If the test case failed.
	 */
	protected abstract void runTest(TestInvocationParameters parameters)
			throws JavaTestRunnerFailureException, InvocationTargetException;

}
