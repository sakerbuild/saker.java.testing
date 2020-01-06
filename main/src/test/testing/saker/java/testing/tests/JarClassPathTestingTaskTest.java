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
package testing.saker.java.testing.tests;

import java.util.Map;

import saker.build.task.TaskFactory;
import saker.build.task.TaskName;
import saker.build.thirdparty.saker.util.ObjectUtils;
import testing.saker.SakerTest;
import testing.saker.java.testing.JavaTestingVariablesMetricEnvironmentTestCase;
import testing.saker.java.testing.TestJarCreatingTaskFactory;
import testing.saker.java.testing.TestingCollectingTestMetric;

@SakerTest
public class JarClassPathTestingTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {
	//uses the same contents as the JUnit compatibility test, but uses jars as classpaths to the testing API

	@Override
	protected TestingCollectingTestMetric createMetricImpl() {
		TestingCollectingTestMetric result = super.createMetricImpl();
		Map<TaskName, TaskFactory<?>> injectedfactories = ObjectUtils.newTreeMap(result.getInjectedTaskFactories());
		injectedfactories.put(TaskName.valueOf("test.jar.create"), new TestJarCreatingTaskFactory());
		result.setInjectedTaskFactories(injectedfactories);
		return result;
	}

	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.MainTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.MainTest"));

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());

		//clear the cached JVM for consistent classloader resource requests during initialization
		environment.clearCachedDatasWaitExecutions();

		files.putFile(SRC_PATH_BASE.resolve("test/ClassPathMain.java"), files
				.getAllBytes(SRC_PATH_BASE.resolve("test/ClassPathMain.java")).toString().replace("@SecondAnnot", ""));
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.MainTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.MainTest"));

		environment.clearCachedDatasWaitExecutions();

		files.putFile(SRC_PATH_BASE.resolve("test/NewClass.java"),
				"package test; @TestAnnot public class NewClass { }");
		runScriptTask("build");
		//the tests are reinvoked, because the class path jar changed
		//but only because the test requested a classloader resource from it during initialization
		//    JVM internals request some classloader resources to initialize some classes
		//in general, the test shouldn't be reinvoked, as the classes accessed by MainTest didn't change
		//this behaviour may be different on different JDKs
		//    therefore we 'if' on the invoked tests and perform the assertion if non-empty 
		if (!getMetric().getInvokedTests().isEmpty()) {
			assertEquals(getMetric().getInvokedTests(), setOf("test.MainTest"));
			assertEquals(getMetric().getSuccessfulTests(), setOf("test.MainTest"));
		}

		System.out.println("JUnitCompatibilityTestingTaskTest.runJavacTestImpl() SEPARATE");

		files.putFile(PATH_WORKING_DIRECTORY.resolve("tests/test/FailTest.java"),
				"package test;  public class FailTest { @org.junit.Test public void fail() { throw new RuntimeException(); } }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.FailTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf());
		assertEquals(getMetric().getFailedTests(), setOf("test.FailTest"));

	}
}
