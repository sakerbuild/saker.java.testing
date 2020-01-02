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
public class ClassPathTestingTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {

	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.Main", "test.CPMainConsumer", "test.Main$Sub"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main", "test.CPMainConsumer", "test.Main$Sub"));

		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf());
		assertEquals(getMetric().getSuccessfulTests(), setOf());

		files.putFile(SRC_PATH_BASE.resolve("test/ClassPathMain.java"), files
				.getAllBytes(SRC_PATH_BASE.resolve("test/ClassPathMain.java")).toString().replace("@SecondAnnot", ""));
		runScriptTask("build");
		assertEquals(getMetric().getInvokedTests(), setOf("test.CPMainConsumer"));
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.CPMainConsumer"));
	}

}
