package testing.saker.java.testing.tests;

import testing.saker.SakerTest;
import testing.saker.java.testing.JavaTestingVariablesMetricEnvironmentTestCase;

@SakerTest
public class FileDependencyModifyTestingTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {
	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.FileOpenTest", "test.DirectoryListTest",
				"test.DirectoryDeleteTest", "test.IgnoredFileOpenTest"));
		assertEquals(getMetric().getSuccessfulTests(),
				setOf("test.FileOpenTest", "test.DirectoryListTest", "test.IgnoredFileOpenTest"));
		assertEquals(getMetric().getFailedTests(), setOf("test.DirectoryDeleteTest"));

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("input.txt"), "modified");
		files.delete(PATH_WORKING_DIRECTORY.resolve("deldirectory/a.txt"));
		runScriptTask("build");
		//DirectoryDeleteTest is not rerun, as it doesn't check for the contents of the directory deleted
		//    this is desired behaviour, as when a test deletes a directory, it should gracefully handle if the directory is not empty
		//    therefore, it should enumerate its contents, and delete them accordingly beforehand
		//    if it does that, then the directory enumeration dependency will be recorded, and the test will be rerun as expected
		assertEquals(getMetric().getInvokedTests(), setOf("test.FileOpenTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf());
		assertEquals(getMetric().getFailedTests(), setOf("test.FileOpenTest", "test.DirectoryDeleteTest"));

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("listing/c.txt"), "c");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.DirectoryListTest"));
		assertEquals(getMetric().getSuccessfulTests(), setOf());
		assertEquals(getMetric().getFailedTests(),
				setOf("test.DirectoryListTest", "test.FileOpenTest", "test.DirectoryDeleteTest"));

		files.putFile(PATH_WORKING_DIRECTORY.resolve("ignored/ignored.txt"), "modified");
		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdResults());

	}
}
