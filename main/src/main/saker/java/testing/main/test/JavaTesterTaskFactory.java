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
package saker.java.testing.main.test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import saker.build.file.path.SakerPath;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.utils.SimpleStructuredObjectTaskResult;
import saker.build.task.utils.annot.SakerInput;
import saker.java.compiler.main.JavaTaskOptionUtils;
import saker.java.compiler.main.compile.option.JavaClassPathTaskOption;
import saker.java.testing.impl.test.TestWorkerTaskFactory;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestParameterInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.nest.utils.FrontendTaskFactory;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;
import testing.saker.java.testing.TestFlag;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.function.Functionals;

@NestInformation("Executes tests for Java classes.\n"
		+ "The task will execute the specified tests in a separate Java Virtual Machine using the given test runner.\n"
		+ "The task provides incremental support for testing, meaning that only tests which have their inputs change will "
		+ "be reinvoked next time. The incremental support includes modifying Java classes, accessed files, and class loader resources. "
		+ "Some aspects of incremental support may not be complete, e.g. reflection based Java class tracking.\n"
		+ "This task is best suited for unit testing of Java classes, and assumes that test cases don't share a common state.\n"
		+ "The task takes 3 class path inputs that are loaded for testing:\n"
		+ "1. The user ClassPath. This contains the classes that are being tested.\n"
		+ "2. The TestClassPath. This contains the test cases that are being run.\n"
		+ "3. The TestRunnerClassPath. This contains the classes that are used to invoke the tests in TestClassPath.\n"
		+ "The class paths are loaded in a way that the TestClassPath sees classes from both the user ClassPath and TestRunnerClassPath,"
		+ "while the user ClassPath is loaded in an isolated way.\n"
		+ "The invoker API of the saker.java.testing bundle is available for the TestRunnerClassPath.")
@NestParameterInformation(value = "TestRunnerClassPath",
		type = @NestTypeUsage(value = Collection.class, elementTypes = JavaClassPathTaskOption.class),
		info = @NestInformation("Specifies the class path which contains the classes that are used to run the test cases.\n"
				+ "This class path contains the classes that invoke the tests. The specified TestInvokerClass will be loaded from it, "
				+ "and it is used to call the test cases. This class path usually contains the testing library like JUnit or similar libraries."))
@NestParameterInformation(value = "TestClassPath",
		type = @NestTypeUsage(value = Collection.class, elementTypes = JavaClassPathTaskOption.class),
		info = @NestInformation("Specifies the class path which contains the test case classes.\n"
				+ "Each class that matches the specified TestClasses will be passed to the test invoker to execute the test.\n"
				+ "The test classes are passed to the invoker one-by-one, instead of in bulk. The test invoker class specified in "
				+ "TestInvokerClass will be used to invoke the test cases."))
@NestParameterInformation(value = "ClassPath",
		required = true,
		type = @NestTypeUsage(value = Collection.class, elementTypes = JavaClassPathTaskOption.class),
		info = @NestInformation("Specifies the class path of the classes being tested.\n"
				+ "These are the classes which are accessible from the TestClassPath and are the subject of testing."))
//TODO DependencyClassPath
@NestParameterInformation(value = "TestClasses",
		type = @NestTypeUsage(value = Collection.class, elementTypes = String.class),
		info = @NestInformation("Wildcard specifications of the Java test case classes.\n"
				+ "The parameter accepts wildcard patterns matching Java class binary names. The patterns are to be specified "
				+ "in a dot ('.') separated format, and they work the same way as wildcard paths.\n"
				+ "E.g. to specify all classes that end with \"Test\" to be used as test cases: \"**.*Test\"\n"
				+ "All matching classes will be passed to the TestInvokerClass for testing."))
@NestParameterInformation(value = "NonDeterministicTests",
		type = @NestTypeUsage(value = Collection.class, elementTypes = String.class),
		info = @NestInformation("Wildcard specifications of the non-deterministic Java test case classes.\n"
				+ "If a non-deterministic test case fails, it will be reinvoked next time the task is run. By default, "
				+ "all test cases are considered to be deterministic. Meaning that if they fail, they won't be re-run if "
				+ "none of its dependencies have changed. Specifying a test case to be non-deterministic will cause the task "
				+ "to re-run tests even if their dependencies haven't changed.\n"
				+ "If you encounter any incremental errors or unexpected outputs for failed test-cases, specifying \"**\" "
				+ "will result in all failed tests to be reinvoked."))

@NestParameterInformation(value = "TestInvokerClass",
		type = @NestTypeUsage(String.class),
		info = @NestInformation("Specifies the Java class name of the test invoker that is used to run the tests.\n"
				+ "The test invoker is loaded from the TestRunnerClassPath, and must implement saker.java.testing.api.test.invoker.JavaTestInvoker. "
				+ "The specified test cases will be passed to the test invoker to be run.\n"
				+ "The test invoker can be configured using TestInvokerParameters.\n"
				+ "By default, a test invoker is used that calls a main method of the specified TestRunnerClass test invoker parameter "
				+ "with the test case name passed as an argument. See TestInvokerParameters and TestClassParameters for the "
				+ "available parameters for the default test invoker."))

