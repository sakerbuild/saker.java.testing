package testing.saker.java.testing.tests;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.java.testing.JavaTestingVariablesMetricEnvironmentTestCase;

@SakerTest
public class ReflectionAccessTestingTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {

	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		SakerPath cpmainpath = SRC_PATH_BASE.resolve("test/ClassPathMain.java");
		SakerPath cpmainaddedpath = SRC_PATH_BASE.resolve("test/ClassPathMainAdded.java");
		SakerPath cpmainaddedclpath = SRC_PATH_BASE.resolve("test/ClassPathMainAddedCL.java");
		SakerPath cpmainaddedloadclpath = SRC_PATH_BASE.resolve("test/ClassPathMainLoadCL.java");

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.LdcMain", "test.LdcArrayMain", "test.ForNameMain",
				"test.ClassNotFoundMain", "test.ClassCLNotFoundMain", "test.LoadCLNotFoundMain"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.LdcMain", "test.LdcArrayMain", "test.ForNameMain"));
		assertEquals(getMetric().getFailedTests(),
				setOf("test.ClassNotFoundMain", "test.ClassCLNotFoundMain", "test.LoadCLNotFoundMain"));

		files.putFile(cpmainpath, files.getAllBytes(cpmainpath).toString().replace("/*field-here*/", "int i;"));
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.LdcMain", "test.LdcArrayMain", "test.ForNameMain"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.LdcMain", "test.LdcArrayMain", "test.ForNameMain"));
		assertEquals(getMetric().getFailedTests(),
				setOf("test.ClassNotFoundMain", "test.ClassCLNotFoundMain", "test.LoadCLNotFoundMain"));

		files.putFile(cpmainaddedpath, "package test; public class ClassPathMainAdded {  }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.ClassNotFoundMain"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.ClassNotFoundMain"));
		assertEquals(getMetric().getFailedTests(), setOf("test.ClassCLNotFoundMain", "test.LoadCLNotFoundMain"));

		files.putFile(cpmainaddedclpath, "package test; public class ClassPathMainAddedCL {  }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.ClassCLNotFoundMain"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.ClassCLNotFoundMain"));
		assertEquals(getMetric().getFailedTests(), setOf("test.LoadCLNotFoundMain"));

		files.putFile(cpmainaddedloadclpath, "package test; public class ClassPathMainLoadCL {  }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.LoadCLNotFoundMain"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.LoadCLNotFoundMain"));
		assertEquals(getMetric().getFailedTests(), setOf());
	}

}
