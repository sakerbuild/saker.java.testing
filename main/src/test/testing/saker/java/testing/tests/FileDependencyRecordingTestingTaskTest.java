package testing.saker.java.testing.tests;

import java.nio.file.Paths;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.java.testing.JavaTestingVariablesMetricEnvironmentTestCase;

@SakerTest
public class FileDependencyRecordingTestingTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {

	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		runScriptTask("build");

		SakerPath mirrorwd = getMirrorDirectory().resolve("wd_");

		SakerPath filetxt = PATH_WORKING_DIRECTORY.resolve(Paths.get("nonexistent/path/to/file.txt"));
		SakerPath jarfilefile = PATH_WORKING_DIRECTORY.resolve("nonexistent/path/to/archive.jar");
		SakerPath zipfilefile = PATH_WORKING_DIRECTORY.resolve("nonexistent/path/to/archive.zip");
		SakerPath zipfilepath = PATH_WORKING_DIRECTORY.resolve(Paths.get("nonexistent/path/to/archive.zip"));
		SakerPath uripath = PATH_WORKING_DIRECTORY.resolve("nonexistent/path/to/archive.zip");

		SakerPath listdirpath = PATH_WORKING_DIRECTORY.resolve(Paths.get("listing/subdir"));

		//do not compare for equality, as some service loaders add service file dependencies

		assertTrue(getMetric().getTestReferencedFiles("test.FileInputStreamTest").contains(filetxt));
		assertTrue(getMetric().getTestReferencedFiles("test.FileOutputStreamTest").contains(filetxt));

		assertTrue(getMetric().getTestReferencedFiles("test.FilesNewInputStreamTest").contains(filetxt));
		//do not assert this, as newOutputStream might delegate to newByteChannel which adds a read dependency
		//assertTrue(getMetric().getTestReferencedFiles("test.FilesNewOutputStreamTest"), setOf());

		assertTrue(getMetric().getTestReferencedFiles("test.MainInvokeTest").contains(filetxt));

		assertTrue(getMetric().getTestReferencedFiles("test.RandomAccessFileTest").contains(filetxt));

		assertTrue(getMetric().getTestReferencedFiles("test.ZipFileTest").contains(zipfilefile));
		assertTrue(getMetric().getTestReferencedFiles("test.JarFileTest").contains(jarfilefile));
		assertTrue(getMetric().getTestReferencedFiles("test.ZipFsURITest").contains(uripath));

		assertTrue(getMetric().getTestReferencedFiles("test.ZipFsPathTest").contains(zipfilefile));

		assertTrue(getMetric().getTestReferencedDirectories("test.FileDirectoryListTest").contains(listdirpath));
		assertTrue(getMetric().getTestReferencedDirectories("test.FullPathDirectoryListTest").contains(listdirpath));
		assertFalse(
				getMetric().getTestReferencedDirectories("test.PartiallyPathDirectoryListTest").contains(listdirpath));
	}

}
