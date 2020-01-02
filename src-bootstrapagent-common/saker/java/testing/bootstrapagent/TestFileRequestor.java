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

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;

public class TestFileRequestor {
	private static Consumer<String> fileReadRequestor = TestFileRequestor::ignore;
	private static Consumer<String> fileWriteRequestor = TestFileRequestor::ignore;
	private static Consumer<String> fileListRequestor = TestFileRequestor::ignore;
	private static Consumer<String> classLoaderResourceRequestor = TestFileRequestor::ignore;

	//TODO keep track of opened resources and make the test api close them after tests
	public static final ConcurrentSkipListMap<String, FileAccessInfo> USED_PATHS = new ConcurrentSkipListMap<>();
	public static final ConcurrentSkipListMap<String, NavigableSet<String>> LISTED_DIRECTORY_CONTENTS = new ConcurrentSkipListMap<>();

	private static final ConcurrentSkipListSet<String> requestedClassLoaderResources = new ConcurrentSkipListSet<>();

	@SuppressWarnings("unused")
	private static void ignore(String s) {
	}

	//XXX maybe implement a caching mechanism to avoid calling the consumers multiple times for a single file

	private TestFileRequestor() {
		throw new UnsupportedOperationException();
	}

	public static void init(Consumer<String> filereadrequestor, Consumer<String> filewriterequestor,
			Consumer<String> filelistrequestor, Consumer<String> classloaderresourcerequestor) {
		TestFileRequestor.fileReadRequestor = filereadrequestor;
		TestFileRequestor.fileWriteRequestor = filewriterequestor;
		TestFileRequestor.fileListRequestor = filelistrequestor;
		TestFileRequestor.classLoaderResourceRequestor = classloaderresourcerequestor;

		clearFileRecordingCollections();
	}

	public static void requestReadFile(String path) {
		fileReadRequestor.accept(path);
	}

	public static void requestWriteFile(String path) {
		fileWriteRequestor.accept(path);
	}

	public static void requestListFile(String path) {
		fileListRequestor.accept(path);
	}

	public static void requestClassLoaderResource(String name) {
		if (name == null) {
			return;
		}
		if (requestedClassLoaderResources.add(name)) {
			classLoaderResourceRequestor.accept(name);
		}
	}

	public static void requestClassLoaderResourceFromClass(Class<?> c, String name) {
		if (name == null) {
			return;
		}
		if (name.startsWith("/")) {
			while (c.isArray()) {
				c = c.getComponentType();
			}
			String baseName = c.getName();
			int index = baseName.lastIndexOf('.');
			if (index >= -1) {
				name = baseName.substring(0, index).replace('.', '/') + "/" + name;
			}
		} else {
			name = name.substring(1);
		}
		if (requestedClassLoaderResources.add(name)) {
			classLoaderResourceRequestor.accept(name);
		}
	}

	public static void requestClassLoaderResourceFromModule(String name) {
		if (name == null) {
			return;
		}
		// leading slash is removed as in the Module source
		if (name.startsWith("/")) {
			name = name.substring(1);
		}
		if (requestedClassLoaderResources.add(name)) {
			classLoaderResourceRequestor.accept(name);
		}
	}

	public static void clear() {
		fileReadRequestor = TestFileRequestor::ignore;
		fileWriteRequestor = TestFileRequestor::ignore;
		fileListRequestor = TestFileRequestor::ignore;
		classLoaderResourceRequestor = TestFileRequestor::ignore;

		clearFileRecordingCollections();
	}

	protected static void clearFileRecordingCollections() {
		USED_PATHS.clear();
		LISTED_DIRECTORY_CONTENTS.clear();

		requestedClassLoaderResources.clear();
	}

}
