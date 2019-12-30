package testing.saker.java.testing.tests;

import testing.saker.SakerTest;
import testing.saker.java.testing.JavaTestingVariablesMetricEnvironmentTestCase;

@SakerTest
public class TestRunnerInitializationFailureTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {

	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		assertTaskException("saker.java.testing.api.test.exc.JavaTestingFailedException", () -> runScriptTask("build"));
	}

}
