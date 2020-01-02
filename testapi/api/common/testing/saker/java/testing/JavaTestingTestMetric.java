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
