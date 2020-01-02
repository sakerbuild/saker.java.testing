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

public class TestFlag {
	private static final JavaTestingTestMetric NULL_METRIC_INSTANCE = new JavaTestingTestMetric() {
	};
	public static final boolean ENABLED = true;

	public static JavaTestingTestMetric metric() {
		Object res = testing.saker.build.flag.TestFlag.metric();
		if (res instanceof JavaTestingTestMetric) {
			return (JavaTestingTestMetric) res;
		}
		return NULL_METRIC_INSTANCE;
	}

}
