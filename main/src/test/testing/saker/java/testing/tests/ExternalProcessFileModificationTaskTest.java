package testing.saker.java.testing.tests;

import testing.saker.SakerTest;
import testing.saker.java.testing.JavaTestingVariablesMetricEnvironmentTestCase;

@SakerTest
public class ExternalProcessFileModificationTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase{

	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		// TODO Auto-generated method stub
		runScriptTask("build");
	}

}
