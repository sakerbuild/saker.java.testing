package saker.java.testing.impl.test;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;

import saker.build.file.SakerFile;
import saker.build.file.content.ContentDescriptor;
import saker.build.file.path.SakerPath;
import saker.std.api.file.location.FileLocation;
import saker.build.thirdparty.saker.util.io.SerialUtils;

public class IncrementalTestingInfo implements Externalizable {
	private static final long serialVersionUID = 1L;

	public static enum TestCaseState {
		SUCCESSFUL,
		FAILED,
		NEW;
	}

	public final static class ReferencedFilePath implements Comparable<ReferencedFilePath>, Externalizable {
		private static final long serialVersionUID = 1L;

		public static final String LOCATION_EXECUTION = "exec";
		public static final String LOCATION_LOCAL = "local";

		private String location;
		private SakerPath path;

		/**
		 * For {@link Externalizable}.
		 */
		public ReferencedFilePath() {
		}

		public ReferencedFilePath(String location, SakerPath path) {
			this.location = location;
			this.path = path;
		}

		public String getLocation() {
			return location;
		}

		public SakerPath getPath() {
			return path;
		}

		@Override
		public int compareTo(ReferencedFilePath o) {
			int cmp = location.compareTo(o.location);
			if (cmp != 0) {
				return cmp;
			}
			return path.compareTo(o.path);
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeUTF(location);
			out.writeObject(path);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			location = in.readUTF();
			path = (SakerPath) in.readObject();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((location == null) ? 0 : location.hashCode());
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ReferencedFilePath other = (ReferencedFilePath) obj;
			if (location == null) {
				if (other.location != null)
					return false;
			} else if (!location.equals(other.location))
				return false;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[" + (location != null ? "location=" + location + ", " : "")
					+ (path != null ? "path=" + path : "") + "]";
		}

	}

	public static class IncrementalTestCaseResult implements Externalizable {
		private static final long serialVersionUID = 1L;

		private String className;
		private NavigableSet<String> dependentClasses;
		private ReferencedFilePath classFilePath;
		private TestCaseState state = TestCaseState.NEW;
		private String failInformation;
		private long executionMilliSeconds = -1;

		private NavigableMap<SakerPath, ? extends ContentDescriptor> referencedFiles;
		private NavigableMap<SakerPath, ? extends NavigableSet<String>> referencedDirectories;
		private NavigableMap<String, ? extends NavigableMap<ReferencedFilePath, ? extends ContentDescriptor>> referencedClassLoaderResources;

		/**
		 * For {@link Externalizable}.
		 */
		public IncrementalTestCaseResult() {
		}

		public IncrementalTestCaseResult(String className, ReferencedFilePath classFilePath,
				long executionMilliSeconds) {
			this.className = className;
			this.classFilePath = classFilePath;
			this.executionMilliSeconds = executionMilliSeconds;
		}

		public void setDependentClasses(NavigableSet<String> dependentClasses) {
			this.dependentClasses = dependentClasses;
		}

		public void setReferencedClassLoaderResources(
				NavigableMap<String, ? extends NavigableMap<ReferencedFilePath, ? extends ContentDescriptor>> accessedClassLoaderResources) {
			this.referencedClassLoaderResources = accessedClassLoaderResources;
		}

		public void setReferencedFiles(NavigableMap<SakerPath, ? extends ContentDescriptor> referencedFiles) {
			this.referencedFiles = referencedFiles;
		}

		public void setReferencedDirectories(
				NavigableMap<SakerPath, ? extends NavigableSet<String>> referencedDirectories) {
			this.referencedDirectories = referencedDirectories;
		}

		public void setState(TestCaseState state) {
			this.state = state;
		}

		public NavigableMap<SakerPath, ? extends ContentDescriptor> getReferencedFiles() {
			return referencedFiles;
		}

