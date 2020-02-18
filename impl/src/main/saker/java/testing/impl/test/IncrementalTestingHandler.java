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
package saker.java.testing.impl.test;

import java.io.Externalizable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import saker.build.exception.InvalidPathFormatException;
import saker.build.file.DirectoryVisitPredicate;
import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;
import saker.build.file.content.ContentDescriptor;
import saker.build.file.content.DirectoryContentDescriptor;
import saker.build.file.content.FileAttributesContentDescriptor;
import saker.build.file.content.MultiPathContentDescriptor;
import saker.build.file.content.NonExistentContentDescriptor;
import saker.build.file.content.NullContentDescriptor;
import saker.build.file.content.SerializableContentDescriptor;
import saker.build.file.path.ProviderHolderPathKey;
import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.file.provider.FileEntry;
import saker.build.file.provider.LocalFileProvider;
import saker.build.file.provider.SakerPathFiles;
import saker.build.launching.Main;
import saker.build.meta.PropertyNames;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.runtime.execution.ExecutionProperty;
import saker.build.runtime.execution.SakerLog;
import saker.build.runtime.params.ExecutionPathConfiguration;
import saker.build.runtime.repository.RepositoryEnvironment;
import saker.build.task.CommonTaskContentDescriptors;
import saker.build.task.InnerTaskExecutionParameters;
import saker.build.task.InnerTaskResultHolder;
import saker.build.task.InnerTaskResults;
import saker.build.task.Task;
import saker.build.task.TaskContext;
import saker.build.task.TaskDependencyFuture;
import saker.build.task.TaskExecutionUtilities;
import saker.build.task.TaskFactory;
import saker.build.task.TaskFileDeltas;
import saker.build.task.delta.DeltaType;
import saker.build.task.delta.FileChangeDelta;
import saker.build.task.dependencies.FileCollectionStrategy;
import saker.build.task.utils.FixedTaskDuplicationPredicate;
import saker.build.task.utils.TaskUtils;
import saker.build.task.utils.dependencies.DirectoryChildrenFileCollectionStrategy;
import saker.build.task.utils.dependencies.RecursiveIgnoreCaseExtensionFileCollectionStrategy;
import saker.build.util.cache.CacheKey;
import saker.build.util.classloader.SakerDirectoryClassLoaderDataFinder;
import saker.build.util.classloader.WildcardFilteringClassLoader;
import saker.build.util.config.ReferencePolicy;
import saker.build.util.property.BuildTimeExecutionProperty;
import saker.java.compiler.api.classpath.ClassPathEntry;
import saker.java.compiler.api.classpath.ClassPathReference;
import saker.java.compiler.api.classpath.ClassPathVisitor;
import saker.java.compiler.api.classpath.CompilationClassPath;
import saker.java.compiler.api.classpath.FileClassPath;
import saker.java.compiler.api.classpath.JavaClassPath;
import saker.java.compiler.api.classpath.JavaClassPathBuilder;
import saker.java.compiler.api.classpath.SDKClassPath;
import saker.java.compiler.api.compile.JavaCompilationWorkerTaskIdentifier;
import saker.java.compiler.api.compile.JavaCompilerWorkerTaskOutput;
import saker.java.compiler.api.compile.SakerJavaCompilerUtils;
import saker.java.testing.api.test.exc.JavaTestRunnerFailureException;
import saker.java.testing.api.test.invoker.JavaTestInvocationResult;
import saker.java.testing.api.test.invoker.JavaTestingFileProvider;
import saker.java.testing.api.test.invoker.JavaTestingInvoker;
import saker.java.testing.api.test.invoker.ReflectionJavaTestInvoker;
import saker.java.testing.api.test.invoker.TestInvocationParameters;
import saker.java.testing.api.test.invoker.TestInvokerParameters;
import saker.java.testing.impl.test.IncrementalTestingInfo.IncrementalTestCaseResult;
import saker.java.testing.impl.test.IncrementalTestingInfo.ReferencedFilePath;
import saker.java.testing.impl.test.IncrementalTestingInfo.TestCaseState;
import saker.java.testing.impl.test.launching.RemoteJavaRMIProcess;
import saker.java.testing.impl.test.launching.TestInvokerDaemon;
import saker.java.testing.main.test.JavaTesterTaskFactory;
import saker.nest.bundle.BundleIdentifier;
import saker.nest.bundle.JarNestRepositoryBundle;
import saker.nest.bundle.NestBundleClassLoader;
import saker.nest.bundle.NestRepositoryBundle;
import saker.nest.bundle.lookup.BundleIdentifierLookupResult;
import saker.nest.exc.BundleLoadingFailedException;
import saker.nest.utils.NestUtils;
import saker.build.thirdparty.saker.rmi.connection.ConstructorTransferProperties;
import saker.build.thirdparty.saker.rmi.connection.RMIVariables;
import saker.build.thirdparty.saker.rmi.exception.RMICallFailedException;
import saker.build.thirdparty.saker.rmi.exception.RMIRuntimeException;
import saker.build.thirdparty.saker.rmi.io.writer.RMIObjectWriteHandler;
import saker.sdk.support.api.SDKPathReference;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.SDKSupportUtils;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocation;
import saker.std.api.file.location.FileLocationVisitor;
import saker.std.api.file.location.LocalFileLocation;
import testing.saker.java.testing.TestFlag;
import saker.build.thirdparty.saker.util.ConcurrentPrependAccumulator;
import saker.build.thirdparty.saker.util.DateUtils;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.ReflectUtils;
import saker.build.thirdparty.saker.util.SetTransformingNavigableMap;
import saker.build.thirdparty.saker.util.StringUtils;
import saker.build.thirdparty.saker.util.classloader.ClassLoaderDataFinder;
import saker.build.thirdparty.saker.util.classloader.JarClassLoaderDataFinder;
import saker.build.thirdparty.saker.util.classloader.MultiDataClassLoader;
import saker.build.thirdparty.saker.util.classloader.ParentExclusiveClassLoader;
import saker.build.thirdparty.saker.util.classloader.PathClassLoaderDataFinder;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.build.thirdparty.saker.util.function.TriConsumer;
import saker.build.thirdparty.saker.util.io.FileUtils;
import saker.build.thirdparty.saker.util.io.ResourceCloser;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.build.trace.BuildTrace;

public class IncrementalTestingHandler {
	//TODO create test to ensure that a jar on the classpath changes, the tests are reinvoked

	private static final ClassLoaderDataFinder[] EMPTY_CLASSLOADERDATAFINDER_ARRAY = new ClassLoaderDataFinder[0];
	private static final ResourceDescriptorClassLoaderDataFinderSupplier[] EMPTY_RESOURCEDESCRIPTORCLASSLOADERDATAFINDERSUPPLIER_ARRAY = new ResourceDescriptorClassLoaderDataFinderSupplier[0];

	private static final Class<?> TEST_INVOKER_DAEMON_MAIN_CLASS = TestInvokerDaemon.class;
	private static final String TEST_INVOKER_DAEMON_MAIN_CLASS_NAME = TEST_INVOKER_DAEMON_MAIN_CLASS.getName();
	private static final String TEST_INVOKER_DAEMON_ENCLOSING_BUNDLE_IDENTIFIER_STRING = NestUtils
			.getClassBundleIdentifier(TEST_INVOKER_DAEMON_MAIN_CLASS).toString();

	private static final String EXTENSION_CLASSFILE = "class";

	private static class RemoteTester {
		private RemoteJavaRMIProcess rmiProcess;

		public RemoteTester(RemoteJavaRMIProcess rmiProcess) {
			this.rmiProcess = rmiProcess;
		}

		public RemoteJavaRMIProcess getRMIProcess() {
			return rmiProcess;
		}
	}

	private static class RemoteJavaTesterCacheKey implements CacheKey<RemoteTester, RemoteJavaRMIProcess> {

		private transient final SakerEnvironment environment;

		private final Path javaExe;
		private final Path sakerJar;
		private final Path workingDirectory;
		private final int identifier;
		private final Map<String, String> executionUserParameters;
		private final List<String> processJVMArguments;
		private final int javaMajor;

		public RemoteJavaTesterCacheKey(SakerEnvironment environment, Path javaExe, Path sakerJar,
				Path workingdirectory, int identifier, List<String> processjvmarguments, int javaMajor) {
			this.environment = environment;
			this.javaExe = javaExe;
			this.sakerJar = sakerJar;
			this.workingDirectory = workingdirectory;
			this.identifier = identifier;
			this.processJVMArguments = processjvmarguments;
			this.javaMajor = javaMajor;

			NestBundleClassLoader cl = (NestBundleClassLoader) IncrementalTestingHandler.class.getClassLoader();
			Map<String, String> userparams = cl.getBundleStorageConfiguration().getBundleLookup()
					.getLocalConfigurationUserParameters(null);
			this.executionUserParameters = userparams;
		}

		@Override
		public void close(RemoteTester data, RemoteJavaRMIProcess resource) throws Exception {
			resource.close();
		}

		@Override
		public RemoteJavaRMIProcess allocate() throws Exception {
			Path testingAgentJar = getAgentJarPath(environment, javaMajor);
			Path bootstrapAgentJar = getBootstrapAgentJarPath(environment, javaMajor);
			String agentpath = getShortPathedAgentPath(testingAgentJar);
			String bootstrapagentpath = getShortPathedAgentPath(bootstrapAgentJar);

			NestBundleClassLoader cl = (NestBundleClassLoader) IncrementalTestingHandler.class.getClassLoader();

			RepositoryEnvironment repoenv = cl.getRepository().getRepositoryEnvironment();
			List<String> commands = new ArrayList<>();
			commands.add(javaExe.toString());
			if (!ObjectUtils.isNullOrEmpty(processJVMArguments)) {
				commands.addAll(processJVMArguments);
			}
			ObjectUtils.addAll(commands, "-javaagent:" + agentpath + "=" + bootstrapagentpath,
					"-D" + PropertyNames.PROPERTY_SAKER_REFERENCE_POLICY + "="
							+ ReferencePolicy.ReferencePolicyCreator.WEAK,

					"-XX:MaxHeapFreeRatio=40", "-XX:MinHeapFreeRatio=15", "-XX:-OmitStackTraceInFastThrow",

					"-cp", sakerJar.toAbsolutePath().normalize().toString(), Main.class.getName(),

					"action", "-storage-dir", repoenv.getEnvironmentStorageDirectory().toString(), "-direct-repo",
					repoenv.getRepositoryClassPathLoadDirectory().toString(), "main");

			for (Entry<String, String> entry : executionUserParameters.entrySet()) {
				commands.add("-U" + entry.getKey().replace("=", "\\=") + "=" + entry.getValue());
			}
			commands.add("-class");
			commands.add(TEST_INVOKER_DAEMON_MAIN_CLASS_NAME);
			commands.add("-bundle");
			commands.add(TEST_INVOKER_DAEMON_ENCLOSING_BUNDLE_IDENTIFIER_STRING);

			LocalFileProvider.getInstance().createDirectories(workingDirectory);
			return new RemoteJavaRMIProcess(commands, workingDirectory.toString(),
					IncrementalTestingHandler.class.getClassLoader(),
					TestInvokerSupport.getTestInvokerRMITransferProperties(), environment.getEnvironmentThreadGroup());
		}

		private static Path getAgentJarPath(SakerEnvironment env, int javamajor) throws IOException {
			try {
				NestRepositoryBundle agentbundle = getBundleForJavaMajor(
						BundleIdentifier.valueOf("saker.java.testing-agent"), javamajor);
				if (agentbundle instanceof JarNestRepositoryBundle) {
					return ((JarNestRepositoryBundle) agentbundle).getJarPath();
				}
				throw new IOException("Unrecognized agent JAR type: " + ObjectUtils.classNameOf(agentbundle));
			} catch (BundleLoadingFailedException e) {
				throw new IOException("Agent Jar not found.", e);
			}
		}

		private static Path getBootstrapAgentJarPath(SakerEnvironment env, int javamajor) throws IOException {
			try {
				NestRepositoryBundle agentbundle = getBundleForJavaMajor(
						BundleIdentifier.valueOf("saker.java.testing-bootstrapagent"), javamajor);
				if (agentbundle instanceof JarNestRepositoryBundle) {
					return ((JarNestRepositoryBundle) agentbundle).getJarPath();
				}
				throw new IOException("Unrecognized bootstrap agent JAR type: " + ObjectUtils.classNameOf(agentbundle));
			} catch (BundleLoadingFailedException e) {
				throw new IOException("Agent Bootstrap Jar not found.", e);
			}
		}

		private static String getShortPathedAgentPath(Path path) throws NoSuchAlgorithmException, IOException {
			//if the jar paths are too long on windows (exceeds MAX_PATH), then the process will not find them.
			//    MAX_PATH is defined as 260, but have some threshold in order to function properly because
			//    the documentation says some functions have less than 260 maximum character count. E.g.
			//        When using an API to create a directory, the specified path cannot be so long that you 
			//        cannot append an 8.3 file name (that is, the directory name cannot exceed MAX_PATH minus 12).

			String pathstr = path.toString();
			if (pathstr.length() <= 240) {
				return pathstr;
			}
			String tempdir = System.getProperty("java.io.tmpdir");
			if (tempdir == null) {
				//no temp directory, warn the user, and return the long path.
				//any failure will be deferred
				SakerLog.warning()
						.println("Temp directory not available. Failed to extract JAR with long path: " + pathstr);
				return pathstr;
			}
			String pathmd5 = StringUtils.toHexString(MessageDigest.getInstance("MD5")
					.digest(path.getParent().toString().getBytes(StandardCharsets.UTF_8)));
			Path parentdir = Paths.get(tempdir, "saker", "shorten", pathmd5);
			Path npath = parentdir.resolve(path.getFileName());
			LocalFileProvider localfiles = LocalFileProvider.getInstance();
			FileEntry currentattrs;
			try {
				currentattrs = localfiles.getFileAttributes(npath);
			} catch (IOException e) {
				currentattrs = null;
			}
			if (currentattrs == null
					|| FileAttributesContentDescriptor.isChanged(currentattrs, localfiles.getFileAttributes(path))) {
				try {
					localfiles.createDirectories(parentdir);
					Files.copy(path, npath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
				} catch (IOException e) {
					SakerLog.warning().println("Failed to extract JAR with long path to temp directory: " + pathstr
							+ " -> " + tempdir + " (" + e + ")");
					return pathstr;
				}
			}
			return npath.toString();
		}

		@Override
		public RemoteTester generate(RemoteJavaRMIProcess resource) throws Exception {
			return new RemoteTester(resource);
		}

		@Override
		public boolean validate(RemoteTester data, RemoteJavaRMIProcess resource) {
			boolean valid = resource.isValid();
			if (!valid) {
				resource.close();
			}
			return valid;
		}

		@Override
		public long getExpiry() {
			if (TestFlag.ENABLED) {
				//if testing, expire ASAP to free resources
				return 5 * DateUtils.MS_PER_SECOND;
			}
			return 5 * DateUtils.MS_PER_MINUTE;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((executionUserParameters == null) ? 0 : executionUserParameters.hashCode());
			result = prime * result + identifier;
			result = prime * result + ((javaExe == null) ? 0 : javaExe.hashCode());
			result = prime * result + javaMajor;
			result = prime * result + ((processJVMArguments == null) ? 0 : processJVMArguments.hashCode());
			result = prime * result + ((sakerJar == null) ? 0 : sakerJar.hashCode());
			result = prime * result + ((workingDirectory == null) ? 0 : workingDirectory.hashCode());
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
			RemoteJavaTesterCacheKey other = (RemoteJavaTesterCacheKey) obj;
			if (executionUserParameters == null) {
				if (other.executionUserParameters != null)
					return false;
			} else if (!executionUserParameters.equals(other.executionUserParameters))
				return false;
			if (identifier != other.identifier)
				return false;
			if (javaExe == null) {
				if (other.javaExe != null)
					return false;
			} else if (!javaExe.equals(other.javaExe))
				return false;
			if (javaMajor != other.javaMajor)
				return false;
			if (processJVMArguments == null) {
				if (other.processJVMArguments != null)
					return false;
			} else if (!processJVMArguments.equals(other.processJVMArguments))
				return false;
			if (sakerJar == null) {
				if (other.sakerJar != null)
					return false;
			} else if (!sakerJar.equals(other.sakerJar))
				return false;
			if (workingDirectory == null) {
				if (other.workingDirectory != null)
					return false;
			} else if (!workingDirectory.equals(other.workingDirectory))
				return false;
			return true;
		}

	}

