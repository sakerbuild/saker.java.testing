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
public class JigsawIllegalAccessTaskTest extends JavaTestingVariablesMetricEnvironmentTestCase {
	@Override
	protected void runNestTaskTestImpl() throws Throwable {
		//just run a simple test that illegally accesses some stuff in java.io
		runScriptTask("build");
		assertEquals(getMetric().getSuccessfulTests(), setOf("test.Main"));
	}

}
