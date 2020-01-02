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

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

public class NioFileSystemSakerProxy {

	public static FileSystemProvider provider(FileSystem fs) {
		return fs.provider();
	}

	public static void close(FileSystem fs) throws IOException {
		fs.close();
	}

	public static boolean isOpen(FileSystem fs) {
		return fs.isOpen();
	}

	public static boolean isReadOnly(FileSystem fs) {
		return fs.isReadOnly();
	}

	public static String getSeparator(FileSystem fs) {
		return fs.getSeparator();
	}

	public static Iterable<Path> getRootDirectories(FileSystem fs) {
		Iterable<Path> roots = fs.getRootDirectories();
		//XXX should we handle getRootDirectories?
		return roots;
	}

	public static Iterable<FileStore> getFileStores(FileSystem fs) {
		return fs.getFileStores();
	}

	public static Set<String> supportedFileAttributeViews(FileSystem fs) {
		return fs.supportedFileAttributeViews();
	}

	public static Path getPath(FileSystem fs, String first, String... more) {
		return fs.getPath(first, more);
	}

	public static PathMatcher getPathMatcher(FileSystem fs, String syntaxAndPattern) {
		return fs.getPathMatcher(syntaxAndPattern);
	}

	public static UserPrincipalLookupService getUserPrincipalLookupService(FileSystem fs) {
		return fs.getUserPrincipalLookupService();
	}

	public static WatchService newWatchService(FileSystem fs) throws IOException {
		return fs.newWatchService();
	}

}
