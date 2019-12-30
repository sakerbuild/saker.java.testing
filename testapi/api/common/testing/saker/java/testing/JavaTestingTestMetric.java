package testing.saker.java.testing;

import java.util.Set;

import saker.build.file.path.SakerPath;

@SuppressWarnings("unused")
public interface JavaTestingTestMetric {
	public default void javaTestSuccessful(String classname) {
	}

	public default void javaTestFailed(String classname) {
	}

	public default void javaTestInvocation(String classname) {
	}

	public default void javaTestReferencedFiles(String classname, Set<SakerPath> files) {
	}

	public default void javaTestReferencedDirectories(String classname, Set<SakerPath> directories) {
	}

	public default void javaTestDependentClasses(String classname, Set<String> directories) {
	}
}
