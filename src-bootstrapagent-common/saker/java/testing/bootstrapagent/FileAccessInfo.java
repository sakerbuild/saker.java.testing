package saker.java.testing.bootstrapagent;

import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class FileAccessInfo {
	private static final AtomicIntegerFieldUpdater<FileAccessInfo> AIFU_accessFlags = AtomicIntegerFieldUpdater
			.newUpdater(FileAccessInfo.class, "accessFlags");

	private static final int FLAG_READ = 1 << 0;
	private static final int FLAG_WRITTEN = 1 << 1;
	private static final int FLAG_LISTED = 1 << 2;

	private volatile int accessFlags = 0;
	private String path;

	public FileAccessInfo(String path) {
		this.path = path;
	}

	private void addFlags(final int additionalflags) {
		if (((additionalflags & FLAG_READ) == FLAG_READ)) {
			TestFileRequestor.requestReadFile(path);
		}
		if (((additionalflags & FLAG_LISTED) == FLAG_LISTED)) {
			TestFileRequestor.requestListFile(path);
		}
		if (((additionalflags & FLAG_WRITTEN) == FLAG_WRITTEN)) {
			TestFileRequestor.requestWriteFile(path);
		}
		while (true) {
			int flags = accessFlags;
			if (((flags & additionalflags) == additionalflags)) {
				//already has all flags that needs to be set
				return;
			}
			if (AIFU_accessFlags.compareAndSet(this, flags, flags | additionalflags)) {
				return;
			}
		}
	}

	public boolean isRead() {
		return ((accessFlags & FLAG_READ) == FLAG_READ);
	}

	public boolean isWritten() {
		return ((accessFlags & FLAG_WRITTEN) == FLAG_WRITTEN);
	}

	public boolean isDirectoryListed() {
		return ((accessFlags & FLAG_LISTED) == FLAG_LISTED);
	}

	public void setRead() {
		addFlags(FLAG_READ);
	}

//	public void setWritten() {
//		addFlags(FLAG_WRITTEN);
//	}

	public void setReadWritten() {
		addFlags(FLAG_READ | FLAG_WRITTEN);
	}

	public void setDirectoryListed() {
		addFlags(FLAG_READ | FLAG_LISTED);
	}

	public static <T> void addRead(T file, Map<T, FileAccessInfo> map) {
		getFileAccessInfoFromMap(file, map).setRead();
	}

//	public static <T> void addWritten(T file, Map<T, FileAccessInfo> map) {
//		getFileAccessInfoFromMap(file, map).setWritten();
//	}

	public static <T> void addDirectoryListed(T file, Map<T, FileAccessInfo> map) {
		getFileAccessInfoFromMap(file, map).setDirectoryListed();
	}

	public static <T> void addReadWritten(T file, Map<T, FileAccessInfo> map) {
		getFileAccessInfoFromMap(file, map).setReadWritten();
	}

	private static <T> FileAccessInfo getFileAccessInfoFromMap(T file, Map<T, FileAccessInfo> map) {
		return map.computeIfAbsent(file, f -> new FileAccessInfo(f.toString()));
//		FileAccessInfo fai = new FileAccessInfo(file.toString());
//		FileAccessInfo prev = map.putIfAbsent(file, fai);
//		if (prev != null) {
//			fai = prev;
//		}
//		return fai;
	}
}
