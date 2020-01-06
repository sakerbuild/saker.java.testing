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
package saker.java.testing.bootstrapagent.java.io;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.TreeSet;
import java.util.zip.ZipFile;

import saker.java.testing.bootstrapagent.FileAccessInfo;
import saker.java.testing.bootstrapagent.TestFileRequestor;

public class IoFileSystemSakerTestProxy {
	private static final MethodHandle getSeparator;
	private static final MethodHandle getPathSeparator;
	private static final MethodHandle normalize;
	private static final MethodHandle prefixLength;
	private static final MethodHandle resolveStringString;
	private static final MethodHandle getDefaultParent;
	private static final MethodHandle fromURIPath;
	private static final MethodHandle isAbsolute;
	private static final MethodHandle resolveFile;
	private static final MethodHandle canonicalize;
	private static final MethodHandle getBooleanAttributes;
	private static final MethodHandle checkAccess;
	private static final MethodHandle setPermission;
	private static final MethodHandle getLastModifiedTime;
	private static final MethodHandle getLength;
	private static final MethodHandle createFileExclusively;
	private static final MethodHandle delete;
	private static final MethodHandle list;
	private static final MethodHandle createDirectory;
	private static final MethodHandle rename;
	private static final MethodHandle setLastModifiedTime;
	private static final MethodHandle setReadOnly;
	private static final MethodHandle listRoots;
	private static final MethodHandle getSpace;
	private static final MethodHandle getNameMax;
	private static final MethodHandle compare;
	private static final MethodHandle hashCode;

