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
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

import saker.build.file.path.SakerPath;
import saker.build.runtime.environment.EnvironmentProperty;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.Task;
import saker.build.task.TaskContext;
import saker.build.task.TaskFactory;
import saker.build.task.identifier.TaskIdentifier;
import saker.java.compiler.api.classpath.JavaClassPath;
import saker.java.compiler.api.compile.SakerJavaCompilerUtils;
import saker.java.testing.api.test.JavaTestingOutput;
import saker.java.testing.api.test.exc.JavaTestingFailedException;
import saker.java.testing.api.test.invoker.TestInvokerParameters;
import saker.java.testing.main.test.JavaTesterTaskFactory;
import saker.sdk.support.api.EnvironmentSDKDescription;
import saker.sdk.support.api.ResolvedSDKDescription;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKDescriptionVisitor;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.api.UserSDKDescription;
import saker.std.api.environment.qualifier.AnyEnvironmentQualifier;
import saker.std.api.environment.qualifier.EnvironmentQualifier;
import saker.std.api.environment.qualifier.EnvironmentQualifierVisitor;
import saker.std.api.environment.qualifier.PropertyEnvironmentQualifier;
import testing.saker.java.testing.TestFlag;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.StringUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;

public class TestWorkerTaskFactory
		implements TaskFactory<JavaTestingOutput>, Task<JavaTestingOutput>, Externalizable, TaskIdentifier {
	private static final long serialVersionUID = 1L;

	private static final SDKDescription DEFAULT_JAVA_SDK = SakerJavaCompilerUtils.getDefaultJavaSDK();

	private JavaClassPath testRunnerClassPath = null;

	private JavaClassPath classPath = null;
	private JavaClassPath testClassPath = null;
	private JavaClassPath dependencyClassPath = null;
	private NavigableSet<String> testClasses = null;
	private NavigableSet<String> nonDeterministicTests = null;
	private Map<String, Collection<String>> additionalTestClassDependencies = Collections.emptyNavigableMap();

	private String testInvokerClass = null;
	private Map<String, String> testInvokerParameters = Collections.emptyNavigableMap();
	private Map<String, Map<String, String>> testClassParameters = Collections.emptyNavigableMap();

	private Collection<Integer> successExitCodes = Collections.singleton(0);

	private Collection<SakerPath> ignoreFileChanges = Collections.emptySet();

	private SakerPath workingDirectory;

	private List<String> processJVMArguments = Collections.emptyList();

	private transient int maxJVMCount = 1;
	private transient boolean failFast = false;
	private transient boolean abortOnFail = true;
	private transient boolean verbose = TestFlag.ENABLED;

	private SDKDescription javaSDK = DEFAULT_JAVA_SDK;

	public TestWorkerTaskFactory() {
	}

	public void setTestRunnerClassPath(JavaClassPath testRunnerClassPath) {
		this.testRunnerClassPath = testRunnerClassPath;
	}

	public void setClassPath(JavaClassPath classPath) {
		this.classPath = classPath;
	}

	public void setTestClassPath(JavaClassPath testClassPath) {
		this.testClassPath = testClassPath;
	}

	public void setDependencyClassPath(JavaClassPath dependencyClassPath) {
		this.dependencyClassPath = dependencyClassPath;
	}

	public void setTestClasses(NavigableSet<String> testClasses) {
		this.testClasses = testClasses;
	}

	public void setNonDeterministicTests(NavigableSet<String> nonDeterministicTests) {
		this.nonDeterministicTests = nonDeterministicTests;
	}

	public void setAdditionalTestClassDependencies(Map<String, Collection<String>> additionalTestClassDependencies) {
		this.additionalTestClassDependencies = additionalTestClassDependencies;
	}

	public void setTestInvokerClass(String testInvokerClass) {
		this.testInvokerClass = testInvokerClass;
	}

	public void setTestInvokerParameters(Map<String, String> testInvokerParameters) {
		this.testInvokerParameters = testInvokerParameters;
	}

	public void setTestClassParameters(Map<String, Map<String, String>> testClassParameters) {
		this.testClassParameters = testClassParameters;
	}

	public void setSuccessExitCodes(Collection<Integer> successExitCodes) {
		this.successExitCodes = successExitCodes;
	}

	public void setIgnoreFileChanges(Collection<SakerPath> ignoreFileChanges) {
		if (ignoreFileChanges == null) {
			this.ignoreFileChanges = Collections.emptySet();
		} else {
			this.ignoreFileChanges = ignoreFileChanges;
		}
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

	public void setAbortOnFail(boolean abortOnFail) {
		this.abortOnFail = abortOnFail;
	}

	public void setMaxJVMCount(int maxJVMCount) {
		this.maxJVMCount = maxJVMCount;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setWorkingDirectory(SakerPath workingDirectory) {
		if (SakerPath.EMPTY.equals(workingDirectory)) {
			this.workingDirectory = null;
		} else {
			this.workingDirectory = workingDirectory;
		}
	}

	public void setProcessJVMArguments(List<String> processjvmargs) {
		if (processjvmargs == null) {
			processjvmargs = Collections.emptyList();
		}
		this.processJVMArguments = processjvmargs;
	}

	public void setJavaSDK(SDKDescription javaSDK) {
		if (javaSDK == null) {
			this.javaSDK = DEFAULT_JAVA_SDK;
		} else {
			this.javaSDK = javaSDK;
		}
	}

	@Override
	public NavigableSet<String> getCapabilities() {
		return ImmutableUtils.makeImmutableNavigableSet(new String[] { CAPABILITY_INNER_TASKS_COMPUTATIONAL });
	}

	@Override
	public Task<? extends JavaTestingOutput> createTask(ExecutionContext executioncontext) {
		return this;
	}

	protected static SDKReference toSDKReference(TaskContext taskcontext, SDKDescription sdkdescription) {
		SDKReference[] result = { null };
		sdkdescription.accept(new SDKDescriptionVisitor() {
			@Override
			public void visit(EnvironmentSDKDescription description) {
				EnvironmentProperty<? extends SDKReference> envproperty = SDKSupportUtils
						.getEnvironmentSDKDescriptionReferenceEnvironmentProperty(description);
				result[0] = taskcontext.getTaskUtilities().getReportEnvironmentDependency(envproperty);
			}

			@Override
			public void visit(ResolvedSDKDescription description) {
				result[0] = description.getSDKReference();
			}

			@Override
			public void visit(UserSDKDescription description) {
				EnvironmentQualifier qualifier = description.getQualifier();
				if (qualifier != null) {
					qualifier.accept(new EnvironmentQualifierVisitor() {
						@Override
						public void visit(PropertyEnvironmentQualifier qualifier) {
							EnvironmentProperty<?> envproperty = qualifier.getEnvironmentProperty();
							Object currentval = taskcontext.getTaskUtilities()
									.getReportEnvironmentDependency(envproperty);
							Object expectedvalue = qualifier.getExpectedValue();
							if (!Objects.equals(currentval, expectedvalue)) {
								throw new IllegalArgumentException(
										"Unsuitable environment, user SDK qualifier mismatch: " + currentval + " - "
												+ expectedvalue + " for property: " + envproperty);
							}
						}

						@Override
						public void visit(AnyEnvironmentQualifier qualifier) {
						}
					});
				}
				result[0] = UserSDKDescription.createSDKReference(description.getPaths(), description.getProperties());
			}
		});
		return result[0];
	}

	@Override
	public JavaTestingOutput run(TaskContext taskcontext) throws Exception {
		taskcontext.setStandardOutDisplayIdentifier(JavaTesterTaskFactory.TASK_NAME);
		IncrementalTestingInfo previnfo = taskcontext.getPreviousTaskOutput(IncrementalTestingInfo.class,
				IncrementalTestingInfo.class);

		ExecutionContext context = taskcontext.getExecutionContext();
		SakerEnvironment environment = context.getEnvironment();
		Path sakerjar = environment.getEnvironmentJarPath();

		if (sakerjar == null) {
			throw new IOException("Failed to run tests, Saker JAR path is not available");
		}

		SDKReference javasdkref = toSDKReference(taskcontext, javaSDK);

		NavigableSet<SakerPath> igenorefilechanges = new TreeSet<>();
		if (!ignoreFileChanges.isEmpty()) {
			//TODO resolve the working directory parameter as well
			SakerPath workingdirpath = taskcontext.getTaskWorkingDirectory().getSakerPath();
			for (SakerPath ignpath : ignoreFileChanges) {
				igenorefilechanges.add(workingdirpath.tryResolve(ignpath));
			}
		}

		IncrementalTestingHandler testhandler = new IncrementalTestingHandler(taskcontext, context, sakerjar,
				javasdkref, testRunnerClassPath, classPath, testClassPath,
				new TestInvokerParameters(testInvokerParameters), testClasses, previnfo, igenorefilechanges);
		testhandler.setFailFast(failFast);
		testhandler.setSuccessExitCodes(successExitCodes);
		testhandler.setMaxJVMCount(maxJVMCount);
		testhandler.setDependencyClassPaths(dependencyClassPath);
		testhandler.setNonDeterministicTests(nonDeterministicTests);
		testhandler.setAdditionalTestClassDependencies(additionalTestClassDependencies);
		testhandler.setTestClassParameters(testClassParameters);
		testhandler.setVerbose(verbose);
		testhandler.setProcessJVMArguments(processJVMArguments);
		if (testInvokerClass != null) {
			testhandler.setTestInvokerClassName(testInvokerClass);
		}
		testhandler.setWorkingDirectory(workingDirectory);

		IncrementalTestingInfo info = testhandler.test();

		taskcontext.setTaskOutput(IncrementalTestingInfo.class, info);

		if (abortOnFail && info.isAnyTestFailed()) {
			JavaTestingFailedException exc = new JavaTestingFailedException("Testing was unsuccessful. ("
					+ StringUtils.toStringJoin(", ", info.getUnsuccessfulTestClassNames()) + ")");
			for (Throwable e : testhandler.getExceptions()) {
				exc.addSuppressed(e);
			}
			taskcontext.abortExecution(exc);
			return null;
		}

		InternalJavaTestingOutput result = new InternalJavaTestingOutput();
		return result;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(testRunnerClassPath);
		out.writeObject(classPath);
		out.writeObject(testClassPath);
		out.writeObject(dependencyClassPath);
		out.writeObject(javaSDK);

		SerialUtils.writeExternalCollection(out, testClasses);
		SerialUtils.writeExternalCollection(out, nonDeterministicTests);
		SerialUtils.writeExternalMap(out, additionalTestClassDependencies, SerialUtils::writeExternalObject,
				SerialUtils::writeExternalCollection);

		out.writeObject(testInvokerClass);
		SerialUtils.writeExternalMap(out, testInvokerParameters);
		SerialUtils.writeExternalMap(out, testClassParameters, SerialUtils::writeExternalObject,
				SerialUtils::writeExternalMap);

		SerialUtils.writeExternalCollection(out, successExitCodes);
		SerialUtils.writeExternalCollection(out, ignoreFileChanges);

		out.writeBoolean(failFast);
		out.writeBoolean(abortOnFail);
		out.writeInt(maxJVMCount);
		out.writeBoolean(verbose);

		out.writeObject(workingDirectory);

		SerialUtils.writeExternalCollection(out, this.processJVMArguments);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		testRunnerClassPath = (JavaClassPath) in.readObject();
		classPath = (JavaClassPath) in.readObject();
		testClassPath = (JavaClassPath) in.readObject();
		dependencyClassPath = (JavaClassPath) in.readObject();
		javaSDK = (SDKDescription) in.readObject();

		testClasses = SerialUtils.readExternalImmutableNavigableSet(in);
		nonDeterministicTests = SerialUtils.readExternalImmutableNavigableSet(in);
		additionalTestClassDependencies = SerialUtils.readExternalMap(new TreeMap<>(), in,
				SerialUtils::readExternalObject, SerialUtils::readExternalImmutableNavigableSet);

		testInvokerClass = (String) in.readObject();
		testInvokerParameters = SerialUtils.readExternalImmutableLinkedHashMap(in);
		testClassParameters = SerialUtils.readExternalMap(new TreeMap<>(), in, SerialUtils::readExternalObject,
				SerialUtils::readExternalImmutableLinkedHashMap);

		successExitCodes = SerialUtils.readExternalImmutableNavigableSet(in);
		ignoreFileChanges = SerialUtils.readExternalImmutableNavigableSet(in);

		failFast = in.readBoolean();
		abortOnFail = in.readBoolean();
		maxJVMCount = in.readInt();
		verbose = in.readBoolean();

		workingDirectory = (SakerPath) in.readObject();

		processJVMArguments = SerialUtils.readExternalImmutableList(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((additionalTestClassDependencies == null) ? 0 : additionalTestClassDependencies.hashCode());
		result = prime * result + ((classPath == null) ? 0 : classPath.hashCode());
		result = prime * result + ((dependencyClassPath == null) ? 0 : dependencyClassPath.hashCode());
		result = prime * result + ((ignoreFileChanges == null) ? 0 : ignoreFileChanges.hashCode());
		result = prime * result + ((javaSDK == null) ? 0 : javaSDK.hashCode());
		result = prime * result + ((nonDeterministicTests == null) ? 0 : nonDeterministicTests.hashCode());
		result = prime * result + ((processJVMArguments == null) ? 0 : processJVMArguments.hashCode());
		result = prime * result + ((successExitCodes == null) ? 0 : successExitCodes.hashCode());
		result = prime * result + ((testClassParameters == null) ? 0 : testClassParameters.hashCode());
		result = prime * result + ((testClassPath == null) ? 0 : testClassPath.hashCode());
		result = prime * result + ((testClasses == null) ? 0 : testClasses.hashCode());
		result = prime * result + ((testInvokerClass == null) ? 0 : testInvokerClass.hashCode());
		result = prime * result + ((testInvokerParameters == null) ? 0 : testInvokerParameters.hashCode());
		result = prime * result + ((testRunnerClassPath == null) ? 0 : testRunnerClassPath.hashCode());
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
		TestWorkerTaskFactory other = (TestWorkerTaskFactory) obj;
		if (additionalTestClassDependencies == null) {
			if (other.additionalTestClassDependencies != null)
				return false;
		} else if (!additionalTestClassDependencies.equals(other.additionalTestClassDependencies))
			return false;
		if (classPath == null) {
			if (other.classPath != null)
				return false;
		} else if (!classPath.equals(other.classPath))
			return false;
		if (dependencyClassPath == null) {
			if (other.dependencyClassPath != null)
				return false;
		} else if (!dependencyClassPath.equals(other.dependencyClassPath))
			return false;
		if (ignoreFileChanges == null) {
			if (other.ignoreFileChanges != null)
				return false;
		} else if (!ignoreFileChanges.equals(other.ignoreFileChanges))
			return false;
		if (javaSDK == null) {
			if (other.javaSDK != null)
				return false;
		} else if (!javaSDK.equals(other.javaSDK))
			return false;
		if (nonDeterministicTests == null) {
			if (other.nonDeterministicTests != null)
				return false;
		} else if (!nonDeterministicTests.equals(other.nonDeterministicTests))
			return false;
		if (processJVMArguments == null) {
			if (other.processJVMArguments != null)
				return false;
		} else if (!processJVMArguments.equals(other.processJVMArguments))
			return false;
		if (successExitCodes == null) {
			if (other.successExitCodes != null)
				return false;
		} else if (!successExitCodes.equals(other.successExitCodes))
			return false;
		if (testClassParameters == null) {
			if (other.testClassParameters != null)
				return false;
		} else if (!testClassParameters.equals(other.testClassParameters))
			return false;
		if (testClassPath == null) {
			if (other.testClassPath != null)
				return false;
		} else if (!testClassPath.equals(other.testClassPath))
			return false;
		if (testClasses == null) {
			if (other.testClasses != null)
				return false;
		} else if (!testClasses.equals(other.testClasses))
			return false;
		if (testInvokerClass == null) {
			if (other.testInvokerClass != null)
				return false;
		} else if (!testInvokerClass.equals(other.testInvokerClass))
			return false;
		if (testInvokerParameters == null) {
			if (other.testInvokerParameters != null)
				return false;
		} else if (!testInvokerParameters.equals(other.testInvokerParameters))
			return false;
		if (testRunnerClassPath == null) {
			if (other.testRunnerClassPath != null)
				return false;
		} else if (!testRunnerClassPath.equals(other.testRunnerClassPath))
			return false;
		if (workingDirectory == null) {
			if (other.workingDirectory != null)
				return false;
		} else if (!workingDirectory.equals(other.workingDirectory))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TestWorkerTaskFactory["
				+ (testRunnerClassPath != null ? "testRunnerClassPath=" + testRunnerClassPath + ", " : "")
				+ (classPath != null ? "classPath=" + classPath + ", " : "")
				+ (testClassPath != null ? "testClassPath=" + testClassPath + ", " : "")
				+ (dependencyClassPath != null ? "dependencyClassPath=" + dependencyClassPath + ", " : "")
				+ (testClasses != null ? "testClasses=" + testClasses + ", " : "")
				+ (nonDeterministicTests != null ? "nonDeterministicTests=" + nonDeterministicTests + ", " : "")
				+ (additionalTestClassDependencies != null
						? "additionalTestClassDependencies=" + additionalTestClassDependencies + ", "
						: "")
				+ (testInvokerClass != null ? "testInvokerClass=" + testInvokerClass + ", " : "")
				+ (testInvokerParameters != null ? "testInvokerParameters=" + testInvokerParameters + ", " : "")
				+ (testClassParameters != null ? "testClassParameters=" + testClassParameters + ", " : "")
				+ (successExitCodes != null ? "successExitCodes=" + successExitCodes + ", " : "")
				+ (ignoreFileChanges != null ? "ignoreFileChanges=" + ignoreFileChanges + ", " : "") + "failFast="
				+ failFast + ", abortOnFail=" + abortOnFail + ", maxJVMCount=" + maxJVMCount + ", "
				+ (workingDirectory != null ? "workingDirectory=" + workingDirectory : "") + "]";
	}

}
