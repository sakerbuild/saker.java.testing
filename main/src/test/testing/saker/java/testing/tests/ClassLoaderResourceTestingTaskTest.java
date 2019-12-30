package testing.saker.java.testing.tests;

import testing.saker.SakerTest;
import testing.saker.java.testing.JavaTestingVariablesMetricEnvironmentTestCase;

@SakerTest
public class ClassLoaderResourceTestingTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {

	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		files.createDirectories(PATH_WORKING_DIRECTORY.resolve("resources"));

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/stream/resource"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/url/resource"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/url/resources"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/abs/resource"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/abs/url/resource"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/test/class/resource"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/test/class/url/resource"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

	}

}