@NestParameterInformation(value = "TestInvokerParameters",
		type = @NestTypeUsage(value = Map.class, elementTypes = { String.class, String.class }),
		info = @NestInformation("Arbitrary key-value pairs that are passed to the TestInvokerClass during initialization.\n"
				+ "The specified entries can be used to configure the test invoker. These parameters are passed to the invoker "
				+ "at the start of testing, and don't change for each test case.\n"
				+ "If the default TestInvokerClass is used, the following parameter(s) can be used: \n"
				+ "TestRunnerClass: Specifies the class that runs the test. It must have a static main(String[]) method which is "
				+ "called for each test case."))
@NestParameterInformation(value = "TestClassParameters",
		type = @NestTypeUsage(value = Map.class, elementTypes = { String.class, Map.class }),
		info = @NestInformation("Specifies arbitrary key-value pairs that are passed to the TestInvokerClass for each test case.\n"
				+ "Each entry in the specified map is a class name wildcard mapped to the string key-value pairs that are passed "
				+ "for any test case which class name matches the associated wildcard.\n"
				+ "The parameters for the test cases can be used to configure the behaviour of the test runner for the invoked "
				+ "test case.\n"
				+ "If the default TestInvokerClass is used, the following test case parameter(s) can be used: \n"
				+ "PrefixArguments: Specifies parameters for the test runner class that are inserted before the test class name.\n"
				+ "SuffixArguments: Specifies parameters for the test runner class that are inserted after the test class name.\n"
				+ "To summarize, for each test case class name, the TestRunnerClass.main(String[]) method is called with the arguments "
				+ "constructed as { <PrefixArguments...>, <test-case-class-name>, <SuffixArguments...> }."))

@NestParameterInformation(value = "SuccessExitCodes",
		type = @NestTypeUsage(value = Collection.class, elementTypes = int.class),
		info = @NestInformation("Specifies the exit codes that should be considered as a result of a successful test case.\n"
				+ "If a test runner decides to call System.exit() as the result of the test execution, it will be considered "
				+ "as a failed test. However, if the exit code is one of the values specified in this parameter, the test "
				+ "case will be considered as successful.\n"
				+ "By default, the exit code of 0 is considered to be a successful test."))

@NestParameterInformation(value = "ProcessJVMParameters",
		type = @NestTypeUsage(value = Collection.class, elementTypes = String.class),
		info = @NestInformation("Additional command line parameters to be passed to the spawned JVM that is used to run the test cases.\n"
				+ "The specified parameters are directly appended to the started Java process right after the executable argument, and "
				+ "before any task related arguments.\n"
				+ "This parameter is useful for example to start a test process with debugging enabled. Using "
				+ "\"-agentlib:jdwp=transport=dt_socket,address=localhost:5432,server=y,suspend=y,quiet=y\" will start the process "
				+ "by listening on the 5432 port for a debugger after being started. We recommend connecting to the test process "
				+ "using your Java IDE of choice. When you specify this argument make sure to add \"quite=y\", else the "
				+ "task may not initialize the process successfully."))

@NestParameterInformation(value = "FailFast",
		type = @NestTypeUsage(boolean.class),
		info = @NestInformation("Specifies if the testing should continue after a failed test has been encountered.\n"
				+ "If set to true, the testing will end if a failed task is encountered. If set to false, all tests will be run.\n"
				+ "The default is false."))
@NestParameterInformation(value = "AbortOnFail",
		type = @NestTypeUsage(boolean.class),
		info = @NestInformation("Specifies if the task should be considered as failed if a failed test is encountered.\n"
				+ "If set to true, then the task will report an error, and will cause the build execution to fail if a test case fails.\n"
				+ "If set to false, then a failed test will not cause the build execution to fail.\n"
				+ "The default is true."))

@NestParameterInformation(value = "JavaSDK",
		type = @NestTypeUsage(SDKDescriptionTaskOption.class),
		info = @NestInformation("Specifies the Java installation that should be used to execute the tests.\n"
				+ "If not specified, the same installed Java Runtime Environment is used as the one used to run the build."))

@NestParameterInformation(value = "Verbose",
		type = @NestTypeUsage(boolean.class),
		info = @NestInformation("Sets the output of the testing task to be verbose.\n"
				+ "Verbose output includes more detailed information about the test cases that are being executed."))

@NestParameterInformation(value = "WorkingDirectory",
		type = @NestTypeUsage(SakerPath.class),
		info = @NestInformation("Specifies the working directory that the tests should use.\n"
				+ "If tests use relative paths, they will be resolved against the specified working directory. The path is "
				+ "an execution path, not a path on the local file system."))

@NestParameterInformation(value = "MaxJVMCount",
		type = @NestTypeUsage(int.class),
		info = @NestInformation("Specifies how many JVM processes may be spawned at maximum to run the tests."))

public class JavaTesterTaskFactory extends FrontendTaskFactory<Object> {
	private static final long serialVersionUID = 1L;

	public static final String TASK_NAME = "saker.java.test";

