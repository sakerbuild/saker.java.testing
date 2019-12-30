package testing.saker.java.testing.tests;

import testing.saker.SakerTest;
import testing.saker.java.testing.JavaTestingVariablesMetricEnvironmentTestCase;

@SakerTest
public class ClassPathTestingTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {

	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main", "test.CPMainConsumer", "test.Main$Sub"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main", "test.CPMainConsumer", "test.Main$Sub"));

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());

		files.putFile(SRC_PATH_BASE.resolve("test/ClassPathMain.java"), files
				.getAllBytes(SRC_PATH_BASE.resolve("test/ClassPathMain.java")).toString().replace("@SecondAnnot", ""));
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.CPMainConsumer"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.CPMainConsumer"));
	}

}
