package testing.saker.java.testing;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

import saker.build.file.path.SakerPath;
import testing.saker.build.tests.CollectingTestMetric;
import testing.saker.java.testing.JavaTestingTestMetric;

public class TestingCollectingTestMetric extends CollectingTestMetric implements JavaTestingTestMetric {
	protected Set<String> invokedTests = new ConcurrentSkipListSet<>();
	protected Set<String> successfulTests = new ConcurrentSkipListSet<>();
	protected Set<String> failedTests = new ConcurrentSkipListSet<>();

	protected Map<String, Set<SakerPath>> testReferencedFiles = new ConcurrentSkipListMap<>();
	protected Map<String, Set<SakerPath>> testReferencedDirectories = new ConcurrentSkipListMap<>();
	protected Map<String, Set<String>> testDependentClasses = new ConcurrentSkipListMap<>();

	@Override
	public void javaTestSuccessful(String classname) {
		this.successfulTests.add(classname);
	}

	@Override
	public void javaTestFailed(String classname) {
		this.failedTests.add(classname);
	}

	@Override
	public void javaTestInvocation(String classname) {
		this.invokedTests.add(classname);
	}

	@Override
	public void javaTestReferencedFiles(String classname, Set<SakerPath> files) {
		this.testReferencedFiles.put(classname, files);
	}

	@Override
	public void javaTestReferencedDirectories(String classname, Set<SakerPath> directories) {
		this.testReferencedDirectories.put(classname, directories);
	}

	@Override
	public void javaTestDependentClasses(String classname, Set<String> directories) {
		this.testDependentClasses.put(classname, new TreeSet<>(directories));
	}

	public Set<String> getInvokedTests() {
		return invokedTests;
	}

	public Set<String> getSuccessfulTests() {
		return successfulTests;
	}

	public Set<String> getFailedTests() {
		return failedTests;
	}

	public Map<String, Set<SakerPath>> getTestReferencedFiles() {
		return testReferencedFiles;
	}

	public Map<String, Set<SakerPath>> getTestReferencedDirectories() {
		return testReferencedDirectories;
	}

	public Set<SakerPath> getTestReferencedFiles(String classname) {
		Set<SakerPath> result = testReferencedFiles.get(classname);
		if (result == null) {
			throw new IllegalArgumentException("Test not found with class name: " + classname);
		}
		return result;
	}

	public Set<SakerPath> getTestReferencedDirectories(String classname) {
		Set<SakerPath> result = testReferencedDirectories.get(classname);
		if (result == null) {
			throw new IllegalArgumentException("Test not found with class name: " + classname);
		}
		return result;
	}

	public Map<String, Set<String>> getTestDependentClasses() {
		return testDependentClasses;
	}

	public Set<String> getTestDependentClasses(String classname) {
		Set<String> result = testDependentClasses.get(classname);
		if (result == null) {
			throw new IllegalArgumentException("Test not found with class name: " + classname);
		}
		return result;
	}
}
