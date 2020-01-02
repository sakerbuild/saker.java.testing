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
public class ClassLoaderResourceTestingTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {

	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		files.createDirectories(PATH_WORKING_DIRECTORY.resolve("resources"));

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/stream/resource"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/url/resource"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/url/resources"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/abs/resource"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/abs/url/resource"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/test/class/resource"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("resources/test/class/url/resource"), "modified");
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
		assertEquals(getMetric().getFailedTests(), setOf());

	}

}
