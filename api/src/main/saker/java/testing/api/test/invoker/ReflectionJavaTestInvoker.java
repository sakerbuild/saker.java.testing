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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import saker.java.testing.api.test.exc.JavaTestRunnerFailureException;

/**
 * {@link BasicInstrumentationJavaTestInvoker} subclass that invokes the <code>main</code> method of a specified test
 * runner class.
 * <p>
 * The test invoker will execute a test case by passing it to the specified <code>main(String[])</code> method as an
 * argument. The called method may locate the test class by using the {@linkplain Thread#getContextClassLoader() thread
 * context classloader} (which is set by the {@link BasicJavaTestingInvoker} superclass).
 * <p>
 * The test invoker should be configured using the parameters specified with the <code>PARAMETER_*</code> constants in
 * the class.
 * <p>
 * The actual arguments for the <code>main</code> method call for a test case is constructed by taking the
 * <code>{@value #PARAMETER_PREFIX_ARGUMENTS}</code>, <code>&lt;test-case-class-name&gt;</code>, and
 * <code>{@value #PARAMETER_SUFFIX_ARGUMENTS}</code> parameters as values.
 */
public class ReflectionJavaTestInvoker extends BasicInstrumentationJavaTestInvoker {
	/**
	 * Parameter specifying a list of arguments that should be prepended before the test class name when the
	 * <code>main</code> method is called.
	 * <p>
	 * The parameter should have a format of <code>&lt;param&gt;[,&lt;param&gt;]*</code>. The intermediate commas
	 * (<code>,</code>) can be escaped using a backslash (<code>\</code>).
	 */
	public static final String PARAMETER_PREFIX_ARGUMENTS = "PrefixArguments";
	/**
	 * <b>Required parameter</b> specifying the name of the test runner class.
	 * <p>
	 * The class is loaded from the {@linkplain #getTestRunnerClassLoader() test runner class loader}.
	 */
	public static final String PARAMETER_TEST_RUNNER_CLASS = "TestRunnerClass";
	/**
	 * Parameter specifying a list of arguments that should be appended after the test class name when the
	 * <code>main</code> method is called.
	 * <p>
	 * The parameter should have a format of <code>&lt;param&gt;[,&lt;param&gt;]*</code>. The intermediate commas
	 * (<code>,</code>) can be escaped using a backslash (<code>\</code>).
	 */
	public static final String PARAMETER_SUFFIX_ARGUMENTS = "SuffixArguments";

	private static final Pattern CMDLINE_SPLITTER = Pattern.compile("(?<!\\\\),");

	private static List<String> splitCommandLine(String cmdline) {
		if (cmdline == null || (cmdline = cmdline.trim()).isEmpty()) {
			return Collections.emptyList();
		}
		String[] split = CMDLINE_SPLITTER.split(cmdline);
		for (int i = 0; i < split.length; i++) {
			split[i] = split[i].replace("\\,", ",").trim();
		}
		return Arrays.asList(split);
	}

	private Class<?> testRunnerClass;
	private Method testMethod;

	/**
	 * Creates a new testing invoker.
	 */
	public ReflectionJavaTestInvoker() {
	}

	@Override
	public void initTestRunner(ClassLoader testrunnerclassloader, TestInvokerParameters parameters)
			throws JavaTestRunnerFailureException {
		super.initTestRunner(testrunnerclassloader, parameters);
		String testrunnerclass = parameters.get(PARAMETER_TEST_RUNNER_CLASS);
		if (testrunnerclass == null) {
			throw new JavaTestRunnerFailureException("Parameter " + PARAMETER_TEST_RUNNER_CLASS + " is missing.");
		}
		try {
			this.testRunnerClass = Class.forName(testrunnerclass, false, getTestRunnerClassLoader());
		} catch (ClassNotFoundException e) {
			throw new JavaTestRunnerFailureException("Test runner class not found: " + testrunnerclass, e);
		}
		try {
			testMethod = testRunnerClass.getDeclaredMethod("main", String[].class);
			if (!Modifier.isStatic(testMethod.getModifiers())) {
				throw new JavaTestRunnerFailureException("Test invoker method is not static: " + testMethod);
			}
		} catch (NoSuchMethodException | SecurityException e) {
			throw new JavaTestRunnerFailureException("Test invoker method not found: " + testMethod, e);
		}
	}

	/**
	 * Gets the class of the test runner.
	 * <p>
	 * This class contains the {@linkplain #getTestMethod() test method}.
	 * 
	 * @return The test runner class.
	 */
	protected final Class<?> getTestRunnerClass() {
		return testRunnerClass;
	}

	/**
	 * Gets the test method.
	 * <p>
	 * This method is called for each test case, and has a single parameter with the type of <code>String[]</code>.
	 * 
	 * @return The test method.
	 */
	protected final Method getTestMethod() {
		return testMethod;
	}

	@Override
	protected void runTest(TestInvocationParameters parameters)
			throws JavaTestRunnerFailureException, InvocationTargetException {
		String testclassname = parameters.getTestClassName();
		List<String> prefixargs = splitCommandLine(parameters.get(PARAMETER_PREFIX_ARGUMENTS, ""));
		List<String> suffixargs = splitCommandLine(parameters.get(PARAMETER_SUFFIX_ARGUMENTS, ""));
		boolean hasargs = !prefixargs.isEmpty() || !suffixargs.isEmpty();
		Object[] callargs;
		if (hasargs) {
			List<String> args = new ArrayList<>(prefixargs.size() + 1 + suffixargs.size());
			args.addAll(prefixargs);
			args.add(testclassname);
			args.addAll(suffixargs);
			callargs = new Object[] { args.toArray(new String[args.size()]) };
		} else {
			callargs = new Object[] { new String[] { testclassname } };
		}
		try {
			testMethod.invoke(null, callargs);
		} catch (IllegalAccessException e) {
			throw new JavaTestRunnerFailureException("Failed to call test runner method: " + testMethod, e);
		}
	}
}