		public NavigableMap<String, ? extends NavigableMap<ReferencedFilePath, ? extends ContentDescriptor>> getReferencedClassLoaderResources() {
			return referencedClassLoaderResources;
		}

		public NavigableMap<SakerPath, ? extends NavigableSet<String>> getReferencedDirectories() {
			return referencedDirectories;
		}

		public ReferencedFilePath getClassFilePath() {
			return classFilePath;
		}

		public String getClassName() {
			return className;
		}

		public NavigableSet<String> getDependentClassNames() {
			return dependentClasses;
		}

		public boolean isSuccessful() {
			return state == TestCaseState.SUCCESSFUL;
		}

		public TestCaseState getState() {
			return state;
		}

		public void setFailureInformation(String failInformation) {
			this.failInformation = failInformation;
		}

		public String getFailInformation() {
			return failInformation;
		}

		public long getExecutionMilliSeconds() {
			return executionMilliSeconds;
		}

		public void setExecutionMilliSeconds(long milliSeconds) {
			this.executionMilliSeconds = milliSeconds;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeUTF(className);
			SerialUtils.writeExternalCollection(out, dependentClasses);
			SerialUtils.writeExternalMap(out, referencedFiles);
			SerialUtils.writeExternalMap(out, referencedDirectories);
			SerialUtils.writeExternalMap(out, referencedClassLoaderResources, ObjectOutput::writeObject,
					SerialUtils::writeExternalMap);
			out.writeObject(classFilePath);
			out.writeObject(state);

			out.writeObject(failInformation);
			out.writeLong(executionMilliSeconds);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			className = in.readUTF();
			dependentClasses = SerialUtils.readExternalSortedImmutableNavigableSet(in);
			referencedFiles = SerialUtils.readExternalSortedImmutableNavigableMap(in);
			referencedDirectories = SerialUtils.readExternalSortedImmutableNavigableMap(in);
			referencedClassLoaderResources = SerialUtils.readExternalSortedImmutableNavigableMap(in,
					SerialUtils::readExternalObject, SerialUtils::readExternalImmutableNavigableMap);
			classFilePath = (ReferencedFilePath) in.readObject();
			state = (TestCaseState) in.readObject();

			failInformation = (String) in.readObject();
			executionMilliSeconds = in.readLong();
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[" + getClassName() + ":" + state + "]";
		}
	}

	public static class ClassInfo implements Externalizable {
		private static final long serialVersionUID = 1L;

		private String className;
		private ContentDescriptor contentDescriptor;

		public ClassInfo() {
		}

		public ClassInfo(String className, ContentDescriptor contentDescriptor) {
			this.className = className;
			this.contentDescriptor = contentDescriptor;
		}

		public String getClassName() {
			return className;
		}

		public ContentDescriptor getContentDescriptor() {
			return contentDescriptor;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeUTF(className);
			out.writeObject(contentDescriptor);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			className = in.readUTF();
			contentDescriptor = (ContentDescriptor) in.readObject();
		}

		@Override
		public String toString() {
			return "ClassInfo [" + (className != null ? "className=" + className + ", " : "")
					+ (contentDescriptor != null ? "contentDescriptor=" + contentDescriptor : "") + "]";
		}
	}

	//TODO clean up unused members by task implementation

	private NavigableMap<ReferencedFilePath, IncrementalTestCaseResult> testCasesByPath = new ConcurrentSkipListMap<>();
	private NavigableMap<SakerPath, ClassInfo> classContentsByPath = new ConcurrentSkipListMap<>();

	private Map<? extends FileLocation, ? extends ContentDescriptor> testRunnerClassPathFiles = Collections.emptyMap();

	private NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> userClassPathFiles = Collections
			.emptyNavigableMap();
	private NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> testClassPathFiles = Collections
			.emptyNavigableMap();
	private NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> dependencyClassPathFiles = Collections
			.emptyNavigableMap();

	public IncrementalTestingInfo() {
	}