	private TaskContext taskContext;
	private ExecutionContext executionContext;
	private Path sakerJarFile;

	private Collection<Integer> successExitCodes;
	private SDKReference javaSDK;
	private SakerPath workingDirectory;
	private int maxJVMCount = 1;
	private String testInvokerClassName = ReflectionJavaTestInvoker.class.getName();

	private JavaClassPath testRunnerClassPaths;
	private JavaClassPath userClassPaths;
	private JavaClassPath testClassPaths;
	private JavaClassPath dependencyClassPaths;
	private TestInvokerParameters testInvokerParameters;
	private Map<Predicate<String>, Map<String, String>> testClassParameters = Collections.emptyNavigableMap();
	private NavigableSet<String> testClasses;
	private NavigableSet<String> nonDeterministicTests;

	private Map<WildcardPath, Collection<WildcardPath>> additionalTestClassDependencies = Collections
			.emptyNavigableMap();

	private NavigableMap<ReferencedFilePath, IncrementalTestCaseResult> prevTestCasesByPath;
	private IncrementalTestingInfo prevInfo;

	private boolean failFast = false;
	private boolean verbose = TestFlag.ENABLED;

	private Set<String> retainClassNames;

	//XXX maybe reify this ignoring with wildcards?
	private final NavigableSet<SakerPath> ignoreFileChanges;

	private List<String> processJVMArguments;

	private Collection<Throwable> exceptions = new ArrayList<>();

	private static final AtomicIntegerFieldUpdater<IncrementalTestingHandler> AIFU_completedSuccessfulTestCount = AtomicIntegerFieldUpdater
			.newUpdater(IncrementalTestingHandler.class, "completedSuccessfulTestCount");
	private static final AtomicLongFieldUpdater<IncrementalTestingHandler> ALFU_lastSuccessfulTestPrintoutNanos = AtomicLongFieldUpdater
			.newUpdater(IncrementalTestingHandler.class, "lastSuccessfulTestPrintoutNanos");

	private transient volatile int completedSuccessfulTestCount;
	private transient volatile long lastSuccessfulTestPrintoutNanos;

	//TODO clean up code comments

	public IncrementalTestingHandler(TaskContext taskcontext, ExecutionContext context, Path sakerjar,
			SDKReference javaSDK, JavaClassPath testRunnerClassPaths, JavaClassPath userClassPaths,
			JavaClassPath testclasspathfiles, TestInvokerParameters testInvokerParameters,
			Collection<String> testClasses, IncrementalTestingInfo previnfo,
			NavigableSet<SakerPath> igenorefilechanges) {
		this.taskContext = taskcontext;
		this.executionContext = context;
		this.sakerJarFile = sakerjar;
		this.javaSDK = javaSDK;
		this.ignoreFileChanges = igenorefilechanges;
		this.testRunnerClassPaths = testRunnerClassPaths;
		this.userClassPaths = userClassPaths;
		this.testClassPaths = testclasspathfiles;
		this.testInvokerParameters = testInvokerParameters;
		this.testClasses = testClasses == null ? null : new TreeSet<>(testClasses);
		this.prevInfo = previnfo;

		if (previnfo == null) {
			prevTestCasesByPath = Collections.emptyNavigableMap();
		} else {
			prevTestCasesByPath = new TreeMap<>(previnfo.getTestCasesByPath());
		}
	}

	public Collection<Throwable> getExceptions() {
		return exceptions;
	}

	public void setMaxJVMCount(int maxJVMCount) {
		this.maxJVMCount = maxJVMCount;
	}

	public void setProcessJVMArguments(List<String> processJVMArguments) {
		this.processJVMArguments = processJVMArguments;
	}

