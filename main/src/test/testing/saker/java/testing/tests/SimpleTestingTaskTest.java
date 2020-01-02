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

import testing.saker.SakerTest;
import testing.saker.java.testing.JavaTestingVariablesMetricEnvironmentTestCase;

@SakerTest
public class SimpleTestingTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {
	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(),
				setOf("test.Main", "test.SecondAnnot", "test.TestAnnot", "test.Main$Sub"));
		assertEquals(getMetric().getSuccessfulTests(),
				setOf("test.Main", "test.SecondAnnot", "test.TestAnnot", "test.Main$Sub"));

		files.putFile(SRC_PATH_BASE.resolve("test/NewClass.java"), "package test; public class NewClass { }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.NewClass"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.NewClass"));

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());

		files.putFile(SRC_PATH_BASE.resolve("test/NewClass.java"),
				"package test; @TestAnnot public class NewClass { }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.NewClass"));
		assertEquals(getMetric().getFailedTests(), setOf("test.NewClass"));

//		runScriptTask("build");
//		assertEquals(getMetric().getInvokedTests(), setOf());
//		assertEquals(getMetric().getFailedTests(), setOf("test.NewClass"));

		files.putFile(SRC_PATH_BASE.resolve("test/NewClass.java"),
				"package test; @TestAnnot @SecondAnnot public class NewClass { }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.NewClass"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.NewClass"));

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());

		//method dependency
		files.putFile(SRC_PATH_BASE.resolve("test/Consumer.java"),
				"package test; public class Consumer { public static void main(String[] args) { Dependent.function(); } }");
		files.putFile(SRC_PATH_BASE.resolve("test/Dependent.java"),
				"package test; public class Dependent { public static void function() { } }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Consumer", "test.Dependent"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Consumer", "test.Dependent"));

		files.putFile(SRC_PATH_BASE.resolve("test/Dependent.java"),
				"package test; public class Dependent { int x; public static void function() { } }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Consumer", "test.Dependent"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Consumer", "test.Dependent"));

		files.putFile(SRC_PATH_BASE.resolve("test/Dependent.java"),
				"package test; @TestAnnot public class Dependent { int x; public static void function() { } }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Consumer", "test.Dependent"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Consumer"));
		assertEquals(getMetric().getFailedTests(), setOf("test.Dependent"));

//		runScriptTask("build");
//		assertEquals(getMetric().getInvokedTests(), setOf());
//		assertEquals(getMetric().getFailedTests(), setOf("test.Dependent"));

		files.putFile(SRC_PATH_BASE.resolve("test/Dependent.java"),
				"package test; @TestAnnot @SecondAnnot public class Dependent { int x; public static void function() { } }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Consumer", "test.Dependent"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Consumer", "test.Dependent"));

		//static field dependency
		//do not set an initial value to static field
		//else <clinit> will be invoked which triggers a dependency
		//    non-static field dependency is not checked, as it is very hard to construct an instance of something and accessing its field without invoking the constructor
		//    invoking the constructor would add a dependency via that method call
		//    we trust the logger bytecode manipulator, as it checks all field manipulating bytecode instructions
		files.putFile(SRC_PATH_BASE.resolve("test/StaticFieldConsumer.java"),
				"package test; public class StaticFieldConsumer { public static void main(String[] args) { StaticFieldDependent.staticfield = 0; } }");
		files.putFile(SRC_PATH_BASE.resolve("test/StaticFieldDependent.java"),
				"package test; public class StaticFieldDependent { public static int staticfield; public static void func() { } }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.StaticFieldConsumer", "test.StaticFieldDependent"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.StaticFieldConsumer", "test.StaticFieldDependent"));

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());

		files.putFile(SRC_PATH_BASE.resolve("test/StaticFieldDependent.java"),
				"package test; public class StaticFieldDependent { public static int staticfield; public static void func() { int x; } }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.StaticFieldConsumer", "test.StaticFieldDependent"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.StaticFieldConsumer", "test.StaticFieldDependent"));

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());

		files.putFile(SRC_PATH_BASE.resolve("test/LambdaField.java"),
				"package test; public class LambdaField { public static Runnable r = () -> { StaticFieldDependent.staticfield = 0;  }; }");
		files.putFile(SRC_PATH_BASE.resolve("test/LambdaUser.java"),
				"package test; public class LambdaUser { public static void main(String[] args) { LambdaField.r.run(); } }");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.LambdaField", "test.LambdaUser"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.LambdaField", "test.LambdaUser"));
		assertTrue(getMetric().getTestDependentClasses("test.LambdaUser")
				.containsAll(setOf("test.LambdaUser", "test.LambdaField", "test.StaticFieldDependent")));
	}

}
