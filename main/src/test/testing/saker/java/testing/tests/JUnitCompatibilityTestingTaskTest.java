package testing.saker.java.testing.tests;

import testing.saker.SakerTest;
import testing.saker.java.testing.JavaTestingVariablesMetricEnvironmentTestCase;

@SakerTest
public class JUnitCompatibilityTestingTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {
	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.MainTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.MainTest"));

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());

		files.putFile(SRC_PATH_BASE.resolve("test/ClassPathMain.java"), files
				.getAllBytes(SRC_PATH_BASE.resolve("test/ClassPathMain.java")).toString().replace("@SecondAnnot", ""));
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.MainTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.MainTest"));

		files.putFile(SRC_PATH_BASE.resolve("test/NewClass.java"),
				"package test; @TestAnnot public class NewClass { }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());

		System.out.println("JUnitCompatibilityTestingTaskTest.runJavacTestImpl() SEPARATE");

		files.putFile(PATH_WORKING_DIRECTORY.resolve("tests/test/FailTest.java"),
				"package test;  public class FailTest { @org.junit.Test public void fail() { throw new RuntimeException(); } }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.FailTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf());
		assertEquals(getMetric().getFailedTests(), setOf("test.FailTest"));

	}

}