	private static void setClassPathFiles(Iterable<? extends SakerFile> files, Set<SakerPath> filesset) {
		for (SakerFile f : files) {
			filesset.add(f.getSakerPath());
		}
	}

	private static boolean isClassPathFilesChanged(Iterable<? extends SakerFile> files,
			NavigableSet<SakerPath> filesset) {
		for (SakerFile f : files) {
			if (!filesset.contains(f.getSakerPath())) {
				return true;
			}
		}
		return false;
	}

	public void setTestRunnerClassPathFiles(Map<FileLocation, ContentDescriptor> files) {
		this.testRunnerClassPathFiles = files;
	}

	public NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> getUserClassPathFiles() {
		return userClassPathFiles;
	}

	public void setUserClassPathFiles(
			NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> userClassPathFiles) {
		this.userClassPathFiles = userClassPathFiles;
	}

	public NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> getTestClassPathFiles() {
		return testClassPathFiles;
	}

	public void setTestClassPathFiles(
			NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> testClassPathFiles) {
		this.testClassPathFiles = testClassPathFiles;
	}

	public NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> getDependencyClassPathFiles() {
		return dependencyClassPathFiles;
	}

	public void setDependencyClassPathFiles(
			NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> dependencyClassPathFiles) {
		this.dependencyClassPathFiles = dependencyClassPathFiles;
	}

	public boolean isTestRunnerClassPathFilesChanged(Map<? extends FileLocation, ? extends ContentDescriptor> files) {
		return !Objects.equals(files, testRunnerClassPathFiles);
	}

	public boolean isAnyTestFailed() {
		for (IncrementalTestCaseResult tc : testCasesByPath.values()) {
			if (!tc.isSuccessful()) {
				return true;
			}
		}
		return false;
	}

	public Collection<String> getUnsuccessfulTestClassNames() {
		TreeSet<String> result = new TreeSet<>();
		for (IncrementalTestCaseResult tc : testCasesByPath.values()) {
			if (!tc.isSuccessful()) {
				result.add(tc.getClassName());
			}
		}
		return result;
	}

	public NavigableMap<ReferencedFilePath, IncrementalTestCaseResult> getTestCasesByPath() {
		return testCasesByPath;
	}

	public NavigableMap<SakerPath, ClassInfo> getClassContentsByPath() {
		return classContentsByPath;
	}

	public void addTestCase(IncrementalTestCaseResult testcase) {
		testCasesByPath.put(testcase.classFilePath, testcase);
	}

	public void addClassContent(SakerPath path, ContentDescriptor filecontent, String classname) {
		this.classContentsByPath.put(path, new ClassInfo(classname, filecontent));
	}

	public int getSuccessfulTestCount() {
		int c = 0;
		for (IncrementalTestCaseResult tcres : testCasesByPath.values()) {
			if (tcres.isSuccessful()) {
				++c;
			}
		}
		return c;
	}

	public int getTestCount() {
		return testCasesByPath.size();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalMap(out, testCasesByPath);
		SerialUtils.writeExternalMap(out, classContentsByPath);

		SerialUtils.writeExternalMap(out, testRunnerClassPathFiles);
		SerialUtils.writeExternalMap(out, userClassPathFiles);
		SerialUtils.writeExternalMap(out, testClassPathFiles);
		SerialUtils.writeExternalMap(out, dependencyClassPathFiles);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		testCasesByPath = SerialUtils.readExternalSortedImmutableNavigableMap(in);
		classContentsByPath = SerialUtils.readExternalSortedImmutableNavigableMap(in);

		testRunnerClassPathFiles = SerialUtils.readExternalImmutableLinkedHashMap(in);
		userClassPathFiles = SerialUtils.readExternalSortedImmutableNavigableMap(in);
		testClassPathFiles = SerialUtils.readExternalSortedImmutableNavigableMap(in);
		dependencyClassPathFiles = SerialUtils.readExternalSortedImmutableNavigableMap(in);
	}
}