	public void setSuccessExitCodes(Collection<Integer> successExitCodes) {
		this.successExitCodes = successExitCodes;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

	public void setWorkingDirectory(SakerPath workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public void setTestInvokerClassName(String testInvokerClassName) {
		Objects.requireNonNull(testInvokerClassName);
		this.testInvokerClassName = testInvokerClassName;
	}

	public void setNonDeterministicTests(Collection<String> nonDeterministicTests) {
		this.nonDeterministicTests = nonDeterministicTests == null ? null : new TreeSet<>(nonDeterministicTests);
	}

	public void setAdditionalTestClassDependencies(
			Map<String, ? extends Collection<String>> additionalTestClassDependencies) {
		this.additionalTestClassDependencies = new TreeMap<>();
		for (Entry<String, ? extends Collection<String>> entry : additionalTestClassDependencies.entrySet()) {
			WildcardPath wc = WildcardPath.valueOf(entry.getKey().replace('.', '/'));
			this.additionalTestClassDependencies.compute(wc, (k, v) -> {
				if (v == null) {
					v = new TreeSet<>();
				}
				for (String p : entry.getValue()) {
					v.add(WildcardPath.valueOf(p.replace('.', '/')));
				}
				return v;
			});
		}
	}

	public void setDependencyClassPaths(JavaClassPath dependencyClassPaths) {
		this.dependencyClassPaths = dependencyClassPaths;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setTestClassParameters(Map<String, Map<String, String>> testClassParameters) {
		this.testClassParameters = new IdentityHashMap<>(testClassParameters.size());
		for (Entry<String, Map<String, String>> entry : testClassParameters.entrySet()) {
			Predicate<String> pred = createClassNameTestPredicate(entry.getKey());
			this.testClassParameters.put(pred, entry.getValue());
		}
	}

	private Collection<String> getAdditionalTestDependencyClasses(String classname) {
		Collection<String> result = new TreeSet<>();
		String pathname = classname.replace('.', '/');
		for (Entry<WildcardPath, Collection<WildcardPath>> entry : additionalTestClassDependencies.entrySet()) {
			if (entry.getKey().includes(pathname)) {
				for (String depcn : retainClassNames) {
					String deppn = depcn.replace('.', '/');
					for (WildcardPath depwc : entry.getValue()) {
						if (depwc.includes(deppn)) {
							result.add(depcn);
							break;
						}
					}
				}
			}
		}
		return result;
	}

	private interface ResourceDescriptorClassLoaderDataFinderSupplier {
		public ClassLoaderDataFinder createDataFinder(TaskContext taskcontext, RMIVariables rmivariables)
				throws IOException;

		public Entry<SakerPath, ContentDescriptor> getResourceFileContentDescriptor(String resourcename);
	}

	private static class SakerDirectoryResourceDescriptorClassLoaderDataFinderSupplier
			implements ResourceDescriptorClassLoaderDataFinderSupplier {

		private final transient TaskExecutionUtilities taskUtils;
		private final SakerDirectory directory;
		private final transient SakerPath directoryPath;

		public SakerDirectoryResourceDescriptorClassLoaderDataFinderSupplier(TaskExecutionUtilities taskUtils,
				SakerDirectory directory, SakerPath directoryPath) {
			this.taskUtils = taskUtils;
			this.directory = directory;
			this.directoryPath = directoryPath;
		}

		@Override
		public ClassLoaderDataFinder createDataFinder(TaskContext taskcontext, RMIVariables rmivariables)
				throws IOException {
			return new SakerDirectoryClassLoaderDataFinder(taskUtils, directory);
		}

		@Override
		public Entry<SakerPath, ContentDescriptor> getResourceFileContentDescriptor(String resourcename) {
			try {
				SakerPath respath = SakerPath.valueOf(resourcename);
				if (!respath.isForwardRelative()) {
					return null;
				}
				SakerPath foundfilepath = directoryPath.resolve(respath);
				SakerFile foundfile = taskUtils.resolveAtRelativePath(directory, respath);
				if (foundfile != null) {
					return ImmutableUtils.makeImmutableMapEntry(foundfilepath, foundfile.getContentDescriptor());
				}
				return ImmutableUtils.makeImmutableMapEntry(foundfilepath, NonExistentContentDescriptor.INSTANCE);
			} catch (InvalidPathFormatException e) {
				return null;
			}
		}

		@Override
		public String toString() {
			return "SakerDirectoryResourceDescriptorClassLoaderDataFinderSupplier [directory=" + directory + "]";
		}
	}

	private static class JarResourceDescriptorClassLoaderDataFinderSupplier
			implements ResourceDescriptorClassLoaderDataFinderSupplier {
		private Entry<SakerPath, ContentDescriptor> resourceFileEntry;
		private SakerFile jarFile;

		public JarResourceDescriptorClassLoaderDataFinderSupplier(SakerFile jarfile, SakerPath filepath)
				throws IOException, NullPointerException {
			this.jarFile = jarfile;
			this.resourceFileEntry = ImmutableUtils.makeImmutableMapEntry(filepath, jarfile.getContentDescriptor());
		}

		@Override
		public ClassLoaderDataFinder createDataFinder(TaskContext taskcontext, RMIVariables rmivariables)
				throws IOException {
			Path mirrorjarpath = taskcontext.mirror(jarFile);
			File mirrorjarfile = mirrorjarpath.toFile();
			try {
				return (ClassLoaderDataFinder) rmivariables.newRemoteInstance(ConstructorTransferProperties
						.builder(ReflectUtils.getConstructorAssert(JarClassLoaderDataFinder.class, File.class))
						.parameterWriter(0, RMIObjectWriteHandler.serialize()).build(), mirrorjarfile);
			} catch (RMIRuntimeException | InvocationTargetException e) {
				throw new IOException(
						"Failed to instantiate JAR classpath: " + mirrorjarfile + " for " + resourceFileEntry.getKey(),
						e);
			}
		}

		@Override
		public Entry<SakerPath, ContentDescriptor> getResourceFileContentDescriptor(String resourcename) {
			return resourceFileEntry;
		}

		@Override
		public String toString() {
			return "JarResourceDescriptorClassLoaderDataFinderSupplier [resourceFileEntry=" + resourceFileEntry
					+ ", jarFile=" + jarFile + "]";
		}
	}

	private static class LocalJarResourceDescriptorClassLoaderDataFinderSupplier
			implements ResourceDescriptorClassLoaderDataFinderSupplier {
		private Path jarPath;

		public LocalJarResourceDescriptorClassLoaderDataFinderSupplier(Path jarPath) {
			this.jarPath = jarPath;
		}

		@Override
		public ClassLoaderDataFinder createDataFinder(TaskContext taskcontext, RMIVariables rmivariables)
				throws IOException {
			File jarfile = jarPath.toFile();
			try {
				return (ClassLoaderDataFinder) rmivariables.newRemoteInstance(ConstructorTransferProperties
						.builder(ReflectUtils.getConstructorAssert(JarClassLoaderDataFinder.class, File.class))
						.parameterWriter(0, RMIObjectWriteHandler.serialize()).build(), jarfile);
			} catch (RMIRuntimeException | InvocationTargetException e) {
				throw new IOException("Failed to instantiate JAR classpath: " + jarfile, e);
			}
		}

		@Override
		public Entry<SakerPath, ContentDescriptor> getResourceFileContentDescriptor(String resourcename) {
			//XXX do we need to implement this?
			return null;
		}

		@Override
		public String toString() {
			return "LocalJarResourceDescriptorClassLoaderDataFinderSupplier[" + jarPath + "]";
		}
	}

	private static class LocalDirectoryResourceDescriptorClassLoaderDataFinderSupplier
			implements ResourceDescriptorClassLoaderDataFinderSupplier {
		private Path dirPath;

		public LocalDirectoryResourceDescriptorClassLoaderDataFinderSupplier(Path dirPath) {
			this.dirPath = dirPath;
		}

		@Override
		public ClassLoaderDataFinder createDataFinder(TaskContext taskcontext, RMIVariables rmivariables)
				throws IOException {
			File dirfile = dirPath.toFile();
			try {
				return (ClassLoaderDataFinder) rmivariables.newRemoteInstance(ConstructorTransferProperties
						.builder(ReflectUtils.getConstructorAssert(PathClassLoaderDataFinder.class, File.class))
						.parameterWriter(0, RMIObjectWriteHandler.serialize()).build(), dirfile);
			} catch (RMIRuntimeException | InvocationTargetException e) {
				throw new IOException("Failed to instantiate directory classpath: " + dirfile, e);
			}
		}

		@Override
		public Entry<SakerPath, ContentDescriptor> getResourceFileContentDescriptor(String resourcename) {
			//XXX do we need to implement this?
			return null;
		}

		@Override
		public String toString() {
			return "LocalDirectoryResourceDescriptorClassLoaderDataFinderSupplier[" + dirPath + "]";
		}
	}

//	private interface ResourceDescriptorClassLoaderDataFinder extends ClassLoaderDataFinder {
//		public Entry<SakerPath, ContentDescriptor> getResourceFileContentDescriptor(String resourcename);
//	}
//
//	private static class SakerDirectoryResourceDescriptorClassLoaderDataFinder extends SakerDirectoryClassLoaderDataFinder
//			implements ResourceDescriptorClassLoaderDataFinder {
//
//		private final SakerPath directoryPath;
//
//		public SakerDirectoryResourceDescriptorClassLoaderDataFinder(TaskExecutionUtilities taskUtils, SakerDirectory directory) throws NullPointerException {
//			super(taskUtils, directory);
//			directoryPath = directory.getSakerPath();
//			SakerPathFiles.requireAbsolutePath(directoryPath);
//		}
//
//		@Override
//		public Entry<SakerPath, ContentDescriptor> getResourceFileContentDescriptor(String resourcename) {
//			try {
//				SakerPath respath = SakerPath.valueOf(resourcename);
//				if (!respath.isForwardRelative()) {
//					return null;
//				}
//				SakerPath foundfilepath = directoryPath.resolve(respath);
//				SakerFile foundfile = taskUtils.resolveAtRelativePath(directory, respath);
//				if (foundfile != null) {
//					return ImmutableUtils.makeImmutableMapEntry(foundfilepath, foundfile.getContentDescriptor());
//				}
//				return ImmutableUtils.makeImmutableMapEntry(foundfilepath, NonExistentContentDescriptor.INSTANCE);
//			} catch (InvalidPathFormatException e) {
//				return null;
//			}
//		}
//	}
//
//	private static class JarResourceDescriptorClassLoaderDataFinder extends JarClassLoaderDataFinder implements ResourceDescriptorClassLoaderDataFinder {
//
//		private Entry<SakerPath, ContentDescriptor> resourceFileEntry;
//
//		public JarResourceDescriptorClassLoaderDataFinder(Path jar, SakerPath jarexecutionpath, ContentDescriptor jarcontentdescriptor)
//				throws IOException, NullPointerException {
//			super(jar);
//			SakerPathFiles.requireAbsolutePath(jarexecutionpath);
//			this.resourceFileEntry = ImmutableUtils.makeImmutableMapEntry(jarexecutionpath, jarcontentdescriptor);
//		}
//
//		@Override
//		public Entry<SakerPath, ContentDescriptor> getResourceFileContentDescriptor(String resourcename) {
//			return resourceFileEntry;
//		}
//
//	}

	private static final Pattern PATTERN_JDK_VERSION_QUALIFIER = Pattern.compile("jdk([0-9]+)");

	public static NestRepositoryBundle getBundleForJavaMajor(BundleIdentifier bundlenamewithqualifiers, int javamajor)
			throws BundleLoadingFailedException {
		String bundlename = bundlenamewithqualifiers.getName();
		NestBundleClassLoader thiscl = (NestBundleClassLoader) IncrementalTestingHandler.class.getClassLoader();
		String thisversionnumber = thiscl.getBundle().getBundleIdentifier().getVersionNumber();
		BundleIdentifierLookupResult lookupresult = thiscl.getRelativeBundleLookup()
				.lookupBundleIdentifiers(bundlename);
		if (lookupresult == null) {
			return null;
		}
		Map<String, ? extends Set<? extends BundleIdentifier>> bundles = lookupresult.getBundles();
		if (ObjectUtils.isNullOrEmpty(bundles)) {
			return null;
		}
		NavigableSet<String> bundlenamequalifiers = bundlenamewithqualifiers.getBundleQualifiers();
		//we expect the given bundles to have the all the qualifiers as the argument, and the jdk<num> qualifier
		int expectedbundlequalifiernum = bundlenamequalifiers.size() + 1;

		int maxfoundversion = -1;
		BundleIdentifier maxbundleid = null;

		search_loop:
		for (Entry<String, ? extends Set<? extends BundleIdentifier>> entry : bundles.entrySet()) {
			if (BundleIdentifier.compareVersionNumbers(thisversionnumber, entry.getKey()) < 0) {
				//don't use a newer one
				continue;
			}
			bundle_search_loop:
			for (BundleIdentifier bid : entry.getValue()) {
				NavigableSet<String> bidqualifiers = bid.getBundleQualifiers();
				if (bidqualifiers.size() != expectedbundlequalifiernum) {
					continue;
				}
				if (!bidqualifiers.containsAll(bundlenamequalifiers)) {
					continue;
				}
				for (String q : bidqualifiers) {
					Matcher matcher = PATTERN_JDK_VERSION_QUALIFIER.matcher(q);
					if (!matcher.matches()) {
						continue;
					}
					int qualifierjavamajor = Integer.parseInt(matcher.group(1));
					if (qualifierjavamajor == javamajor) {
						maxbundleid = bid;
						//found the exact match
						break search_loop;
					} else if (qualifierjavamajor < javamajor) {
						//valid bundleid
						if (qualifierjavamajor > maxfoundversion) {
							maxfoundversion = qualifierjavamajor;
							maxbundleid = bid;
							continue bundle_search_loop;
						}

					}
				}
			}
		}
		if (maxbundleid != null) {
			return lookupresult.getStorageView().getBundle(maxbundleid);
		}
		return null;
	}

	private ResourceDescriptorClassLoaderDataFinderSupplier createNonContainerClassLoaderDataFinder(SakerFile mf,
			SakerPath filepath) throws IOException {
		if (FileUtils.hasExtensionIgnoreCase(filepath.getFileName(), "jar")) {
			return new JarResourceDescriptorClassLoaderDataFinderSupplier(mf, filepath);
		}
		throw new IOException("ClassPath entry is not a directory, and not a JAR: " + mf);
	}

	//XXX when deleting this, check for other unsupported throwing methods
	private static UnsupportedOperationException unsupportedNonDirectoryClassPath(SakerFile mf) {
		return new UnsupportedOperationException("Non directory classpaths is not yet supported: " + mf);
	}

	private NavigableMap<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> collectFileLocationClassPath(
			Iterable<? extends FileLocation> filelocations) throws IOException {
		TaskExecutionUtilities taskutils = taskContext.getTaskUtilities();
		TreeMap<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> result = new TreeMap<>();
		for (FileLocation location : filelocations) {
			location.accept(new FileLocationVisitor() {
				@Override
				public void visit(ExecutionFileLocation loc) {
					SakerPath filepath = loc.getPath();
					SakerFile mf = taskutils.resolveAtAbsolutePath(filepath);
					if (mf == null) {
						throw ObjectUtils
								.sneakyThrow(new FileNotFoundException("Class path file not found: " + filepath));
					}
					ResourceDescriptorClassLoaderDataFinderSupplier finder;
					if (mf instanceof SakerDirectory) {
						finder = new SakerDirectoryResourceDescriptorClassLoaderDataFinderSupplier(
								taskContext.getTaskUtilities(), (SakerDirectory) mf, filepath);
					} else {
						try {
							finder = createNonContainerClassLoaderDataFinder(mf, filepath);
						} catch (IOException e) {
							throw ObjectUtils.sneakyThrow(e);
						}
					}
					result.put(new ReferencedFilePath(ReferencedFilePath.LOCATION_EXECUTION, filepath), finder);
				}

				@Override
				public void visit(LocalFileLocation loc) {
					ResourceDescriptorClassLoaderDataFinderSupplier finder;
					Path realpath = LocalFileProvider.toRealPath(loc.getLocalPath());
					try {
						FileEntry attrs = LocalFileProvider.getInstance().getFileAttributes(realpath);
						if (attrs.isDirectory()) {
							finder = new LocalDirectoryResourceDescriptorClassLoaderDataFinderSupplier(realpath);
						} else {
							finder = new LocalJarResourceDescriptorClassLoaderDataFinderSupplier(realpath);
						}
					} catch (IOException e) {
						throw ObjectUtils.sneakyThrow(e);
					}
					result.put(new ReferencedFilePath(ReferencedFilePath.LOCATION_LOCAL, loc.getLocalPath()), finder);
				}
			});
		}
		return result;
	}

	private NavigableMap<SakerPath, ResourceDescriptorClassLoaderDataFinderSupplier> collectClassPath(
			Iterable<? extends SakerFile> classpaths) throws IOException {
		NavigableMap<SakerPath, ResourceDescriptorClassLoaderDataFinderSupplier> result = new TreeMap<>();
		for (SakerFile mf : classpaths) {
			SakerPath filepath = SakerPathFiles.requireAbsolutePath(mf);
			ResourceDescriptorClassLoaderDataFinderSupplier finder;
			if (mf instanceof SakerDirectory) {
				finder = new SakerDirectoryResourceDescriptorClassLoaderDataFinderSupplier(
						taskContext.getTaskUtilities(), (SakerDirectory) mf, filepath);
			} else {
				finder = createNonContainerClassLoaderDataFinder(mf, filepath);
			}
			result.put(filepath, finder);
		}
		return result;
	}

	private static void collectChangedClassNamesInClassPath(
			Map<SakerFile, ? extends NavigableMap<SakerPath, ?>> currentclasspathfiles,
			NavigableSet<SakerPath> previousclasspathfiles, Collection<String> result, TaskFileDeltas inputfilechanges,
			TaskFileDeltas inputfileadditions, TestingFileTags tag) {
		NavigableMap<SakerPath, SakerFile> additionpathfiles = TaskUtils.collectFilesForTag(inputfileadditions, tag);
		NavigableMap<SakerPath, SakerFile> changedfiles = TaskUtils.collectFilesForTag(inputfilechanges, tag);

		for (Entry<SakerFile, ? extends NavigableMap<SakerPath, ?>> entry : currentclasspathfiles.entrySet()) {
			SakerFile entryfile = entry.getKey();
			SakerPath cppath = entryfile.getSakerPath();

			NavigableMap<SakerPath, ?> cpclassfiles = entry.getValue();
			if (cpclassfiles == null) {
				//XXX make this warning suppressable
				SakerLog.warning().println("Class file changes are not tracked for testing for classpath: " + cppath);
				continue;
			}

			if (!previousclasspathfiles.contains(cppath)) {
				//newly added classpath
				//add all of the corresponding class files to the changed set
				for (SakerPath cfpath : cpclassfiles.keySet()) {
					result.add(getClassNameFromClassFilePath(cppath, cfpath));
				}
			} else {
				//classpath was present in the previous testing too
				//get changes and additions from the task context
				for (SakerPath cfpath : SakerPathFiles.getPathSubMapDirectoryChildren(changedfiles, cppath, false)
						.keySet()) {
					result.add(getClassNameFromClassFilePath(cppath, cfpath));
				}

				for (SakerPath cfpath : SakerPathFiles.getPathSubMapDirectoryChildren(additionpathfiles, cppath, false)
						.keySet()) {
					result.add(getClassNameFromClassFilePath(cppath, cfpath));
				}
			}
		}
	}

	private static void collectFileLocationChangedClassNamesInClassPath(
			Map<FileLocation, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> currentclasspathfiles,
			NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> previousclasspathfiles,
			Collection<String> result, TaskFileDeltas inputfilechanges, TaskFileDeltas inputfileadditions,
			TestingFileTags tag) {
		NavigableMap<SakerPath, SakerFile> additionpathfiles = TaskUtils.collectFilesForTag(inputfileadditions, tag);
		NavigableMap<SakerPath, SakerFile> changedfiles = TaskUtils.collectFilesForTag(inputfilechanges, tag);

		for (Entry<FileLocation, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> entry : currentclasspathfiles
				.entrySet()) {
			FileLocation entryfile = entry.getKey();
			NavigableMap<SakerPath, ? extends ContentDescriptor> cpclassfiles = entry.getValue();
			if (cpclassfiles == null) {
				//XXX make this warning suppressable
				SakerLog.warning().verbose()
						.println("Class file changes are not tracked for testing for classpath: " + entryfile);
				continue;
			}

			ReferencedFilePath reffilecp = toReferencedFilePath(entryfile);
			SakerPath cppath = reffilecp.getPath();

			NavigableMap<SakerPath, ? extends ContentDescriptor> prevcpfiles = previousclasspathfiles.get(reffilecp);

			if (prevcpfiles == null) {
				//newly added classpath
				//add all of the corresponding class files to the changed set
				for (SakerPath cfpath : cpclassfiles.keySet()) {
					result.add(getClassNameFromClassFilePath(cppath, cfpath));
				}
			} else {
				//classpath was present in the previous testing too
				//get changes and additions from the task context
				if (ReferencedFilePath.LOCATION_EXECUTION.equals(reffilecp.getLocation())) {
					for (SakerPath cfpath : SakerPathFiles.getPathSubMapDirectoryChildren(changedfiles, cppath, false)
							.keySet()) {
						result.add(getClassNameFromClassFilePath(cppath, cfpath));
					}

					for (SakerPath cfpath : SakerPathFiles
							.getPathSubMapDirectoryChildren(additionpathfiles, cppath, false).keySet()) {
						result.add(getClassNameFromClassFilePath(cppath, cfpath));
					}
				} else if (ReferencedFilePath.LOCATION_LOCAL.equals(reffilecp.getLocation())) {
					ObjectUtils.iterateSortedMapEntries(prevcpfiles, cpclassfiles, (k, prevcd, currentcd) -> {
						if (currentcd == null || prevcd == null || currentcd.isChanged(prevcd)) {
							result.add(getClassNameFromClassFilePath(cppath, k));
						}
					});
				} else {
					throw new UnsupportedOperationException();
				}
			}
		}
	}

	private static void collectClassNamesByPath(Map<SakerFile, ? extends NavigableMap<SakerPath, ?>> classpath,
			NavigableMap<SakerPath, String> result) {
		for (Entry<SakerFile, ? extends NavigableMap<SakerPath, ?>> entry : classpath.entrySet()) {
			SakerPath cppath = entry.getKey().getSakerPath();
			NavigableMap<SakerPath, ?> cpclassfiles = entry.getValue();
			if (cpclassfiles != null) {
				for (SakerPath classfilepath : cpclassfiles.keySet()) {
					result.put(classfilepath, getClassNameFromClassFilePath(cppath, classfilepath));
				}
			}
		}
	}

	public static String getClassNameFromClassFilePath(SakerPath dirpath, SakerPath classfilepath) {
		SakerPath relative = dirpath.relativize(classfilepath);
		//remove .class extension
		return StringUtils.removeFromEnd(relative.toString().replace('/', '.'), 6);
	}

	private static void collectFileLocationClassNamesByPath(
			Map<FileLocation, ? extends NavigableMap<SakerPath, ?>> classpath,
			NavigableMap<ReferencedFilePath, String> result) {
		for (Entry<FileLocation, ? extends NavigableMap<SakerPath, ?>> entry : classpath.entrySet()) {
			ReferencedFilePath reffp = toReferencedFilePath(entry.getKey());
			NavigableMap<SakerPath, ?> cpclassfiles = entry.getValue();
			if (cpclassfiles != null) {
				for (SakerPath classfilepath : cpclassfiles.keySet()) {
					result.put(new ReferencedFilePath(reffp.getLocation(), classfilepath),
							getClassNameFromClassFilePath(reffp.getPath(), classfilepath));
				}
			}
		}
	}

	private static ReferencedFilePath toReferencedFilePath(FileLocation fl) {
		ReferencedFilePath[] result = { null };
		fl.accept(new FileLocationVisitor() {
			@Override
			public void visit(ExecutionFileLocation loc) {
				result[0] = new ReferencedFilePath(ReferencedFilePath.LOCATION_EXECUTION, loc.getPath());
			}

			@Override
			public void visit(LocalFileLocation loc) {
				result[0] = new ReferencedFilePath(ReferencedFilePath.LOCATION_LOCAL, loc.getLocalPath());
			}
		});
		return result[0];
	}

	private static NavigableSet<ReferencedFilePath> toReferencedFilePathSet(
			Collection<? extends FileLocation> filelocations) {
		TreeSet<ReferencedFilePath> result = new TreeSet<>();
		for (FileLocation fl : filelocations) {
			result.add(toReferencedFilePath(fl));
		}
		return result;
	}

	private static <V> NavigableMap<ReferencedFilePath, V> toReferencedFilePathMap(
			Map<? extends FileLocation, V> filelocations) {
		NavigableMap<ReferencedFilePath, V> result = new TreeMap<>();
		for (Entry<? extends FileLocation, V> entry : filelocations.entrySet()) {
			result.put(toReferencedFilePath(entry.getKey()), entry.getValue());
		}
		return result;
	}

	public static String getClassSimpleNameFromBinaryName(String binaryname) {
		if (binaryname == null) {
			return null;
		}
		int dotidx = binaryname.lastIndexOf('.');
		int dollaridx = binaryname.lastIndexOf('$', dotidx);
		return binaryname.substring(Math.max(dotidx, dollaridx) + 1);
	}

	public IncrementalTestingInfo test() throws Exception {
		final Path testerworkingdirpath;
		SakerDirectory workmoddir;
		if (this.workingDirectory == null) {
			workmoddir = taskContext.getTaskWorkingDirectory();
		} else {
			workmoddir = taskContext.getTaskUtilities().resolveDirectoryAtPath(workingDirectory);
			if (workmoddir == null) {
				throw new IllegalArgumentException("Working directory not found at path: " + workingDirectory);
			}
		}
		SakerPath workmoddirpath = workmoddir.getSakerPath();
		testerworkingdirpath = executionContext.toMirrorPath(workmoddirpath);
		Path workingdirectoryactualpath = executionContext.getPathConfiguration().toLocalPath(workmoddirpath);

//		Collection<SakerFile> testrunnercpfiles = JavaTaskUtils
//				.entriesToClassPathEntryMap(taskContext, testrunnerclasspathentrycollections,
//						TestingFileTags.TEST_RUNNER_CLASSPATH, ClassPathDependencyMode.IMPLEMENTATION)
//				.keySet();

		//TODO introduce sdk references
		Map<String, SDKReference> sdkreferences = Collections.emptyMap();
		Map<FileLocation, ContentDescriptor> testrunnercpfilelocations = collectFileLocationsWithImplementationDependencyReporting(
				taskContext, testRunnerClassPaths, TestingFileTags.TEST_RUNNER_CLASSPATH, sdkreferences);

//		Map<SakerFile, ? extends NavigableMap<SakerPath, ?>> usercpfilesmap = JavaTaskUtils.entriesToClassPathFilesMap(
//				taskContext, userclasspathentrycollections.getClassPathReferences(), TestingFileTags.USER_CLASSPATH,
//				ClassPathDependencyMode.IMPLEMENTATION);
//		Map<SakerFile, ? extends NavigableMap<SakerPath, ?>> depcpfilesmap = JavaTaskUtils.entriesToClassPathFilesMap(
//				taskContext, dependencyclasspathentrycollections.getClassPathReferences(),
//				TestingFileTags.DEPENDENCY_CLASSPATH, ClassPathDependencyMode.IMPLEMENTATION);
//		Map<SakerFile, ? extends NavigableMap<SakerPath, ?>> testcpfilesmap = JavaTaskUtils.entriesToClassPathFilesMap(
//				taskContext, testclasspathentrycollections.getClassPathReferences(), TestingFileTags.TEST_CLASSPATH,
//				ClassPathDependencyMode.IMPLEMENTATION);

		//maps of file locations to their contained class path files with their contents
		Map<FileLocation, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> usercpfilesmap = collectFileLocationClassFileConentsWithImplementationDependencyReporting(
				userClassPaths, TestingFileTags.USER_CLASSPATH);
		Map<FileLocation, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> depcpfilesmap = collectFileLocationClassFileConentsWithImplementationDependencyReporting(
				dependencyClassPaths, TestingFileTags.DEPENDENCY_CLASSPATH);
		Map<FileLocation, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> testcpfilesmap = collectFileLocationClassFileConentsWithImplementationDependencyReporting(
				testClassPaths, TestingFileTags.TEST_CLASSPATH);

		TaskFileDeltas inputfilechangedeltas = taskContext.getFileDeltas(DeltaType.INPUT_FILE_CHANGE);
		TaskFileDeltas inputfileadditiondeltas = taskContext.getFileDeltas(DeltaType.INPUT_FILE_ADDITION);

		if (prevInfo != null) {
			if (prevInfo.isTestRunnerClassPathFilesChanged(testrunnercpfilelocations)
					|| inputfilechangedeltas.hasFileDeltaWithTag(TestingFileTags.TEST_RUNNER_CLASSPATH)
					|| inputfileadditiondeltas.hasFileDeltaWithTag(TestingFileTags.TEST_RUNNER_CLASSPATH)) {
				//the test runner class path changed, rerun all the tests
				prevTestCasesByPath = Collections.emptyNavigableMap();
			}
		}

		IncrementalTestingInfo resultinfo = new IncrementalTestingInfo();

		resultinfo.setTestRunnerClassPathFiles(testrunnercpfilelocations);
		//TODO check if the test parameters changed

		NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> prevtestclasspathfiles;
		NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> prevuserclasspathfiles;
		NavigableMap<ReferencedFilePath, ? extends NavigableMap<SakerPath, ? extends ContentDescriptor>> prevdependencyclasspathfiles;
		if (prevInfo == null) {
			prevtestclasspathfiles = prevuserclasspathfiles = prevdependencyclasspathfiles = Collections
					.emptyNavigableMap();
		} else {
			prevtestclasspathfiles = prevInfo.getTestClassPathFiles();
			prevuserclasspathfiles = prevInfo.getUserClassPathFiles();
			prevdependencyclasspathfiles = prevInfo.getDependencyClassPathFiles();
		}

		NavigableSet<String> changedclassnames = new TreeSet<>();
		collectFileLocationChangedClassNamesInClassPath(testcpfilesmap, prevtestclasspathfiles, changedclassnames,
				inputfilechangedeltas, inputfileadditiondeltas, TestingFileTags.TEST_CLASSPATH);
		collectFileLocationChangedClassNamesInClassPath(usercpfilesmap, prevuserclasspathfiles, changedclassnames,
				inputfilechangedeltas, inputfileadditiondeltas, TestingFileTags.USER_CLASSPATH);
		collectFileLocationChangedClassNamesInClassPath(depcpfilesmap, prevdependencyclasspathfiles, changedclassnames,
				inputfilechangedeltas, inputfileadditiondeltas, TestingFileTags.DEPENDENCY_CLASSPATH);

//		collectChangedClassNamesInClassPath(testcpfilesmap, prevtestclasspathfiles, changedclassnames,
//				inputfilechangedeltas, inputfileadditiondeltas, TestingFileTags.TEST_CLASSPATH);
//		collectChangedClassNamesInClassPath(usercpfilesmap, prevuserclasspathfiles, changedclassnames,
//				inputfilechangedeltas, inputfileadditiondeltas, TestingFileTags.USER_CLASSPATH);
//		collectChangedClassNamesInClassPath(depcpfilesmap, prevdependencyclasspathfiles, changedclassnames,
//				inputfilechangedeltas, inputfileadditiondeltas, TestingFileTags.DEPENDENCY_CLASSPATH);

		NavigableMap<ReferencedFilePath, String> presentclassnamesbypath = new TreeMap<>();
		NavigableMap<ReferencedFilePath, String> testclassnamesbypath = new TreeMap<>();

//		collectClassNamesByPath(usercpfilesmap, presentclassnamesbypath);
//		collectClassNamesByPath(depcpfilesmap, presentclassnamesbypath);
//		collectClassNamesByPath(testcpfilesmap, testclassnamesbypath);
		collectFileLocationClassNamesByPath(usercpfilesmap, presentclassnamesbypath);
		collectFileLocationClassNamesByPath(depcpfilesmap, presentclassnamesbypath);
		collectFileLocationClassNamesByPath(testcpfilesmap, testclassnamesbypath);
		presentclassnamesbypath.putAll(testclassnamesbypath);

		retainClassNames = ImmutableUtils.makeImmutableNavigableSet(presentclassnamesbypath.values());

		Predicate<String> classnameincludepredicate = testClasses == null ? Functionals.alwaysPredicate()
				: createClassNameTestPredicate(testClasses);
		Predicate<String> nondeterministicpredicate = nonDeterministicTests == null ? Functionals.neverPredicate()
				: createClassNameTestPredicate(nonDeterministicTests);

		//determine the classes to run
		NavigableMap<IncrementalTestCaseResult, IncrementalTestCaseResult> teststorun = new TreeMap<>(
				IncrementalTestingHandler::compareByRuntimeAndSucceeded);

		NavigableMap<SakerPath, ContentDescriptor> classpathnottrackingfiles = new TreeMap<>();
		//TODO handle not tracked files
//		addUntrackedClassPathFiles(usercpfilesmap, classpathnottrackingfiles);
//		addUntrackedClassPathFiles(depcpfilesmap, classpathnottrackingfiles);
//		addUntrackedClassPathFiles(testcpfilesmap, classpathnottrackingfiles);
//		removeIgnoredFileChangesFromMap(ignoreFileChanges, classpathnottrackingfiles);

		NavigableMap<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> testclassfinders = collectFileLocationClassPath(
				testcpfilesmap.keySet());
		NavigableMap<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> userclassfinders = collectFileLocationClassPath(
				usercpfilesmap.keySet());
		NavigableMap<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> testrunnerclasspaths = collectFileLocationClassPath(
				testrunnercpfilelocations.keySet());

		resultinfo.setTestClassPathFiles(toReferencedFilePathMap(testcpfilesmap));
		resultinfo.setDependencyClassPathFiles(toReferencedFilePathMap(depcpfilesmap));
		resultinfo.setUserClassPathFiles(toReferencedFilePathMap(usercpfilesmap));

		NavigableMap<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> allclassfinders = new TreeMap<>(
				testclassfinders);
		allclassfinders.putAll(userclassfinders);
		allclassfinders.putAll(testrunnerclasspaths);

		test_invocation_checker_loop:
		for (Entry<ReferencedFilePath, String> entry : testclassnamesbypath.entrySet()) {
			ReferencedFilePath tcpath = entry.getKey();
			String cname = entry.getValue();
			if (!classnameincludepredicate.test(cname)) {
				continue test_invocation_checker_loop;
			}

			IncrementalTestCaseResult prevtc = prevTestCasesByPath.get(tcpath);
			if (prevtc == null) {
				if (verbose) {
					System.out.println("Invoking: " + cname + " because it's new.");
				}
				teststorun.put(new IncrementalTestCaseResult(cname, tcpath, 0), null);
				continue test_invocation_checker_loop;
			}
			if (changedclassnames.contains(cname)) {
				if (verbose) {
					System.out.println("Invoking: " + cname + " because class changed.");
				}
				teststorun.put(new IncrementalTestCaseResult(cname, tcpath, prevtc.getExecutionMilliSeconds()), prevtc);
				continue test_invocation_checker_loop;
			}
			TestCaseState prevstate = prevtc.getState();
			switch (prevstate) {
				case NEW: {
					if (verbose) {
						System.out.println("Invoking: " + cname + " because it's new.");
					}
					teststorun.put(new IncrementalTestCaseResult(cname, tcpath, prevtc.getExecutionMilliSeconds()),
							prevtc);
					continue test_invocation_checker_loop;
				}
				case FAILED: {
					if (nondeterministicpredicate.test(cname)) {
						if (verbose) {
							System.out.println("Invoking: " + cname + " because it's nondeterministic.");
						}
						teststorun.put(new IncrementalTestCaseResult(cname, tcpath, prevtc.getExecutionMilliSeconds()),
								prevtc);
						continue test_invocation_checker_loop;
					}
					break;
				}
				case SUCCESSFUL: {
					break;
				}
				default: {
					throw new IllegalArgumentException("Unknonw state: " + prevstate);
				}
			}

			NavigableSet<String> dependentclassnames = prevtc.getDependentClassNames();
			String dependentclasschanged = getAnyDependentClassChanged(dependentclassnames, changedclassnames, cname);
			if (dependentclasschanged != null) {
				//class file changed or
				//dependent classes were changed
				if (verbose) {
					System.out.println(
							"Invoking: " + cname + " because dependent class changed. (" + dependentclasschanged + ")");
				}
				teststorun.put(new IncrementalTestCaseResult(cname, tcpath, prevtc.getExecutionMilliSeconds()), prevtc);
				continue test_invocation_checker_loop;
			}
			FileContentModifyTag modifytag = new FileContentModifyTag(cname);

			FileChangeDelta inputmodifydelta = inputfilechangedeltas.getAnyFileDeltaWithTag(modifytag);
			if (inputmodifydelta != null) {
				if (verbose) {
					System.out
							.println("Invoking: " + cname + " because file changed: " + inputmodifydelta.getFilePath());
				}
				teststorun.put(new IncrementalTestCaseResult(cname, tcpath, prevtc.getExecutionMilliSeconds()), prevtc);
				continue test_invocation_checker_loop;
			}
			NavigableMap<String, ? extends NavigableMap<ReferencedFilePath, ? extends ContentDescriptor>> referencedclresources = prevtc
					.getReferencedClassLoaderResources();
			if (!ObjectUtils.isNullOrEmpty(referencedclresources)) {
				for (Entry<String, ? extends NavigableMap<ReferencedFilePath, ? extends ContentDescriptor>> resentry : referencedclresources
						.entrySet()) {
					String resname = resentry.getKey();
					NavigableMap<ReferencedFilePath, ? extends ContentDescriptor> resclexpectedcontents = resentry
							.getValue();
					boolean iterateres = ObjectUtils.iterateSortedMapEntriesBreak(resclexpectedcontents,
							allclassfinders, (k, contents, clfinder) -> {
								if (contents == null) {
									//the class loader data finder was added
									return false;
								}
								if (clfinder == null) {
									//the class loader data finder was removed
									return false;
								}
								Entry<SakerPath, ContentDescriptor> currentrescontentsentry = clfinder
										.getResourceFileContentDescriptor(resname);
								ContentDescriptor currentcontents;
								if (currentrescontentsentry == null) {
									currentcontents = NonExistentContentDescriptor.INSTANCE;
								} else {
									currentcontents = currentrescontentsentry.getValue();
								}
								if (currentcontents.isChanged(contents)) {
									return false;
								}
								return true;
							});
					if (!iterateres) {
						teststorun.put(new IncrementalTestCaseResult(cname, tcpath, prevtc.getExecutionMilliSeconds()),
								prevtc);
						if (verbose) {
							System.out.println(
									"Invoking: " + cname + " because class loader resource changed: " + resname);
						}
						continue test_invocation_checker_loop;
					}
				}
			}

			DirectoryContentFileAdditionTag additiontag = new DirectoryContentFileAdditionTag(cname);
			FileChangeDelta dirchangedelta = inputfileadditiondeltas.getAnyFileDeltaWithTag(additiontag);
			if (dirchangedelta != null
					|| (dirchangedelta = inputfilechangedeltas.getAnyFileDeltaWithTag(additiontag)) != null) {
				if (verbose) {
					System.out.println("Invoking: " + cname + " because directory entry changed in: "
							+ dirchangedelta.getFilePath().getParent());
				}
				teststorun.put(new IncrementalTestCaseResult(cname, tcpath, prevtc.getExecutionMilliSeconds()), prevtc);
				continue test_invocation_checker_loop;
			}
			//we haven't added the class to be run, reuse the previous test result
			if (prevstate != TestCaseState.SUCCESSFUL) {
				//if it wasnt successful, print the details
				//we dont have to rerun it as no related class files changed
				printTestResult(prevtc);
			}
			reportInputDependenciesForTestResult(prevtc);
			resultinfo.addTestCase(prevtc);
		}

		if (!teststorun.isEmpty()) {
			System.out.println("Running " + teststorun.size() + " tests.");
			ConcurrentPrependAccumulator<IncrementalTestCaseResult> testcasestorun = new ConcurrentPrependAccumulator<>(
					teststorun.navigableKeySet());
			//this is just a collector list to keep reference to the testers until the closing is done
			//it might be possible to have the remote testers garbage collected before the closing of the classloaders is done
			//avoid this by keeping a reference to the instantiated remote testers

			int maxjvmcount = Math.max(1, maxJVMCount);

			try {
				try (ResourceCloser rescloser = new ResourceCloser()) {
					ResourceDescriptorClassLoaderDataFinderSupplier[] usercp = userclassfinders.values()
							.toArray(EMPTY_RESOURCEDESCRIPTORCLASSLOADERDATAFINDERSUPPLIER_ARRAY);
					ResourceDescriptorClassLoaderDataFinderSupplier[] testclasscp = testclassfinders.values()
							.toArray(EMPTY_RESOURCEDESCRIPTORCLASSLOADERDATAFINDERSUPPLIER_ARRAY);
					ResourceDescriptorClassLoaderDataFinderSupplier[] testrunnerclasscp = testrunnerclasspaths.values()
							.toArray(EMPTY_RESOURCEDESCRIPTORCLASSLOADERDATAFINDERSUPPLIER_ARRAY);

//					ObjectUtils.transformArray(usercp, CloseProtectedClassLoaderDataFinder::new);
//					ObjectUtils.transformArray(testclasscp, CloseProtectedClassLoaderDataFinder::new);
//					ObjectUtils.transformArray(testrunnerclasscp, CloseProtectedClassLoaderDataFinder::new);

					BlockingQueue<Supplier<JavaTestingInvoker>> testinvokerqueue = new LinkedBlockingQueue<>();
					AtomicInteger invokerinstantiatenums = new AtomicInteger(0);

					TestRunnerInnerTaskFactory innertaskfactory = new TestRunnerInnerTaskFactory(this, testcasestorun,
							testinvokerqueue, invokerinstantiatenums, maxjvmcount);
					innertaskfactory.allclassfinders = allclassfinders;
					innertaskfactory.classpathnottrackingfiles = classpathnottrackingfiles;
					innertaskfactory.nondeterministicpredicate = nondeterministicpredicate;
					innertaskfactory.rescloser = rescloser;
					innertaskfactory.resultinfo = resultinfo;
					innertaskfactory.testclasscp = testclasscp;
					innertaskfactory.testerworkingdirpath = testerworkingdirpath;
					innertaskfactory.testrunnerclasscp = testrunnerclasscp;
					innertaskfactory.usercp = usercp;
					innertaskfactory.workingdirectoryactualpath = workingdirectoryactualpath;
					innertaskfactory.workmoddir = workmoddir;

					InnerTaskExecutionParameters innertaskparams = new InnerTaskExecutionParameters();
					//TODO make the testing remote dispatchable
					innertaskparams.setAllowedClusterEnvironmentIdentifiers(Collections
							.singleton(taskContext.getExecutionContext().getEnvironment().getEnvironmentIdentifier()));
					innertaskparams.setDuplicationCancellable(true);
					innertaskparams.setDuplicationPredicate(new FixedTaskDuplicationPredicate(teststorun.size()));
					if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_008) {
						innertaskparams.setMaxEnvironmentFactor(maxjvmcount);
					}
					InnerTaskResults<Void> testinnertaskresults = taskContext.startInnerTask(innertaskfactory,
							innertaskparams);

					boolean cancelled = false;
					while (true) {
						InnerTaskResultHolder<Void> testinnerres = testinnertaskresults.getNext();
						if (testinnerres == null) {
							break;
						}
						Throwable testexc = testinnerres.getExceptionIfAny();
						if (testexc != null) {
							exceptions.add(testexc);
							if (!cancelled) {
								testinnertaskresults.cancelDuplicationOptionally();
								cancelled = true;
							}
						}
					}
				}
			} finally {
				if (!testcasestorun.isEmpty()) {
					//add unrun tests from previous so they are not recognized as new in the next run
					//report build time dependency, so pending tests are run in the next build
					taskContext.reportExecutionDependency(BuildTimeExecutionProperty.INSTANCE, -1L);
					for (IncrementalTestCaseResult tcres : testcasestorun) {
						resultinfo.addTestCase(tcres);
					}
				}
			}
		}
		//a last notification
		printSuccessfulTestFinalNotification();
		return resultinfo;
	}

	private static class TestRunnerInnerTaskFactory implements TaskFactory<Void>, Task<Void> {
		private IncrementalTestingHandler testingHandler;
		private ConcurrentPrependAccumulator<IncrementalTestCaseResult> testsCasesToRun;
		private BlockingQueue<Supplier<JavaTestingInvoker>> testInvokerQueue;
		private AtomicInteger invokerInstantiationNumber;
		private int maxJVMCount;

		private ResourceCloser rescloser;
		private ResourceDescriptorClassLoaderDataFinderSupplier[] usercp;
		private ResourceDescriptorClassLoaderDataFinderSupplier[] testclasscp;
		private ResourceDescriptorClassLoaderDataFinderSupplier[] testrunnerclasscp;
		private Path testerworkingdirpath;
		private ConcurrentPrependAccumulator<RemoteTester> testers = new ConcurrentPrependAccumulator<>();

		private IncrementalTestingInfo resultinfo;
		private SakerDirectory workmoddir;
		private Path workingdirectoryactualpath;
		private NavigableMap<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> allclassfinders;
		private NavigableMap<SakerPath, ContentDescriptor> classpathnottrackingfiles;
		private Predicate<String> nondeterministicpredicate;

		public TestRunnerInnerTaskFactory(IncrementalTestingHandler testingHandler,
				ConcurrentPrependAccumulator<IncrementalTestCaseResult> testcasestorun,
				BlockingQueue<Supplier<JavaTestingInvoker>> testinvokerqueue, AtomicInteger invokerinstantiatenums,
				int maxJVMCount) {
			this.testingHandler = testingHandler;
			this.testsCasesToRun = testcasestorun;
			this.testInvokerQueue = testinvokerqueue;
			this.invokerInstantiationNumber = invokerinstantiatenums;
			this.maxJVMCount = maxJVMCount;
		}

		@Override
		public int getRequestedComputationTokenCount() {
			// TODO make RequestedComputationTokenCount value configureable for testing
			return 1;
		}

		@Override
		public Void run(TaskContext taskcontext) throws Exception {
			IncrementalTestCaseResult testcase = testsCasesToRun.take();
			if (testcase == null) {
				return null;
			}
			if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
				BuildTrace.setDisplayInformation(getClassSimpleNameFromBinaryName(testcase.getClassName()),
						testcase.getClassName());
			}
			Supplier<JavaTestingInvoker> invokersupplier;
			try {
				while ((invokersupplier = testInvokerQueue.poll()) == null) {
					int instantiatenum = invokerInstantiationNumber.get();
					if (instantiatenum < maxJVMCount) {
						//can possibly instantiate
						if (!invokerInstantiationNumber.compareAndSet(instantiatenum, instantiatenum + 1)) {
							//failed to increment, dont instantiate
							continue;
						}
						try {
							JavaTestingInvoker invoker = testingHandler.getTestingInvoker(taskcontext, instantiatenum,
									rescloser, usercp, testclasscp, testrunnerclasscp, testerworkingdirpath, testers);
							invokersupplier = Functionals.valSupplier(invoker);
						} catch (Throwable e) {
							//decrement back as the initialization failed
							invokerInstantiationNumber.decrementAndGet();
							testInvokerQueue.add(() -> {
								throw ObjectUtils.sneakyThrow(
										new JavaTestRunnerFailureException("Failed to initialize test runner.", e));
							});
							throw e;
						}
						break;
					}
					//we can't create more JVMs, wait for an invoker
					invokersupplier = testInvokerQueue.take();
					break;
				}

				try {
					JavaTestingInvoker invoker = invokersupplier.get();
					testingHandler.invokeTestingImpl(testerworkingdirpath, resultinfo, testcase, invoker, workmoddir,
							workingdirectoryactualpath, allclassfinders, classpathnottrackingfiles);
				} finally {
					testInvokerQueue.add(invokersupplier);
				}
			} catch (Throwable e) {
				testsCasesToRun.add(testcase);
				throw e;
			}
			if (!testcase.isSuccessful()) {
				//if the test failed, and is non deterministic, make sure we are called in the next build, even if there are no changes
				if (nondeterministicpredicate.test(testcase.getClassName())) {
					taskcontext.reportExecutionDependency(BuildTimeExecutionProperty.INSTANCE, -1L);
				}
				if (testingHandler.failFast) {
					//TODO reify exception
					throw new RuntimeException("Failed: " + testcase.getClassName());
				}
			}

			return null;
		}

		@Override
		public Task<? extends Void> createTask(ExecutionContext executioncontext) {
			return this;
		}

	}

	private void addUntrackedClassPathFiles(Map<SakerFile, ? extends NavigableMap<SakerPath, ?>> filesmap,
			Map<SakerPath, ContentDescriptor> classpathnottrackingfiles) {
		for (Entry<SakerFile, ?> entry : filesmap.entrySet()) {
			if (entry.getValue() == null) {
				SakerFile file = entry.getKey();
				classpathnottrackingfiles.put(file.getSakerPath(), file.getContentDescriptor());
			}
		}
	}

	private static String getAnyDependentClassChanged(NavigableSet<String> dependentclassnames,
			NavigableSet<String> changedclasses, String cname) {
		if (changedclasses.contains(cname)) {
			return cname;
		}
		//TODO use navigableset subset to reduce count
		String contained = ObjectUtils.getContainsAny(dependentclassnames, changedclasses);
		return contained;
	}

	private static class DirectoryContentFileAdditionTag implements Externalizable {
		private static final long serialVersionUID = 1L;

		private String className;

		public DirectoryContentFileAdditionTag() {
		}

		public DirectoryContentFileAdditionTag(String className) {
			this.className = className;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeUTF(className);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			className = in.readUTF();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((className == null) ? 0 : className.hashCode());
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
			DirectoryContentFileAdditionTag other = (DirectoryContentFileAdditionTag) obj;
			if (className == null) {
				if (other.className != null)
					return false;
			} else if (!className.equals(other.className))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "DirectoryContentFileAdditionTag [" + className + "]";
		}
	}

	private static class FileContentModifyTag implements Externalizable {
		private static final long serialVersionUID = 1L;

		private String className;

		/**
		 * For {@link Externalizable}.
		 */
		public FileContentModifyTag() {
		}

		public FileContentModifyTag(String className) {
			this.className = className;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeUTF(className);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			className = in.readUTF();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((className == null) ? 0 : className.hashCode());
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
			FileContentModifyTag other = (FileContentModifyTag) obj;
			if (className == null) {
				if (other.className != null)
					return false;
			} else if (!className.equals(other.className))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "FileContentModifyTag [" + className + "]";
		}
	}

	private static void removeIgnoredFileChangesFromMap(Set<SakerPath> ignoredfiles, NavigableMap<SakerPath, ?> map) {
		if (ignoredfiles.isEmpty()) {
			return;
		}
		for (SakerPath ignpath : ignoredfiles) {
			//remove the files which are not tracked
			SakerPathFiles.getPathSubMapDirectoryChildren(map, ignpath, true).clear();
		}
	}

	private void reportInputDependenciesForTestResult(IncrementalTestCaseResult tcres) {
		String classname = tcres.getClassName();

		NavigableMap<SakerPath, ? extends ContentDescriptor> referencedfiles = tcres.getReferencedFiles();
		FileContentModifyTag modifytag = new FileContentModifyTag(classname);
		if (!referencedfiles.isEmpty()) {
			NavigableMap<SakerPath, ? extends ContentDescriptor> reportfiles;
			if (ignoreFileChanges.isEmpty()) {
				reportfiles = referencedfiles;
			} else {
				reportfiles = new TreeMap<>(referencedfiles);
				removeIgnoredFileChangesFromMap(ignoreFileChanges, reportfiles);
			}
			taskContext.getTaskUtilities().reportInputFileDependency(modifytag, reportfiles);
		}

		NavigableMap<SakerPath, ? extends NavigableSet<String>> referenceddirectories = tcres
				.getReferencedDirectories();
		if (!ObjectUtils.isNullOrEmpty(referenceddirectories)) {
			DirectoryContentFileAdditionTag additiontag = new DirectoryContentFileAdditionTag(classname);
			for (Entry<SakerPath, ? extends NavigableSet<String>> entry : referenceddirectories.entrySet()) {
				NavigableSet<String> contents = entry.getValue();
				if (contents != null) {
					SakerPath path = entry.getKey();
					if (SakerPathFiles.hasPathOrParent(ignoreFileChanges, path)) {
						//do not report dependencies for ignored path
						continue;
					}
					reportInputFileDependenciesWithContent(path, additiontag, contents,
							CommonTaskContentDescriptors.PRESENT);
					taskContext.reportInputFileAdditionDependency(additiontag,
							DirectoryChildrenFileCollectionStrategy.create(path));
				}
			}
		}
	}

	private void reportInputFileDependenciesWithContent(SakerPath basepath, Object tag,
			NavigableSet<String> basepathfilenames, ContentDescriptor content) {
		NavigableMap<SakerPath, ContentDescriptor> contents = new SetTransformingNavigableMap<String, SakerPath, ContentDescriptor>(
				basepathfilenames) {
			@Override
			protected Map.Entry<SakerPath, ContentDescriptor> transformEntry(String e) {
				return ImmutableUtils.makeImmutableMapEntry(basepath.resolve(e), content);
			}
		};
		taskContext.getTaskUtilities().reportInputFileDependency(tag, contents);
	}

	private void invokeTestingImpl(final Path testerworkingdirpath, IncrementalTestingInfo resultinfo,
			IncrementalTestCaseResult tcres, JavaTestingInvoker invoker, SakerDirectory worksakerdir,
			Path actualworkingdirectorypath,
			NavigableMap<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> allclassfinders,
			NavigableMap<SakerPath, ContentDescriptor> classpathnottrackingfiles)
			throws JavaTestRunnerFailureException, Exception {
		try {
			String classname = tcres.getClassName();
			if (TestFlag.ENABLED) {
				TestFlag.metric().javaTestInvocation(classname);
			}
			long startnanos = System.nanoTime();
			NavigableMap<SakerPath, Path> trackeddirectories = new TreeMap<>();
			trackeddirectories.put(worksakerdir.getSakerPath(), actualworkingdirectorypath);
			TestCallResult callres = callTestWithClass(invoker, classname, worksakerdir, testerworkingdirpath,
					trackeddirectories, allclassfinders);
			JavaTestInvocationResult invocationres = callres.getInvocationResult();
			long endnanos = System.nanoTime();
			NavigableMap<SakerPath, ContentDescriptor> reffiles = callres.getReferencedFiles();
			NavigableMap<String, ? extends NavigableSet<String>> listeddirs = callres.getListedDirectories();

			NavigableMap<SakerPath, ContentDescriptor> referencedfiles = new TreeMap<>();
			NavigableMap<SakerPath, NavigableSet<String>> referenceddirectories = new TreeMap<>();

			for (Entry<String, ? extends NavigableSet<String>> entry : listeddirs.entrySet()) {
				Path dirpath = Paths.get(entry.getKey());
				if (!dirpath.isAbsolute()) {
					dirpath = testerworkingdirpath.resolve(dirpath).normalize();
				} else {
					dirpath = dirpath.normalize();
				}
				SakerPath unmirrored = executionContext.toUnmirrorPath(dirpath);
				SakerPath dirmpath = null;
				if (unmirrored != null) {
					dirmpath = unmirrored;
				} else {
					for (Entry<SakerPath, Path> trackentry : trackeddirectories.entrySet()) {
						Path trackedpath = trackentry.getValue();
						if (trackedpath != null) {
							if (dirpath.startsWith(trackedpath)) {
								Path relpath = trackedpath.relativize(dirpath);
								dirmpath = trackentry.getKey().resolve(relpath);
								break;
							}
						}
					}
				}
				if (dirmpath != null) {
					NavigableSet<String> contents = entry.getValue();
					referenceddirectories.put(dirmpath, contents);
				}
			}

			if (!reffiles.isEmpty()) {
				ExecutionPathConfiguration pathconfig = executionContext.getPathConfiguration();

				for (Entry<SakerPath, ContentDescriptor> entry : reffiles.entrySet()) {
					SakerPath path = entry.getKey();
					ContentDescriptor cdesc = entry.getValue();
					if (cdesc == CONTENT_WROTE_MARKER) {
						ProviderHolderPathKey pathkey = pathconfig.getPathKey(path);
						cdesc = taskContext.invalidateGetContentDescriptor(pathkey);

						if (cdesc == null) {
							cdesc = NonExistentContentDescriptor.INSTANCE;
						}
					}
					referencedfiles.put(path, cdesc);
				}
			}

			TreeSet<String> dependentresult = new TreeSet<>(invocationres.getDependentClasses());
			dependentresult.addAll(getAdditionalTestDependencyClasses(classname));
			boolean successful = invocationres.isSuccessful();
			tcres.setDependentClasses(dependentresult);
			tcres.setReferencedFiles(referencedfiles);
			tcres.setReferencedClassLoaderResources(callres.getReferencedClassLoaderResources());
			tcres.setReferencedDirectories(referenceddirectories);
			tcres.setState(successful ? TestCaseState.SUCCESSFUL : TestCaseState.FAILED);
			long millis = (endnanos - startnanos) / 1_000_000;
			tcres.setExecutionMilliSeconds(millis);
			if (!successful) {
				tcres.setFailureInformation(invocationres.getFailureInformation());
			}
			reportInputDependenciesForTestResult(tcres);
			resultinfo.addTestCase(tcres);

			if (TestFlag.ENABLED) {
				TestFlag.metric().javaTestReferencedFiles(classname, referencedfiles.keySet());
				TestFlag.metric().javaTestReferencedDirectories(classname, referenceddirectories.keySet());
				TestFlag.metric().javaTestDependentClasses(classname, invocationres.getDependentClasses());
			}
		} catch (Throwable e) {
			//in case of an error, add the previous test result to the info, as it has to be run again in the next build
			resultinfo.addTestCase(new IncrementalTestCaseResult(tcres.getClassName(), tcres.getClassFilePath(),
					tcres.getExecutionMilliSeconds()));
			throw e;
		}

		printTestResult(tcres);
	}

	private static Predicate<String> createClassNameTestPredicate(String wildcard) {
		WildcardPath wc = WildcardPath.valueOf(wildcard.replace('.', '/'));
		return c -> {
			c = c.replace('.', '/');
			return wc.includes(c);
		};
	}

	private static Predicate<String> createClassNameTestPredicate(Collection<String> wildcards) {
		if (wildcards.isEmpty()) {
			return Functionals.neverPredicate();
		}
		Iterator<String> it = wildcards.iterator();
		String first = it.next();
		Predicate<String> result = createClassNameTestPredicate(first);
		while (it.hasNext()) {
			result = result.or(createClassNameTestPredicate(it.next()));
		}
		return result;
	}

	private JavaTestingInvoker getTestingInvoker(TaskContext taskcontext, int identifier, ResourceCloser rescloser,
			ResourceDescriptorClassLoaderDataFinderSupplier[] userclasspathsuppliers,
			ResourceDescriptorClassLoaderDataFinderSupplier[] testclasspathsuppliers,
			ResourceDescriptorClassLoaderDataFinderSupplier[] testrunnerclasspathsuppliers, Path workingdir,
			ConcurrentPrependAccumulator<RemoteTester> testers) throws Exception {
		RemoteTester proc = getRMIProcess(identifier, workingdir);
		testers.add(proc);
		RMIVariables variables = proc.getRMIProcess().getConnection().newVariables();
		//XXX should be directly added
		rescloser.add(variables::close);

		ClassLoaderDataFinder[] userclasspath = classPathSuppliersToDataFinders(rescloser, userclasspathsuppliers,
				taskcontext, variables);
		ClassLoaderDataFinder[] testclasspath = classPathSuppliersToDataFinders(rescloser, testclasspathsuppliers,
				taskcontext, variables);
		ClassLoaderDataFinder[] testrunnerclasspath = classPathSuppliersToDataFinders(rescloser,
				testrunnerclasspathsuppliers, taskcontext, variables);

		Object remotesystemcl = variables.getRemoteContextVariable(TestInvokerDaemon.CONTEXT_VARIABLE_BASE_CLASSLOADER);

		Method wildcardfilterclmethod = ReflectUtils.getMethodAssert(WildcardFilteringClassLoader.class, "create",
				ClassLoader.class, CharSequence[].class);
		Object tapisharedcl = variables.invokeRemoteStaticMethod(wildcardfilterclmethod, remotesystemcl,
				new CharSequence[] { "saker.java.testing.**" });

		Object testrunnercl = variables.newRemoteInstance(ReflectUtils.getConstructorAssert(MultiDataClassLoader.class,
				ClassLoader.class, ClassLoaderDataFinder[].class), tapisharedcl, testrunnerclasspath);

		JavaTestingInvoker invoker;
		try {
			invoker = (JavaTestingInvoker) variables.newRemoteOnlyInstance(testrunnercl, testInvokerClassName);
		} catch (RMICallFailedException | ClassCastException e) {
			try {
				invoker = (JavaTestingInvoker) variables.newRemoteOnlyInstance(remotesystemcl, testInvokerClassName);
			} catch (RMICallFailedException | ClassCastException e2) {
				e2.addSuppressed(e);
				throw e2;
			}
		}

		rescloser.add(invoker);
		Object parentexctestrunnercl = variables.newRemoteInstance(
				ReflectUtils.getConstructorAssert(ParentExclusiveClassLoader.class, ClassLoader.class), testrunnercl);

		RMIVariables.invokeMethod(invoker, ReflectUtils.getMethodAssert(JavaTestingInvoker.class, "initTestRunner",
				ClassLoader.class, TestInvokerParameters.class), parentexctestrunnercl, testInvokerParameters);
		Method getuserclparentmethod = ReflectUtils.getMethodAssert(TestInvokerSupport.class,
				"getTestingUserClassLoaderParent");
		Object userclasspathparentcl = variables.invokeRemoteStaticMethod(getuserclparentmethod);
		//XXX is null fine for test class path parent?
		Object testclasspathparentcl = null;
		RMIVariables.invokeMethod(invoker,
				ReflectUtils.getMethodAssert(JavaTestingInvoker.class, "initClassLoaders", ClassLoader.class,
						ClassLoaderDataFinder[].class, ClassLoader.class, ClassLoaderDataFinder[].class),
				userclasspathparentcl, userclasspath, testclasspathparentcl, testclasspath);
		return invoker;
	}

	private static ClassLoaderDataFinder[] classPathSuppliersToDataFinders(ResourceCloser rescloser,
			ResourceDescriptorClassLoaderDataFinderSupplier[] classpathsuppliers, TaskContext taskcontext,
			RMIVariables rmivariables) throws IOException {
		ClassLoaderDataFinder[] classpath = new ClassLoaderDataFinder[classpathsuppliers.length];
		for (int i = 0; i < classpath.length; i++) {
			ClassLoaderDataFinder finder = classpathsuppliers[i].createDataFinder(taskcontext, rmivariables);
			rescloser.add(finder);
			classpath[i] = finder;
		}
		return classpath;
	}

	private RemoteTester getRMIProcess(int identifier, Path workingdir) throws Exception {
		SakerPath javaexesdkpath = javaSDK.getPath(SakerJavaCompilerUtils.JAVASDK_PATH_JAVA_EXE);
		if (javaexesdkpath == null) {
			throw new IllegalArgumentException("Java executable SDK path not found in: " + javaSDK + " for identifier: "
					+ SakerJavaCompilerUtils.JAVASDK_PATH_JAVA_EXE);
		}
		String sdkmajor = javaSDK.getProperty(SakerJavaCompilerUtils.JAVASDK_PROPERTY_JAVA_MAJOR);
		if (sdkmajor == null) {
			throw new IllegalArgumentException("Java major version SDK property not found in: " + javaSDK
					+ " for identifier: " + SakerJavaCompilerUtils.JAVASDK_PROPERTY_JAVA_MAJOR);
		}
		Path exepath = LocalFileProvider.toRealPath(javaexesdkpath);
		int javamajor = Integer.parseInt(sdkmajor);
		return executionContext.getEnvironment()
				.getCachedData(new RemoteJavaTesterCacheKey(executionContext.getEnvironment(), exepath, sakerJarFile,
						workingdir, identifier, processJVMArguments, javamajor));
	}

	private static int compareByRuntimeAndSucceeded(IncrementalTestCaseResult l, IncrementalTestCaseResult r) {
		int scmp = Boolean.compare(l.isSuccessful(), r.isSuccessful());
		if (scmp != 0) {
			return scmp;
		}
		int mcmp = Long.compare(l.getExecutionMilliSeconds(), r.getExecutionMilliSeconds());
		if (mcmp != 0) {
			return mcmp;
		}
		return l.getClassName().compareTo(r.getClassName());
	}

	private synchronized void printTestResult(IncrementalTestCaseResult result) {
		printSuccessfulTestNotification();
		if (result.isSuccessful()) {
			if (verbose) {
				SakerLog.success().verbose().println("Test " + result.getClassName() + " ("
						+ DateUtils.durationToString(result.getExecutionMilliSeconds()) + ")");
			} else {
				AIFU_completedSuccessfulTestCount.getAndIncrement(this);
			}

			if (TestFlag.ENABLED) {
				TestFlag.metric().javaTestSuccessful(result.getClassName());
			}
		} else {
			String failinfo = result.getFailInformation();
			if (!ObjectUtils.isNullOrEmpty(failinfo)) {
				SakerLog.info().println("Test information: " + result.getClassName());
				SakerLog.log().println(failinfo);
			}
			SakerLog.error().println("Test failed: " + result.getClassName() + " ("
					+ DateUtils.durationToString(result.getExecutionMilliSeconds()) + ")");

			if (TestFlag.ENABLED) {
				TestFlag.metric().javaTestFailed(result.getClassName());
			}
		}
	}

	private void printSuccessfulTestNotification() {
		long nanos = System.nanoTime();
		long prevprintoutnanos = lastSuccessfulTestPrintoutNanos;
		if (nanos - prevprintoutnanos >= DateUtils.NANOS_PER_SECOND) {
			//haven't printed out info to the user in the last second. print something
			if (ALFU_lastSuccessfulTestPrintoutNanos.compareAndSet(this, prevprintoutnanos, nanos)) {
				int prevsuccessfultestcount = AIFU_completedSuccessfulTestCount.getAndSet(this, 0);
				if (prevsuccessfultestcount > 0) {
					SakerLog.success().verbose()
							.println("Testing in progress... (Completed " + prevsuccessfultestcount + ")");
				}
			}
		}
	}

	private void printSuccessfulTestFinalNotification() {
		ALFU_lastSuccessfulTestPrintoutNanos.set(this, System.nanoTime());
		int prevsuccessfultestcount = AIFU_completedSuccessfulTestCount.getAndSet(this, 0);
		if (prevsuccessfultestcount > 0) {
			SakerLog.success().verbose().println("Testing in progress... (Completed " + prevsuccessfultestcount + ")");
		}
	}

	private JavaTestingFileProvider createTestingFileProvider(ExecutionContext executionContext,
			SakerDirectory workingdir, Path testerworkingdir, TestCallResult callresult,
			NavigableMap<SakerPath, Path> trackeddirectories,
			NavigableMap<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> allclassfinders) {
		SakerPath directoryPath = workingdir.getSakerPath();
		if (directoryPath.isRelative()) {
			//directory was replaced meanwhile
			throw new IllegalStateException("Working directory is no longer present: " + this.workingDirectory);
		}
		return new SynchronizingTestingFileProvider(executionContext, workingdir, directoryPath, testerworkingdir,
				callresult, trackeddirectories, allclassfinders);
	}

	private static final NullContentDescriptor CONTENT_WROTE_MARKER = new NullContentDescriptor();

	//TODO we might need to move this outside of this class, to keep a common instace between class tests
	private final ConcurrentSkipListMap<SakerPath, Object> synchronizeLocks = new ConcurrentSkipListMap<>();

	private class SynchronizingTestingFileProvider implements JavaTestingFileProvider {
		//XXX make a more specific implementation that doesn't synchronize the contents of a parent directory, only when they are requested as well

		// TODO clear up code comments

		private final ExecutionContext executionContext;
//		private final ContentDatabase contentDatabase;
		private final SakerDirectory workingDirectory;
		private final SakerPath workingDirectorySakerPath;
		private final Path testerWorkingDirectoryPath;

		private final TestCallResult callResult;

		private final LocalFileProvider localFiles = LocalFileProvider.getInstance();

		//TODO add option to add more to this
		private final NavigableMap<SakerPath, Path> trackedDirectories;

//		private final ConcurrentSkipListMap<SakerPath, NavigableSet<String>> listedDirectoryContents = new ConcurrentSkipListMap<>();
		private Path mirrorDirectoryPath;
		private NavigableMap<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> allClassFinders;

		public SynchronizingTestingFileProvider(ExecutionContext executionContext, SakerDirectory workingDirectory,
				SakerPath workingdirectorysakerpath, Path testerworkingdirectory, TestCallResult callresult,
				NavigableMap<SakerPath, Path> trackedDirectories,
				NavigableMap<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> allclassfinders) {
			this.executionContext = executionContext;
//			this.contentDatabase = executionContext.getContentDatabase();
			this.workingDirectory = workingDirectory;
			this.workingDirectorySakerPath = workingdirectorysakerpath;
			this.testerWorkingDirectoryPath = testerworkingdirectory;
			this.callResult = callresult;
			this.allClassFinders = allclassfinders;

			this.mirrorDirectoryPath = executionContext.getMirrorDirectory();

			this.trackedDirectories = trackedDirectories;
		}

		@Override
		public void requestClassLoaderResource(String name) {
			if (name == null) {
				//it shouldn't be null, but check more than less
				return;
			}
			NavigableMap<ReferencedFilePath, ContentDescriptor> contentdescriptors = new TreeMap<>();
			NavigableMap<ReferencedFilePath, ContentDescriptor> prevcontentdescriptors = callResult.referencedClassLoaderResources
					.putIfAbsent(name, contentdescriptors);
			if (prevcontentdescriptors != null) {
				//referenced multiple times, no need to record it more 
				return;
			}
			for (Entry<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> entry : allClassFinders
					.entrySet()) {
				ResourceDescriptorClassLoaderDataFinderSupplier clfinder = entry.getValue();
				Entry<SakerPath, ContentDescriptor> resourceassociation = clfinder
						.getResourceFileContentDescriptor(name);
				if (resourceassociation != null) {
					contentdescriptors.put(entry.getKey(), resourceassociation.getValue());
					putReferencedFileIfAbsent(resourceassociation.getKey(), resourceassociation.getValue());
				} else {
					contentdescriptors.put(entry.getKey(), NonExistentContentDescriptor.INSTANCE);
				}
			}
		}

		protected Path toAbsoluteNormalizedAccessPath(String path) {
			Path ppath = Paths.get(path).normalize();
			if (!ppath.isAbsolute()) {
				ppath = testerWorkingDirectoryPath.resolve(ppath);
			}
			ppath = ppath.normalize();
			return ppath;
		}

		private void doWithFilePath(String path, TriConsumer<SakerFile, Path, SakerPath> consumer) {
			Path ppath = toAbsoluteNormalizedAccessPath(path);
			SakerPath unmirrored = executionContext.toUnmirrorPath(ppath);
			SakerFile f = null;
			SakerPath filesakerpath = null;
			if (unmirrored != null) {
				//the test is accessing a file inside the mirror directory
				//check if the chosen directory is tracked
				if (!SakerPathFiles.hasPathOrParent(trackedDirectories.navigableKeySet(), unmirrored)) {
					return;
				}
				f = SakerPathFiles.resolveAtAbsolutePath(executionContext, unmirrored);
				filesakerpath = unmirrored;
			} else {
				for (Entry<SakerPath, Path> entry : trackedDirectories.entrySet()) {
					Path entrypath = entry.getValue();
					if (entrypath == null) {
						continue;
					}
					if (!ppath.startsWith(entrypath)) {
						continue;
					}
					//the tracked directory is on the local filesystem
					//the test is accessing a file inside the tracked directory
					SakerPath relmodpath = SakerPath.valueOf(entrypath.relativize(ppath));
					f = SakerPathFiles.resolveAtRelativePath(workingDirectory, relmodpath);
					filesakerpath = workingDirectorySakerPath.resolve(relmodpath);
					break;
				}
			}
			if (filesakerpath == null) {
				//no saker file found for the path
				//not in a tracked directory
				return;
			}

			consumer.accept(f, ppath, filesakerpath);
		}

		@Override
		public void requestFileRead(String path) {
			doWithFilePath(path, (f, ppath, filesakerpath) -> {
				synchronize(f, ppath, filesakerpath);
				//XXX the following is not necessary, as synchronize already puts the referenced file.
//				callResult.referencedFiles.computeIfAbsent(filesakerpath, p -> {
//					ContentDescriptor cdres = contentDatabase.getContentDescriptor(localFiles.getPathKey(ppath));
//					if (cdres == null) {
//						return NonExistentContentDescriptor.INSTANCE;
//					}
//					return cdres;
//				});
			});
		}

		@Override
		public void requestFileWrite(String path) {
			//a read must precede a write request
			Path ppath = toAbsoluteNormalizedAccessPath(path);
			taskContext.invalidate(localFiles.getPathKey(ppath));
			doWithFilePath(path, (f, diskpath, filesakerpath) -> {
				callResult.referencedFiles.put(filesakerpath, CONTENT_WROTE_MARKER);
			});

//			Path ppath = Paths.get(path).normalize();
//			Path absppath;
//			if (ppath.isAbsolute()) {
//				if (!ppath.startsWith(testerWorkingDirectoryPath)) {
//					contentDatabase.invalidate(localFiles.getPathKey(ppath));
//					return;
//				}
//				absppath = ppath;
//				ppath = testerWorkingDirectoryPath.relativize(ppath);
//			} else {
//				absppath = testerWorkingDirectoryPath.resolve(ppath).normalize();
//			}
//			SakerPath absmpath = workingDirectorySakerPath.resolve(ppath);
//
//			synchronize(absmpath.getParent());
//			contentDatabase.invalidate(localFiles.getPathKey(absppath));
		}

		@Override
		public void requestFileList(String path) {
//			doWithFilePath(path, (f, ppath, filemodpath) -> {
//				NavigableSet<String> listed = listedDirectoryContents.get(filemodpath);
//				if (listed == null) {
//					throw new AssertionError("Directory was not listed, but is requested: " + filemodpath);
//				}
//				callResult.listedDirectories.putIfAbsent(filemodpath, listed);
//			});

//			Path ppath = Paths.get(path).normalize();
//			if (ppath.isAbsolute()) {
//				if (!ppath.startsWith(testerWorkingDirectoryPath)) {
//					return;
//				}
//				ppath = testerWorkingDirectoryPath.relativize(ppath);
//			}
//
//			SakerPath relativempath = SakerPath.valueOf(ppath);
//
//			NavigableMap<String, ContentDescriptor> listed = listedDirectoryContents.get(relativempath);
//			if (listed == null) {
//				throw new AssertionError("Directory was not listed, but is requested: " + relativempath);
//			}
//			SakerPath absmpath = workingDirectorySakerPath.resolve(ppath);
//
//			callResult.listedDirectories.putIfAbsent(absmpath, listed);
		}

		private void synchronize(SakerFile f, Path path, SakerPath filesakerpath) {
			if (filesakerpath == null
					|| !SakerPathFiles.hasPathOrParent(trackedDirectories.navigableKeySet(), filesakerpath)) {
				return;
			}
			Object lock = new Object();
			synchronized (lock) {
				Object prevlock = synchronizeLocks.putIfAbsent(filesakerpath, lock);
				if (prevlock != null) {
					synchronized (prevlock) {
						//do nothing, but wait for the synchronization to finish by trying to enter the lock
					}
					putReferencedFileIfAbsent(filesakerpath,
							f == null ? NonExistentContentDescriptor.INSTANCE : f.getContentDescriptor());
					return;
				}
				//TODO we should use NullContentDescriptor instead of non existent when the synchronization has failed?
				ProviderHolderPathKey pathkey = localFiles.getPathKey(path);
				if (f == null) {
					try {
						// make sure that there is no actual file present (maybe a leftover from something?)
//						contentDatabase.delete(pathkey);
						localFiles.deleteRecursively(path);
					} catch (IOException e) {
						//XXX do not print exception?
						e.printStackTrace();
					}
					taskContext.invalidate(pathkey);
					putReferencedFile(filesakerpath, NonExistentContentDescriptor.INSTANCE);
//					listedDirectoryContents.put(filemodpath, LIST_SYNC_FAILED_MARKER_MAP);
					return;
				}
				synchronize(f.getParent(), path.getParent(), filesakerpath.getParent());
				if (f instanceof SakerDirectory) {
					try {
						((SakerDirectory) f).synchronize(pathkey, DirectoryVisitPredicate.children());
//						NavigableMap<String, SakerFile> syncedchildren = ((SakerDirectory) f).synchronizeChildren(pathkey, 1,
//								Functionals.alwaysPredicate());
//						listedDirectoryContents.put(filemodpath, new TreeSet<>(syncedchildren.navigableKeySet()));
						putReferencedFile(filesakerpath, f.getContentDescriptor());
					} catch (IOException e) {
						// TODO: handle exception
						e.printStackTrace();
//						listedDirectoryContents.put(filemodpath, LIST_SYNC_FAILED_MARKER_MAP);
						putReferencedFile(filesakerpath, NonExistentContentDescriptor.INSTANCE);
					}
				} else {
					try {
						f.synchronize(pathkey);
						putReferencedFile(filesakerpath, f.getContentDescriptor());
					} catch (IOException e) {
						// TODO: handle exception
						e.printStackTrace();
						putReferencedFile(filesakerpath, NonExistentContentDescriptor.INSTANCE);
					}
				}
			}
		}

		private void putReferencedFile(SakerPath path, ContentDescriptor cd) {
			ContentDescriptor prev = callResult.referencedFiles.put(path, cd);
			if (prev != null) {
				throw new AssertionError(
						"Content descriptor for path: " + path + " is added multiple times: " + prev + " - " + cd);
			}
		}

		private void putReferencedFileIfAbsent(SakerPath path, ContentDescriptor cd) {
			callResult.referencedFiles.putIfAbsent(path, cd);
		}

//		private ContentDescriptor synchronize(SakerPath absolutempath) {
//			if (absolutempath == null || !absolutempath.startsWith(workingDirectorySakerPath)) {
//				return null;
//			}
//			synchronize(absolutempath.getParent());
//			SakerPath relativepath = workingDirectorySakerPath.relativize(absolutempath);
//			SakerFile f = workingDirectory.getAtPath(relativepath);
//			boolean synced = synchronizeWithDepth(relativepath, f, 1);
//			if (synced) {
//				if (f == null) {
//					return NonExistentContentDescriptor.INSTANCE;
//				}
//				return f.getContentDescriptor();
//			}
//			return null;
//		}
//
//		private boolean synchronizeWithDepth(SakerPath relativepath, SakerFile f, int depth) {
//			SakerPath mpath = workingDirectorySakerPath.resolve(relativepath);
//			ProviderHolderPathKey pathkey = new SimpleProviderHolderPathKey(SakerPath.valueOf(testerWorkingDirectoryPath.resolve(relativepath.toString())),
//					localFiles);
//			Object lock = new Object();
//			synchronized (lock) {
//				Object prevlock = synchronizeLocks.putIfAbsent(mpath, lock);
//				if (prevlock != null) {
//					synchronized (prevlock) {
//						//do nothing, but wait for the synchronization to finish by trying to enter the lock
//					}
//					return false;
//				}
//				if (f == null) {
//					try {
//						// make sure that there is no actual file present (maybe a leftover from something?)
//						contentDatabase.delete(pathkey);
//					} catch (IOException e) {
//						//XXX do not print exception?
//						e.printStackTrace();
//					}
//					return true;
//				}
//				try {
//					if (f instanceof SakerDirectory) {
//						//TODO do not synchronize already overwritten files
//						NavigableMap<String, SakerFile> syncedchildren = ((SakerDirectory) f).synchronizeChildren(pathkey, depth,
//								Functionals.alwaysPredicate());
//						listedDirectoryContents.put(relativepath, SakerPathFiles.toFileContentMap(syncedchildren));
//					} else {
//						f.synchronize(pathkey);
//					}
//				} catch (IOException e) {
//					//XXX dont print this stacktrace?
//					e.printStackTrace();
//				}
//				return true;
//			}
//		}
	}

	private static class TestCallResult {
		protected JavaTestInvocationResult invocationResult;
		protected NavigableMap<SakerPath, ContentDescriptor> referencedFiles = new ConcurrentSkipListMap<>();
		protected NavigableMap<String, NavigableMap<ReferencedFilePath, ContentDescriptor>> referencedClassLoaderResources = new ConcurrentSkipListMap<>();
//		protected NavigableMap<SakerPath, NavigableSet<String>> listedDirectories = new ConcurrentSkipListMap<>();

		public TestCallResult() {
		}

		public void setInvocationResult(JavaTestInvocationResult invocationResult) {
			this.invocationResult = invocationResult;
		}

		public JavaTestInvocationResult getInvocationResult() {
			return invocationResult;
		}

		public NavigableMap<SakerPath, ContentDescriptor> getReferencedFiles() {
			return referencedFiles;
		}

		public NavigableMap<String, ? extends NavigableSet<String>> getListedDirectories() {
			return invocationResult.getListedDirectories();
		}

		public NavigableMap<String, NavigableMap<ReferencedFilePath, ContentDescriptor>> getReferencedClassLoaderResources() {
			return referencedClassLoaderResources;
		}
//		public NavigableMap<SakerPath, NavigableSet<String>> getListedDirectories() {
//			return listedDirectories;
//		}
	}

	private TestCallResult callTestWithClass(JavaTestingInvoker invoker, String classname, SakerDirectory worksakerdir,
			Path testerworkingdir, NavigableMap<SakerPath, Path> trackeddirectories,
			NavigableMap<ReferencedFilePath, ResourceDescriptorClassLoaderDataFinderSupplier> allclassfinders)
			throws JavaTestRunnerFailureException, Exception {
		Map<String, String> userparams = new TreeMap<>();
		for (Entry<Predicate<String>, Map<String, String>> entry : testClassParameters.entrySet()) {
			if (entry.getKey().test(classname)) {
				userparams.putAll(entry.getValue());
			}
		}
		TestInvocationParameters parameters = new TestInvocationParameters(classname, userparams);

		TestCallResult callresult = new TestCallResult();
		JavaTestingFileProvider fileprovider = createTestingFileProvider(executionContext, worksakerdir,
				testerworkingdir, callresult, trackeddirectories, allclassfinders);
		JavaTestInvocationResult invocationresult = invoker.invokeTest(fileprovider, parameters);
		Integer exitcode = invocationresult.getExitCode();

		if (exitcode != null) {
			invocationresult.setSuccessful(successExitCodes.contains(exitcode));
		}

		Set<String> dependentclasses = invocationresult.getDependentClasses();
//		dependentclasses.retainAll(retainClassNames);
		dependentclasses.add(classname);

		callresult.setInvocationResult(invocationresult);
		return callresult;
	}

//	private Path getWorkingDirectoryPath() {
//		final Path workingdir;
//		if (this.workingDirectory != null) {
//			workingdir = this.workingDirectory;
//		} else {
//			Path cwdir = executionContext.getWorkingDirectory().toPath();
//			if (cwdir == null) {
//				workingdir = Paths.get(System.getProperty("user.dir"));
//			} else {
//				workingdir = cwdir;
//			}
//		}
//		return workingdir;
//	}

	private Map<FileLocation, NavigableMap<SakerPath, ? extends ContentDescriptor>> collectFileLocationClassFileConentsWithImplementationDependencyReporting(
			JavaClassPath classpath, Object tag) throws IOException {
		if (classpath == null) {
			return Collections.emptyMap();
		}
		LinkedHashMap<FileLocation, NavigableMap<SakerPath, ? extends ContentDescriptor>> result = new LinkedHashMap<>();
		classpath.accept(new ClassPathVisitor() {
			private Set<JavaCompilationWorkerTaskIdentifier> handledWorkerTaskIds = new HashSet<>();

			@Override
			public void visit(ClassPathReference classpath) {
				Collection<? extends ClassPathEntry> entries = classpath.getEntries();
				if (ObjectUtils.isNullOrEmpty(entries)) {
					SakerLog.warning().println("No class path entries found for: " + classpath);
					return;
				}
				for (ClassPathEntry entry : entries) {
					if (entry == null) {
						SakerLog.warning().println("Class path entry is null for: " + classpath);
						continue;
					}
					FileLocation filelocation = entry.getFileLocation();
					if (filelocation == null) {
						SakerLog.warning().println("No class path file location for: " + entry);
						continue;
					}
					handleFileLocation(filelocation);

					Collection<? extends ClassPathReference> additionalclasspaths = entry
							.getAdditionalClassPathReferences();
					if (!ObjectUtils.isNullOrEmpty(additionalclasspaths)) {
						JavaClassPathBuilder additionalcpbuilder = JavaClassPathBuilder.newBuilder();
						for (ClassPathReference additionalcp : additionalclasspaths) {
							additionalcpbuilder.addClassPath(additionalcp);
						}
						JavaClassPath additionalcp = additionalcpbuilder.build();
						additionalcp.accept(this);
					}
				}
			}

			@Override
			public void visit(CompilationClassPath classpath) {
				JavaCompilationWorkerTaskIdentifier workertaskid = classpath.getCompilationWorkerTaskIdentifier();
				if (!handledWorkerTaskIds.add(workertaskid)) {
					//don't get the task result to not install another dependency
					return;
				}
				TaskDependencyFuture<?> depresult = taskContext.getTaskDependencyFuture(workertaskid);
				JavaCompilerWorkerTaskOutput output = (JavaCompilerWorkerTaskOutput) depresult.getFinished();
				JavaClassPath classpathcompilecp = output.getClassPath();
				SakerPath classdirpath = output.getClassDirectory();
				ExecutionFileLocation filelocation = ExecutionFileLocation.create(classdirpath);

				SakerDirectory classesdir = taskContext.getTaskUtilities().resolveDirectoryAtPath(classdirpath);
				if (classesdir == null) {
					throw ObjectUtils.sneakyThrow(
							new FileNotFoundException("Compilation class directory not found: " + classesdir));
				}

				Object implversionkey = output.getImplementationVersionKey();
				if (implversionkey != null) {
					depresult.setTaskOutputChangeDetector(SakerJavaCompilerUtils
							.getCompilerOutputImplementationVersionKeyTaskOutputChangeDetector(implversionkey));
					depresult.setTaskOutputChangeDetector(SakerJavaCompilerUtils
							.getCompilerOutputClassPathTaskOutputChangeDetector(classpathcompilecp));
				}
				FileCollectionStrategy classfileadditiondep = RecursiveIgnoreCaseExtensionFileCollectionStrategy
						.create(classdirpath, "." + EXTENSION_CLASSFILE);
				NavigableMap<SakerPath, SakerFile> classfiles = taskContext.getTaskUtilities()
						.collectFilesReportInputFileAndAdditionDependency(tag, classfileadditiondep);

				NavigableMap<SakerPath, ContentDescriptor> contentmap = SakerPathFiles.toFileContentMap(classfiles);
				result.put(filelocation, contentmap);

				if (classpathcompilecp != null) {
					classpathcompilecp.accept(this);
				}
			}

			@Override
			public void visit(FileClassPath classpath) {
				FileLocation location = classpath.getFileLocation();
				handleFileLocation(location);
			}

			@Override
			public void visit(SDKClassPath classpath) {
				// TODO handle SDK class paths in testing
				ClassPathVisitor.super.visit(classpath);
			}

			private void handleFileLocation(FileLocation location) {
				if (result.containsKey(location)) {
					return;
				}
				@SuppressWarnings({ "unchecked", "rawtypes" })
				NavigableMap<SakerPath, ? extends ContentDescriptor>[] cdres = new NavigableMap[] { null };
				location.accept(new FileLocationVisitor() {
					@Override
					public void visit(ExecutionFileLocation loc) {
						SakerPath path = loc.getPath();
						SakerFile cpfile = taskContext.getTaskUtilities().resolveAtPath(path);
						if (cpfile == null) {
							throw ObjectUtils
									.sneakyThrow(new FileNotFoundException("Class path file not found: " + path));
						}

						if (cpfile instanceof SakerDirectory) {
							FileCollectionStrategy classfileadditiondep = RecursiveIgnoreCaseExtensionFileCollectionStrategy
									.create(path, "." + EXTENSION_CLASSFILE);
							NavigableMap<SakerPath, SakerFile> classfiles = taskContext.getTaskUtilities()
									.collectFilesReportInputFileAndAdditionDependency(tag, classfileadditiondep);
							cdres[0] = SakerPathFiles.toFileContentMap(classfiles);
						} else {
							taskContext.getTaskUtilities().reportInputFileDependency(tag, cpfile);
							cdres[0] = null;
						}
					}

					@Override
					public void visit(LocalFileLocation loc) {
						SakerPath path = loc.getLocalPath();
						TaskExecutionUtilities taskutils = taskContext.getTaskUtilities();
						ContentDescriptor cd = taskutils.getReportExecutionDependency(
								new LocalPathFileContentDescriptorExecutionProperty(path));
						if (cd == null) {
							throw ObjectUtils
									.sneakyThrow(new FileNotFoundException("Class path local file not found: " + path));
						}

						if (DirectoryContentDescriptor.INSTANCE.equals(cd)) {
							//the class path denotes a directory
							//add the dependencies on the class files

							LocalDirectoryClassFilesExecutionProperty.PropertyValue pval = taskutils
									.getReportExecutionDependency(new LocalDirectoryClassFilesExecutionProperty(path));
							cdres[0] = pval.getContents();
						} else {
							//TODO detect changes
							cdres[0] = null;
						}
					}
				});
				result.put(location, cdres[0]);
			}
		});
		return result;
	}

	/**
	 * @return The content descriptors may be <code>null</code>.
	 */
	//TODO duplicated with JavaTaskUtils
	private static Map<FileLocation, ContentDescriptor> collectFileLocationsWithImplementationDependencyReporting(
			TaskContext taskcontext, JavaClassPath classpath, Object tag, Map<String, SDKReference> sdks)
			throws IOException {
		if (classpath == null) {
			return Collections.emptyMap();
		}
		Map<FileLocation, ContentDescriptor> result = new LinkedHashMap<>();
		classpath.accept(new ClassPathVisitor() {
			private Set<JavaCompilationWorkerTaskIdentifier> handledWorkerTaskIds = new HashSet<>();

			@Override
			public void visit(ClassPathReference classpath) {
				Collection<? extends ClassPathEntry> entries = classpath.getEntries();
				if (ObjectUtils.isNullOrEmpty(entries)) {
					SakerLog.warning().println("No class path entries found for: " + classpath);
					return;
				}
				for (ClassPathEntry entry : entries) {
					if (entry == null) {
						SakerLog.warning().println("Class path entry is null for: " + classpath);
						continue;
					}
					FileLocation filelocation = entry.getFileLocation();
					if (filelocation == null) {
						SakerLog.warning().println("No class path file location for: " + entry);
						continue;
					}
					if (result.containsKey(filelocation)) {
						continue;
					}
					ContentDescriptor cd = handleFileLocation(filelocation);
					result.put(filelocation, cd);

					Collection<? extends ClassPathReference> additionalclasspaths = entry
							.getAdditionalClassPathReferences();
					if (!ObjectUtils.isNullOrEmpty(additionalclasspaths)) {
						JavaClassPathBuilder additionalcpbuilder = JavaClassPathBuilder.newBuilder();
						for (ClassPathReference additionalcp : additionalclasspaths) {
							additionalcpbuilder.addClassPath(additionalcp);
						}
						JavaClassPath additionalcp = additionalcpbuilder.build();
						additionalcp.accept(this);
					}
				}
			}

			@Override
			public void visit(CompilationClassPath classpath) {
				JavaCompilationWorkerTaskIdentifier workertaskid = classpath.getCompilationWorkerTaskIdentifier();
				if (!handledWorkerTaskIds.add(workertaskid)) {
					//don't get the task result to not install another dependency
					return;
				}
				TaskDependencyFuture<?> depresult = taskcontext.getTaskDependencyFuture(workertaskid);
				JavaCompilerWorkerTaskOutput output = (JavaCompilerWorkerTaskOutput) depresult.getFinished();
				SakerPath classdirpath = output.getClassDirectory();
				ExecutionFileLocation filelocation = ExecutionFileLocation.create(classdirpath);
				JavaClassPath outputcp = output.getClassPath();

				Object implversionkey = output.getImplementationVersionKey();
				if (implversionkey != null) {
					depresult.setTaskOutputChangeDetector(SakerJavaCompilerUtils
							.getCompilerOutputImplementationVersionKeyTaskOutputChangeDetector(implversionkey));
					depresult.setTaskOutputChangeDetector(
							SakerJavaCompilerUtils.getCompilerOutputClassPathTaskOutputChangeDetector(outputcp));
					result.put(filelocation, new SerializableContentDescriptor(implversionkey));
				} else {
					SakerDirectory classesdir = taskcontext.getTaskUtilities().resolveDirectoryAtPath(classdirpath);
					if (classesdir == null) {
						throw ObjectUtils.sneakyThrow(
								new FileNotFoundException("Compilation class directory not found: " + classesdir));
					}

					FileCollectionStrategy classfileadditiondep = RecursiveIgnoreCaseExtensionFileCollectionStrategy
							.create(classdirpath, "." + EXTENSION_CLASSFILE);
					NavigableMap<SakerPath, SakerFile> classfiles = taskcontext.getTaskUtilities()
							.collectFilesReportInputFileAndAdditionDependency(tag, classfileadditiondep);

					NavigableMap<SakerPath, ContentDescriptor> contentmap = SakerPathFiles.toFileContentMap(classfiles);
					result.put(filelocation, new MultiPathContentDescriptor(contentmap));
				}
				if (outputcp != null) {
					outputcp.accept(this);
				}
			}

			@Override
			public void visit(FileClassPath classpath) {
				FileLocation location = classpath.getFileLocation();
				if (result.containsKey(location)) {
					return;
				}
				ContentDescriptor cd = handleFileLocation(location);
				result.put(location, cd);
			}

			@Override
			public void visit(SDKClassPath classpath) {
				SDKPathReference sdkpathref = classpath.getSDKPathReference();
				SakerPath path = SDKSupportUtils.getSDKPathReferencePath(sdkpathref, sdks);
				LocalFileLocation fileloc = LocalFileLocation.create(path);
				result.put(fileloc, null);
			}

			private ContentDescriptor handleExecutionFileLocation(SakerPath path, SakerFile cpfile) {
				if (cpfile instanceof SakerDirectory) {
					FileCollectionStrategy classfileadditiondep = RecursiveIgnoreCaseExtensionFileCollectionStrategy
							.create(path, "." + EXTENSION_CLASSFILE);
					NavigableMap<SakerPath, SakerFile> classfiles = taskcontext.getTaskUtilities()
							.collectFilesReportInputFileAndAdditionDependency(tag, classfileadditiondep);
					return new MultiPathContentDescriptor(SakerPathFiles.toFileContentMap(classfiles));
				}
				taskcontext.getTaskUtilities().reportInputFileDependency(tag, cpfile);
				return cpfile.getContentDescriptor();
			}

			private ContentDescriptor handleFileLocation(FileLocation location) {
				ContentDescriptor[] cdres = { null };
				location.accept(new FileLocationVisitor() {
					@Override
					public void visit(ExecutionFileLocation loc) {
						SakerPath path = loc.getPath();
						SakerFile cpfile = taskcontext.getTaskUtilities().resolveAtPath(path);
						if (cpfile == null) {
							throw ObjectUtils
									.sneakyThrow(new FileNotFoundException("Class path file not found: " + path));
						}
						cdres[0] = handleExecutionFileLocation(path, cpfile);
					}

					@Override
					public void visit(LocalFileLocation loc) {
						SakerPath path = loc.getLocalPath();
						TaskExecutionUtilities taskutils = taskcontext.getTaskUtilities();
						ContentDescriptor cd = taskutils.getReportExecutionDependency(
								new LocalPathFileContentDescriptorExecutionProperty(path));
						if (cd == null) {
							throw ObjectUtils
									.sneakyThrow(new FileNotFoundException("Class path local file not found: " + path));
						}

						if (DirectoryContentDescriptor.INSTANCE.equals(cd)) {
							//the class path denotes a directory
							//add the dependencies on the class files

							LocalDirectoryClassFilesExecutionProperty.PropertyValue pval = taskutils
									.getReportExecutionDependency(new LocalDirectoryClassFilesExecutionProperty(path));
							cdres[0] = new MultiPathContentDescriptor(pval.getContents());
						} else {
							cdres[0] = cd;
						}
					}
				});
				return cdres[0];
			}
		});
		return result;
	}

	private static class LocalPathFileContentDescriptorExecutionProperty
			implements ExecutionProperty<ContentDescriptor>, Externalizable {
		//TODO duplicated with JavaTaskUtils
		private static final long serialVersionUID = 1L;

		private SakerPath path;

		/**
		 * For {@link Externalizable}.
		 */
		public LocalPathFileContentDescriptorExecutionProperty() {
		}

		public LocalPathFileContentDescriptorExecutionProperty(SakerPath path) {
			this.path = path;
		}

		@Override
		public ContentDescriptor getCurrentValue(ExecutionContext executioncontext) {
			try {
				ContentDescriptor result = executioncontext
						.getContentDescriptor(LocalFileProvider.getInstance().getPathKey(path));
				return result;
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(path);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			path = (SakerPath) in.readObject();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
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
			LocalPathFileContentDescriptorExecutionProperty other = (LocalPathFileContentDescriptorExecutionProperty) obj;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[path=" + path + "]";
		}

	}

	private static class LocalDirectoryClassFilesExecutionProperty
			implements ExecutionProperty<LocalDirectoryClassFilesExecutionProperty.PropertyValue>, Externalizable {
		//TODO duplicated with JavaTaskUtils
		private static final long serialVersionUID = 1L;

		public static class PropertyValue implements Externalizable {
			private static final long serialVersionUID = 1L;

			//TODO use MultiPathContentDescriptor
			private NavigableMap<SakerPath, ContentDescriptor> contents;

			/**
			 * For {@link Externalizable}.
			 */
			public PropertyValue() {
			}

			public PropertyValue(NavigableMap<SakerPath, ContentDescriptor> contents) {
				this.contents = contents;
			}

			public NavigableMap<SakerPath, ? extends ContentDescriptor> getContents() {
				return contents;
			}

			@Override
			public void writeExternal(ObjectOutput out) throws IOException {
				SerialUtils.writeExternalMap(out, contents);
			}

			@Override
			public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
				contents = SerialUtils.readExternalImmutableNavigableMap(in);
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ((contents == null) ? 0 : contents.hashCode());
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
				PropertyValue other = (PropertyValue) obj;
				if (!ObjectUtils.mapOrderedEquals(this.contents, other.contents)) {
					return false;
				}
				return true;
			}

			@Override
			public String toString() {
				return getClass().getSimpleName() + "[" + (contents != null ? "contents=" + contents : "") + "]";
			}

		}

		private SakerPath path;

		/**
		 * For {@link Externalizable}.
		 */
		public LocalDirectoryClassFilesExecutionProperty() {
		}

		public LocalDirectoryClassFilesExecutionProperty(SakerPath path) {
			this.path = path;
		}

		@Override
		public PropertyValue getCurrentValue(ExecutionContext executioncontext) throws Exception {
			NavigableMap<SakerPath, ContentDescriptor> result = new TreeMap<>();
			for (Entry<SakerPath, ? extends FileEntry> entry : LocalFileProvider.getInstance()
					.getDirectoryEntriesRecursively(path).entrySet()) {
				if (!entry.getValue().isRegularFile()) {
					continue;
				}
				SakerPath keypath = entry.getKey();
				if (!StringUtils.endsWithIgnoreCase(keypath.getFileName(), "." + EXTENSION_CLASSFILE)) {
					//not a class file
					continue;
				}
				SakerPath cpabspath = path.resolve(keypath);
				ContentDescriptor classfilecd = executioncontext.getExecutionPropertyCurrentValue(
						new LocalPathFileContentDescriptorExecutionProperty(cpabspath));
				if (classfilecd == null) {
					continue;
				}
				result.put(cpabspath, classfilecd);
			}
			return new PropertyValue(ImmutableUtils.unmodifiableNavigableMap(result));
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(path);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			path = (SakerPath) in.readObject();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
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
			LocalDirectoryClassFilesExecutionProperty other = (LocalDirectoryClassFilesExecutionProperty) obj;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[" + path + "]";
		}
	}
}
