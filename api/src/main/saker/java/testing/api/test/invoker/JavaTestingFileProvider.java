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
package saker.java.testing.api.test.invoker;

/**
 * Interface for notify the test instrumentation about a resource access.
 * <p>
 * The appropriate methods of the interface can be called in order to notify the testing instrumentation that a file
 * read, write, or access is being done by the test case. The instrumentation may perform synchronization operations in
 * order to make the files available for access to the test case.
 * <p>
 * The provider also records the incremental dependency information about the test case.
 */
public interface JavaTestingFileProvider {
	/**
	 * A file read request is performed by the test case.
	 * <p>
	 * Should be called for files and directories as well.
	 * 
	 * @param path
	 *            The file path.
	 */
	public void requestFileRead(String path);

	/**
	 * A file write request is performed by the test case.
	 * <p>
	 * Should be called for files and directories as well.
	 * 
	 * @param path
	 *            The file path.
	 */
	public void requestFileWrite(String path);

	/**
	 * The contents of a directory is being listed by the test case.
	 * 
	 * @param path
	 *            The directory path.
	 */
	public void requestFileList(String path);

	/**
	 * A {@link ClassLoader} resources is being accessed by the test case.
	 * 
	 * @param name
	 *            The name of the resource.
	 * @see ClassLoader#getResource(String)
	 */
	public void requestClassLoaderResource(String name);
}
