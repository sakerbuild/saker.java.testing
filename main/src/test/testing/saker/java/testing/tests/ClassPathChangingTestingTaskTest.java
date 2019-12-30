package testing.saker.java.testing.tests;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.java.testing.JavaTestingVariablesMetricEnvironmentTestCase;

@SakerTest
public class ClassPathChangingTestingTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {

	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("tests.MainTest", "tests.ThrowingTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("tests.MainTest"));
		assertEquals(getMetric().getFailedTests(), setOf("tests.ThrowingTest"));

		System.out.println("ClassPathChangingTestingTaskTest.runJavacTestImpl() 1");

		//nothing is run
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());
		assertEquals(getMetric().getFailedTests(), setOf());

		System.out.println("ClassPathChangingTestingTaskTest.runJavacTestImpl() 2");

		//modify the test runner class path, add a class to it
		SakerPath traddedpath = PATH_WORKING_DIRECTORY.resolve("testrunner/testrunner/TRAdded.java");
		files.putFile(traddedpath, "package testrunner; public class TRAdded { } ");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("tests.MainTest", "tests.ThrowingTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("tests.MainTest"));
		assertEquals(getMetric().getFailedTests(), setOf("tests.ThrowingTest"));

		System.out.println("ClassPathChangingTestingTaskTest.runJavacTestImpl() 3");

		//add a new class to the user class path
		//the failed test is logged again
		SakerPath useraddedpath = PATH_WORKING_DIRECTORY.resolve("src/test/UserAdded.java");
		files.putFile(useraddedpath,
				"package test; public class UserAdded { public static void main(String[] args) { } } ");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());
		assertEquals(getMetric().getFailedTests(), setOf("tests.ThrowingTest"));

		System.out.println("ClassPathChangingTestingTaskTest.runJavacTestImpl() 4");

		//add a new test for the added class
		SakerPath useraddedtestpath = PATH_WORKING_DIRECTORY.resolve("tests/tests/UserAddedTest.java");
		files.putFile(useraddedtestpath,
				"package tests; public class UserAddedTest { public static void main(String[] args) { test.UserAdded.main(args); } } ");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("tests.UserAddedTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("tests.UserAddedTest"));
		assertEquals(getMetric().getFailedTests(), setOf("tests.ThrowingTest"));

		System.out.println("ClassPathChangingTestingTaskTest.runJavacTestImpl() 5");

		//remove the user added test
		files.delete(useraddedtestpath);
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());
		assertEquals(getMetric().getFailedTests(), setOf("tests.ThrowingTest"));

		//remove the user added class file
		files.delete(useraddedpath);
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());
		assertEquals(getMetric().getFailedTests(), setOf("tests.ThrowingTest"));

		//remove the test runner added class, the test runner classpath is changed
		files.delete(traddedpath);
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("tests.MainTest", "tests.ThrowingTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("tests.MainTest"));
		assertEquals(getMetric().getFailedTests(), setOf("tests.ThrowingTest"));
	}

}
