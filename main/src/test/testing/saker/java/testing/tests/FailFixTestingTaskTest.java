package testing.saker.java.testing.tests;

import testing.saker.java.testing.JavaTestingVariablesMetricEnvironmentTestCase;

/**
 * To make sure all previously failfast skipped tests are rerun in a next build.
 */
//@SakerTest
public class FailFixTestingTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {
	//TODO this test is temporarily disabled as with the introducton of the inner task test execution, we don't have a way to reliably
	//     ensure that the LongTest doesn't get invoked when the test base is modified
	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.LongTest", "test.ShortTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.LongTest", "test.ShortTest"));

		//modify test base to trigger all tests rerun
		files.putFile(SRC_PATH_BASE.resolve("test", "TestBase.java"), "package test; public class TestBase { int i; }");
		files.putFile(SRC_PATH_BASE.resolve("test", "ShortTest.java"),
				"package test; public class ShortTest extends TestBase { @org.junit.Test public void test() { throw new RuntimeException(); } }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.ShortTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf());
		assertEquals(getMetric().getFailedTests(), setOf("test.ShortTest"));

		files.putFile(SRC_PATH_BASE.resolve("test", "ShortTest.java"),
				"package test; public class ShortTest extends TestBase { @org.junit.Test public void test() {  } }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.LongTest", "test.ShortTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.LongTest", "test.ShortTest"));

	}

}