	@Override
	public ParameterizableTask<? extends Object> createTask(ExecutionContext executioncontext) {
		return new JavaTesterTask();
	}

	public static class JavaTesterTask implements ParameterizableTask<Object> {
		@SakerInput("TestRunnerClassPath")
		public Collection<JavaClassPathTaskOption> testRunnerClassPathOption = Collections.emptySet();

		@SakerInput("ClassPath")
		public Collection<JavaClassPathTaskOption> classPathOption = Collections.emptySet();
		@SakerInput(value = "TestClassPath", required = true)
		public Collection<JavaClassPathTaskOption> testClassPathOption = Collections.emptySet();
		//TODO create test for DependencyClassPath
		@SakerInput("DependencyClassPath")
		public Collection<JavaClassPathTaskOption> dependencyClassPathOption = Collections.emptySet();
		@SakerInput("TestClasses")
		public Collection<String> testClassesOption = null;
		@SakerInput("NonDeterministicTests")
		public Collection<String> nonDeterministicTestsOption = null;
		@SakerInput("AdditionalTestClassDependencies")
		public Map<String, Collection<String>> additionalTestClassDependenciesOption = Collections.emptyNavigableMap();

		@SakerInput("TestInvokerClass")
		public String testInvokerClassOption = null;
		@SakerInput("TestInvokerParameters")
		public Map<String, String> testInvokerParametersOption = Collections.emptyNavigableMap();
		@SakerInput("TestClassParameters")
		public Map<String, Map<String, String>> testClassParametersOption = Collections.emptyNavigableMap();

		@SakerInput("SuccessExitCodes")
		public Collection<Integer> successExitCodesOption = Collections.singleton(0);

		@SakerInput("IgnoreFileChanges")
		public Collection<SakerPath> ignoreFileChangesOption = Collections.emptySet();

		@SakerInput("ProcessJVMParameters")
		public List<String> processJVMParametersOption;

		@SakerInput("FailFast")
		public boolean failFastOption = false;
		@SakerInput("AbortOnFail")
		public boolean abortOnFailOption = true;
//TODO some parameters are still undocumented
		@SakerInput("MaxJVMCount")
		public int maxJVMCountOption = 1;

		@SakerInput("Verbose")
		public boolean verbose = TestFlag.ENABLED;

		@SakerInput("JavaSDK")
		public SDKDescriptionTaskOption javaSDKOption;

		@SakerInput("WorkingDirectory")
		public SakerPath workingDirectory;

		@Override
		public Object run(TaskContext taskcontext) throws Exception {
			SDKDescription[] javasdk = { null };
			if (javaSDKOption != null) {
				javaSDKOption.clone().accept(new SDKDescriptionTaskOption.Visitor() {
					@Override
					public void visit(SDKDescription description) {
						javasdk[0] = description;
					}
				});
			}
			TestWorkerTaskFactory workerfactory = new TestWorkerTaskFactory();
			workerfactory.setTestRunnerClassPath(
					JavaTaskOptionUtils.createClassPath(taskcontext, testRunnerClassPathOption));
			workerfactory.setClassPath(JavaTaskOptionUtils.createClassPath(taskcontext, classPathOption));
			workerfactory.setTestClassPath(JavaTaskOptionUtils.createClassPath(taskcontext, testClassPathOption));
			workerfactory.setDependencyClassPath(
					JavaTaskOptionUtils.createClassPath(taskcontext, dependencyClassPathOption));
			workerfactory.setTestClasses(ObjectUtils.cloneTreeSet(testClassesOption));
			workerfactory.setNonDeterministicTests(ObjectUtils.cloneTreeSet(nonDeterministicTestsOption));
			workerfactory.setAdditionalTestClassDependencies(ObjectUtils.cloneTreeMap(
					additionalTestClassDependenciesOption, Functionals.identityFunction(), ObjectUtils::cloneTreeSet));
			workerfactory.setTestInvokerClass(testInvokerClassOption);
			workerfactory
					.setTestInvokerParameters(ImmutableUtils.makeImmutableLinkedHashMap(testInvokerParametersOption));
			workerfactory.setTestClassParameters(ObjectUtils.cloneTreeMap(testClassParametersOption,
					Functionals.identityFunction(), ImmutableUtils::makeImmutableLinkedHashMap));
			workerfactory.setSuccessExitCodes(ObjectUtils.cloneTreeSet(successExitCodesOption));
			workerfactory.setIgnoreFileChanges(ObjectUtils.cloneTreeSet(ignoreFileChangesOption));
			workerfactory.setFailFast(failFastOption);
			workerfactory.setAbortOnFail(abortOnFailOption);
			workerfactory.setMaxJVMCount(maxJVMCountOption);
			workerfactory.setVerbose(verbose);
			workerfactory.setJavaSDK(javasdk[0]);
			workerfactory.setWorkingDirectory(workingDirectory);
			workerfactory.setProcessJVMArguments(ObjectUtils.cloneArrayList(processJVMParametersOption));

			taskcontext.startTask(workerfactory, workerfactory, null);
			return new SimpleStructuredObjectTaskResult(workerfactory);
		}
	}
}
