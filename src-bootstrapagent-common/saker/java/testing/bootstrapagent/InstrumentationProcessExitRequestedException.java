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
package saker.java.testing.bootstrapagent;

public class InstrumentationProcessExitRequestedException extends Throwable {
	private static final long serialVersionUID = 1L;

	private int resultCode;

	public InstrumentationProcessExitRequestedException(int resultcode) {
		super("JVM process exit requested with code: " + resultcode);
		this.resultCode = resultcode;
	}

	public int getResultCode() {
		return resultCode;
	}

}
