package saker.java.testing.api.test.invoker;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.NavigableMap;
import java.util.NavigableSet;

import saker.build.thirdparty.saker.util.io.SerialUtils;

/**
 * Data class holding the result of a Java test invocation.
 * <p>
 * Clients should use the appropriate setter methods to initialize the results.
 * <p>
 * The test invocation results provides information about the enumerated file system directories. This is in order for
 * the build task to be able to track the expected file contents of a directory. Note that the class doesn't include
 * information about the accessed files. The file accesses by the test case are tracked by the testing instrumentation
 * on-the-fly.
 */
public final class JavaTestInvocationResult implements Externalizable {
	private static final long serialVersionUID = 1L;

	private NavigableSet<String> dependentClasses;
	private NavigableMap<String, ? extends NavigableSet<String>> listedDirectories;
	private Integer exitCode = null;
	private boolean successful;
	private String failureInformation;

	/**
	 * For {@link Externalizable}.
	 */
	public JavaTestInvocationResult() {
	}

	/**
	 * Creates a new instance and sets the result of the test.
	 * 
	 * @param successful
	 *            <code>true</code> if the testing was successful.
	 */
	public JavaTestInvocationResult(boolean successful) {
		this.successful = successful;
	}

	/**
	 * Sets the class dependencies of the test case.
	 * 
	 * @param dependentClasses
	 *            The class dependencies. The elements should be the <i>internal names</i> of the classes.
	 */
	public void setDependentClasses(NavigableSet<String> dependentClasses) {
		this.dependentClasses = dependentClasses;
	}

	/**
	 * Gets the class dependent classes that the test case uses.
	 * <p>
	 * The result contains the <i>internal names</i> of the classes.
	 * 
	 * @return The set of class internal names or <code>null</code> if not set.
	 */
	public NavigableSet<String> getDependentClasses() {
		return dependentClasses;
	}

	/**
	 * Sets the listed directories by the test case.
	 * 
	 * @param listedDirectories
	 *            The listed directories.
	 * @see #getListedDirectories()
	 */
	public void setListedDirectories(NavigableMap<String, ? extends NavigableSet<String>> listedDirectories) {
		this.listedDirectories = listedDirectories;
	}

	/**
	 * Gets the listed directories by the test case.
	 * <p>
	 * The returned map contains the directory paths as keys mapped to the enumerated file names by the test case.
	 * 
	 * @return The listed directories or <code>null</code> if not set.
	 */
	public NavigableMap<String, ? extends NavigableSet<String>> getListedDirectories() {
		return listedDirectories;
	}

	/**
	 * Sets the test result.
	 * 
	 * @param successful
	 *            <code>true</code> if the test was successful.
	 * @see #isSuccessful()
	 */
	public void setSuccessful(boolean successful) {
		this.successful = successful;
	}

	/**
	 * Gets if the test execution was successful.
	 * <p>
	 * If the test case wasn't set as successful, it may be still considered as succeeded if the
	 * {@linkplain #getExitCode() exit code} is accepted by the test build task configuration.
	 * 
	 * @return <code>true</code> if the test was successful.
	 */
	public boolean isSuccessful() {
		return successful;
	}

	/**
	 * Sets the test exit code.
	 * 
	 * @param exitCode
	 *            The exit code.
	 * @see #getExitCode()
	 */
	public void setExitCode(Integer exitCode) {
		this.exitCode = exitCode;
	}

	/**
	 * Gets the exit code of the test case.
	 * <p>
	 * The exit code may be set to non-<code>null</code> to signal that the test exited with an exit code. This is
	 * usually done if the test runner calls {@link System#exit(int)}, and the instrumentation catches this call rather
	 * than killing the JVM process.
	 * <p>
	 * If the test is set to {@linkplain #isSuccessful() failed}, and the tester build task was configured to accept the
	 * test exit code, then the test fill be considered as successful.
	 * 
	 * @return The test exit code or <code>null</code> if not none.
	 */
	public Integer getExitCode() {
		return exitCode;
	}

	/**
	 * Sets the textual failure information of the test.
	 * 
	 * @param failureInformation
	 *            The failure information.
	 * @see #getFailureInformation()
	 */
	public void setFailureInformation(String failureInformation) {
		this.failureInformation = failureInformation;
	}

	/**
	 * Gets the test failure information.
	 * <p>
	 * The failure information contains arbitrary string contents about the test case that can be displayed to the user
	 * if the test failed. It's usually the standard out and error of the test case.
	 * 
	 * @return The information or <code>null</code>.
	 */
	public String getFailureInformation() {
		return failureInformation;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, dependentClasses);
		SerialUtils.writeExternalMap(out, listedDirectories, ObjectOutput::writeUTF,
				SerialUtils::writeExternalCollection);
		out.writeObject(exitCode);
		out.writeBoolean(successful);
		out.writeObject(failureInformation);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		dependentClasses = SerialUtils.readExternalSortedTreeSet(in);
		listedDirectories = SerialUtils.readExternalSortedTreeMap(in, ObjectInput::readUTF,
				SerialUtils::readExternalSortedTreeSet);
		exitCode = (Integer) in.readObject();
		successful = in.readBoolean();
		failureInformation = (String) in.readObject();
	}

}