	static {
		try {
			Class<?> fsclass = Class.forName("java.io.FileSystem", false, File.class.getClassLoader());
			Lookup lookup = MethodHandles.lookup();
			getSeparator = unreflectFileSystemMethod(lookup, fsclass, "getSeparator", char.class);
			getPathSeparator = unreflectFileSystemMethod(lookup, fsclass, "getPathSeparator", char.class);
			normalize = unreflectFileSystemMethod(lookup, fsclass, "normalize", char.class, String.class);
			prefixLength = unreflectFileSystemMethod(lookup, fsclass, "prefixLength", char.class, String.class);
			resolveStringString = unreflectFileSystemMethod(lookup, fsclass, "resolve", String.class, String.class,
					String.class);
			getDefaultParent = unreflectFileSystemMethod(lookup, fsclass, "getDefaultParent", String.class);
			fromURIPath = unreflectFileSystemMethod(lookup, fsclass, "fromURIPath", String.class, String.class);
			isAbsolute = unreflectFileSystemMethod(lookup, fsclass, "isAbsolute", boolean.class, File.class);
			resolveFile = unreflectFileSystemMethod(lookup, fsclass, "resolve", String.class, File.class);
			canonicalize = unreflectFileSystemMethod(lookup, fsclass, "canonicalize", String.class, String.class);
			getBooleanAttributes = unreflectFileSystemMethod(lookup, fsclass, "getBooleanAttributes", int.class,
					File.class);
			checkAccess = unreflectFileSystemMethod(lookup, fsclass, "checkAccess", boolean.class, File.class,
					int.class);
			setPermission = unreflectFileSystemMethod(lookup, fsclass, "setPermission", boolean.class, File.class,
					int.class, boolean.class, boolean.class);
			getLastModifiedTime = unreflectFileSystemMethod(lookup, fsclass, "getLastModifiedTime", long.class,
					File.class);
			getLength = unreflectFileSystemMethod(lookup, fsclass, "getLength", long.class, File.class);
			createFileExclusively = unreflectFileSystemMethod(lookup, fsclass, "createFileExclusively", boolean.class,
					String.class);
			delete = unreflectFileSystemMethod(lookup, fsclass, "delete", boolean.class, File.class);
			list = unreflectFileSystemMethod(lookup, fsclass, "list", String[].class, File.class);
			createDirectory = unreflectFileSystemMethod(lookup, fsclass, "createDirectory", boolean.class, File.class);
			rename = unreflectFileSystemMethod(lookup, fsclass, "rename", boolean.class, File.class, File.class);
			setLastModifiedTime = unreflectFileSystemMethod(lookup, fsclass, "setLastModifiedTime", boolean.class,
					File.class, long.class);
			setReadOnly = unreflectFileSystemMethod(lookup, fsclass, "setReadOnly", boolean.class, File.class);
			listRoots = unreflectFileSystemMethod(lookup, fsclass, "listRoots", File[].class);
			getSpace = unreflectFileSystemMethod(lookup, fsclass, "getSpace", long.class, File.class, int.class);
			getNameMax = unreflectFileSystemMethod(lookup, fsclass, "getNameMax", int.class, String.class);
			compare = unreflectFileSystemMethod(lookup, fsclass, "compare", int.class, File.class, File.class);
			hashCode = unreflectFileSystemMethod(lookup, fsclass, "hashCode", int.class, File.class);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private static MethodHandle unreflectFileSystemMethod(Lookup lookup, Class<?> fsclass, String methodname,
			Class<?> returntype, Class<?>... parametertypes) throws Exception {
		Method m = fsclass.getMethod(methodname, parametertypes);
		m.setAccessible(true);
		return lookup.unreflect(m);
	}

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
	public static void newFileOutputStream(File file, boolean append) throws Throwable {
		addReadWritten(file);
	}

	public static void newFileInputStream(File file) throws Throwable {
		addRead(file);
	}

	@SuppressWarnings("unused")
	public static void newZipFile(File file, int mode, Charset charset) throws Throwable {
		if (((mode & ZipFile.OPEN_DELETE) == ZipFile.OPEN_DELETE)) {
			addReadWritten(file);
		} else {
			addRead(file);
		}
	}

	public static void newRandomAccessFile(File file, String mode) throws Throwable {
		boolean r = mode.contains("r");
		boolean w = mode.contains("w");
		if (r && !w) {
			addRead(file);
		} else {
			addReadWritten(file);
		}
	}

	public static char getSeparator(Object fs) throws Throwable {
		return (char) getSeparator.invoke(fs);
//		return fs.getSeparator();
	}

	public static char getPathSeparator(Object fs) throws Throwable {
		return (char) getPathSeparator.invoke(fs);
	}

	public static String normalize(Object fs, String path) throws Throwable {
		return (String) normalize.invoke(fs, path);
	}

	public static int prefixLength(Object fs, String path) throws Throwable {
		return (int) prefixLength.invoke(fs, path);
	}

	public static String resolve(Object fs, String parent, String child) throws Throwable {
		return (String) resolveStringString.invoke(fs, parent, child);
	}

	public static String getDefaultParent(Object fs) throws Throwable {
		return (String) getDefaultParent.invoke(fs);
	}

	public static String fromURIPath(Object fs, String path) throws Throwable {
		return (String) fromURIPath.invoke(fs, path);
	}

	public static boolean isAbsolute(Object fs, File f) throws Throwable {
		return (boolean) isAbsolute.invoke(fs, f);
	}

	public static String resolve(Object fs, File f) throws Throwable {
		return (String) resolveFile.invoke(fs, f);
	}

	public static String canonicalize(Object fs, String path) throws Throwable {
		//XXX should we add this or not?
//		addReadImpl(path);
		return (String) canonicalize.invoke(fs, path);
	}

	public static int getBooleanAttributes(Object fs, File f) throws Throwable {
		addRead(f);
		return (int) getBooleanAttributes.invoke(fs, f);
	}

	public static boolean checkAccess(Object fs, File f, int access) throws Throwable {
		addRead(f);
		return (boolean) checkAccess.invoke(fs, f, access);
	}

	public static boolean setPermission(Object fs, File f, int access, boolean enable, boolean owneronly)
			throws Throwable {
		addReadWritten(f);
		return (boolean) setPermission.invoke(fs, f, access, enable, owneronly);
	}

	public static long getLastModifiedTime(Object fs, File f) throws Throwable {
		addRead(f);
		return (long) getLastModifiedTime.invoke(fs, f);
	}

	public static long getLength(Object fs, File f) throws Throwable {
		addRead(f);
		return (long) getLength.invoke(fs, f);
	}

	public static boolean createFileExclusively(Object fs, String pathname) throws Throwable {
		addReadWritten(pathname);
		return (boolean) createFileExclusively.invoke(fs, pathname);
	}

	public static boolean delete(Object fs, File f) throws Throwable {
		addReadWritten(f);
		return (boolean) delete.invoke(fs, f);
	}

	public static String[] list(Object fs, File f) throws Throwable {
		addRead(f);
		addDirectoryListed(f);
		String[] result = (String[]) list.invoke(fs, f);
		String fstr = f.toString();
		if (result != null) {
			String slashedfstr = fstr;
			if (!slashedfstr.endsWith("/") && !slashedfstr.endsWith("\\")) {
				slashedfstr += (char) getSeparator.invoke(fs);
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

	public static boolean createDirectory(Object fs, File f) throws Throwable {
		addReadWritten(f);
		return (boolean) createDirectory.invoke(fs, f);
	}

	public static boolean rename(Object fs, File f1, File f2) throws Throwable {
		addReadWritten(f1);
		addReadWritten(f2);
		return (boolean) rename.invoke(fs, f1, f2);
	}

	public static boolean setLastModifiedTime(Object fs, File f, long time) throws Throwable {
		addReadWritten(f);
		return (boolean) setLastModifiedTime.invoke(fs, f, time);
	}

	public static boolean setReadOnly(Object fs, File f) throws Throwable {
		addReadWritten(f);
		return (boolean) setReadOnly.invoke(fs, f);
	}

	public static File[] listRoots(Object fs) throws Throwable {
		File[] roots = (File[]) listRoots.invoke(fs);
		//XXX should we handle listRoots?
		return roots;
	}

	public static long getSpace(Object fs, File f, int t) throws Throwable {
		return (long) getSpace.invoke(fs, f, t);
	}

	public static int getNameMax(Object fs, String path) throws Throwable {
		return (int) getNameMax.invoke(fs, path);
	}

	public static int compare(Object fs, File f1, File f2) throws Throwable {
		return (int) compare.invoke(fs, f1, f2);
	}

	public static int hashCode(Object fs, File f) throws Throwable {
		return (int) hashCode.invoke(fs, f);
	}
}
