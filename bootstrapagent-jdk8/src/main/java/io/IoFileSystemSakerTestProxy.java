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
package java.io;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.TreeSet;
import java.util.zip.ZipFile;

import saker.java.testing.bootstrapagent.FileAccessInfo;
import saker.java.testing.bootstrapagent.TestFileRequestor;

public class IoFileSystemSakerTestProxy {

//	private static void addWritten(File file) {
//		addWritten(file.toString());
//	}
//
//	private static void addWritten(String pathstr) {
//		FileAccessInfo.addWritten(pathstr, USED_PATHS);
//	}

	private static void addRead(File file) {
		addRead(file.toString());
	}

	private static void addRead(String pathstr) {
		FileAccessInfo.addRead(pathstr, TestFileRequestor.USED_PATHS);
	}

	private static void addReadWritten(File file) {
		addReadWritten(file.toString());
	}

	private static void addReadWritten(String pathstr) {
		FileAccessInfo.addReadWritten(pathstr, TestFileRequestor.USED_PATHS);
	}

	private static void addDirectoryListed(File f) {
		addDirectoryListed(f.toString());
	}

	private static void addDirectoryListed(String pathstr) {
		FileAccessInfo.addDirectoryListed(pathstr, TestFileRequestor.USED_PATHS);
	}

	@SuppressWarnings("unused")
	public static void newFileOutputStream(File file, boolean append) {
		addReadWritten(file);
	}

	public static void newFileInputStream(File file) {
		addRead(file);
	}

	@SuppressWarnings("unused")
	public static void newZipFile(File file, int mode, Charset charset) {
		if (((mode & ZipFile.OPEN_DELETE) == ZipFile.OPEN_DELETE)) {
			addReadWritten(file);
		} else {
			addRead(file);
		}
	}

	public static void newRandomAccessFile(File file, String mode) {
		boolean r = mode.contains("r");
		boolean w = mode.contains("w");
		if (r && !w) {
			addRead(file);
		} else {
			addReadWritten(file);
		}
	}

	public static char getSeparator(FileSystem fs) {
		return fs.getSeparator();
	}

	public static char getPathSeparator(FileSystem fs) {
		return fs.getPathSeparator();
	}

	public static String normalize(FileSystem fs, String path) {
		return fs.normalize(path);
	}

	public static int prefixLength(FileSystem fs, String path) {
		return fs.prefixLength(path);
	}

	public static String resolve(FileSystem fs, String parent, String child) {
		return fs.resolve(parent, child);
	}

	public static String getDefaultParent(FileSystem fs) {
		return fs.getDefaultParent();
	}

	public static String fromURIPath(FileSystem fs, String path) {
		return fs.fromURIPath(path);
	}

	public static boolean isAbsolute(FileSystem fs, File f) {
		return fs.isAbsolute(f);
	}

	public static String resolve(FileSystem fs, File f) {
		return fs.resolve(f);
	}

	public static String canonicalize(FileSystem fs, String path) throws IOException {
		//XXX should we add this or not?
//		addReadImpl(path);
		return fs.canonicalize(path);
	}

	public static int getBooleanAttributes(FileSystem fs, File f) {
		addRead(f);
		return fs.getBooleanAttributes(f);
	}

	public static boolean checkAccess(FileSystem fs, File f, int access) {
		addRead(f);
		return fs.checkAccess(f, access);
	}

	public static boolean setPermission(FileSystem fs, File f, int access, boolean enable, boolean owneronly) {
		addReadWritten(f);
		return fs.setPermission(f, access, enable, owneronly);
	}

	public static long getLastModifiedTime(FileSystem fs, File f) {
		addRead(f);
		return fs.getLastModifiedTime(f);
	}

	public static long getLength(FileSystem fs, File f) {
		addRead(f);
		return fs.getLength(f);
	}

	public static boolean createFileExclusively(FileSystem fs, String pathname) throws IOException {
		addReadWritten(pathname);
		return fs.createFileExclusively(pathname);
	}

	public static boolean delete(FileSystem fs, File f) {
		addReadWritten(f);
		return fs.delete(f);
	}

	public static String[] list(FileSystem fs, File f) {
		addRead(f);
		addDirectoryListed(f);
		String[] result = fs.list(f);
		String fstr = f.toString();
		if (result != null) {
			String slashedfstr = fstr;
			if (!slashedfstr.endsWith("/") && !slashedfstr.endsWith("\\")) {
				slashedfstr += fs.getSeparator();
			}
			for (String r : result) {
				String resolved = slashedfstr + r;
				addRead(resolved);
			}
			TestFileRequestor.LISTED_DIRECTORY_CONTENTS.computeIfAbsent(fstr, x -> {
				TreeSet<String> tsres = new TreeSet<>();
				for (String subf : result) {
					tsres.add(subf);
				}
				return tsres;
			});
		} else {
			TestFileRequestor.LISTED_DIRECTORY_CONTENTS.putIfAbsent(fstr, Collections.emptyNavigableSet());
		}
		return result;
	}

	public static boolean createDirectory(FileSystem fs, File f) {
		addReadWritten(f);
		return fs.createDirectory(f);
	}

	public static boolean rename(FileSystem fs, File f1, File f2) {
		addReadWritten(f1);
		addReadWritten(f2);
		return fs.rename(f1, f2);
	}

	public static boolean setLastModifiedTime(FileSystem fs, File f, long time) {
		addReadWritten(f);
		return fs.setLastModifiedTime(f, time);
	}

	public static boolean setReadOnly(FileSystem fs, File f) {
		addReadWritten(f);
		return fs.setReadOnly(f);
	}

	public static File[] listRoots(FileSystem fs) {
		File[] roots = fs.listRoots();
		//XXX should we handle listRoots?
		return roots;
	}

	public static long getSpace(FileSystem fs, File f, int t) {
		return fs.getSpace(f, t);
	}

	public static int compare(FileSystem fs, File f1, File f2) {
		return fs.compare(f1, f2);
	}

	public static int hashCode(FileSystem fs, File f) {
		return fs.hashCode(f);
	}

}
