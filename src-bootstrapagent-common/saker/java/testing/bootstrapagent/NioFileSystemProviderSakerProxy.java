package saker.java.testing.bootstrapagent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;

//TODO add support to creation of JAR filesystem
public class NioFileSystemProviderSakerProxy {
	private static class RecordingDirectoryStream implements DirectoryStream<Path> {
		protected class RecordingDirectoryIterator implements Iterator<Path> {
			private final Iterator<Path> it;

			protected RecordingDirectoryIterator(Iterator<Path> it) {
				this.it = it;
			}

			@Override
			public boolean hasNext() {
				boolean result = it.hasNext();
				if (!result) {
					fullyIterated();
				}
				return result;
			}

			@Override
			public Path next() {
				Path result = it.next();
				addReadImpl(result);
				return result;
			}

			protected void fullyIterated() {
				//this means that the consumer of this stream has iterated the directory fully
				//meaning he was searching for something, and havent found it, or needed all the files in the directory
				//in these cases, and only in these, are any additions to the directory needs to be tracked

				//if the user doesn't iterate the directory contents fully, it means that even if we add any file to the directory,
				//it wouldnt necessarily be listed. If the test result would depend on the presence of a file that has not been enumerated,
				//that would violate the deterministic test policy. We can avoid tracking directory content additions as the result
				//because the stream returns the children in an unspecified order.

				//so: add the directory to the tracked directory list
				addDirectoryListedImpl(dir);
			}
		}

		protected final Path dir;
		protected final DirectoryStream<Path> stream;

		protected RecordingDirectoryStream(Path dir, DirectoryStream<Path> stream) {
			this.dir = dir;
			this.stream = stream;
		}

		@Override
		public void close() throws IOException {
			stream.close();
		}

		@Override
		public Iterator<Path> iterator() {
			Iterator<Path> it = stream.iterator();
			return createIterator(it);
		}

		protected RecordingDirectoryIterator createIterator(Iterator<Path> it) {
			return new RecordingDirectoryIterator(it);
		}
	}

	private static class ListingRecordingDirectoryStream extends RecordingDirectoryStream {
		protected ListingRecordingDirectoryStream(Path dir, DirectoryStream<Path> stream) {
			super(dir, stream);
		}

		@Override
		protected RecordingDirectoryIterator createIterator(Iterator<Path> it) {
			return new RecordingDirectoryIterator(it) {
				private NavigableSet<String> elements = new TreeSet<>();

				@Override
				public Path next() {
					Path result = super.next();
					elements.add(result.getFileName().toString());
					return result;
				}

				@Override
				protected void fullyIterated() {
					super.fullyIterated();
					TestFileRequestor.LISTED_DIRECTORY_CONTENTS.putIfAbsent(dir.toString(), elements);
				}
			};
		}
	}

	//TODO we should create a proxy for Files class too e.g. for walkdirectory

	private static void addReadImpl(Path path) {
		FileAccessInfo.addRead(path.toString(), TestFileRequestor.USED_PATHS);
	}

//	private static void addWrittenImpl(Path path) {
//		FileAccessInfo.addWritten(path, USED_PATHS);
//	}

	private static void addReadWrittenImpl(Path path) {
		FileAccessInfo.addReadWritten(path.toString(), TestFileRequestor.USED_PATHS);
	}

	private static void addDirectoryListedImpl(Path dir) {
		FileAccessInfo.addDirectoryListed(dir.toString(), TestFileRequestor.USED_PATHS);
	}

	private static void addRead(FileSystemProvider fsp, Path path) {
		if ("file".equals(fsp.getScheme())) {
			addReadImpl(path);
		}
	}

//	private static void addWritten(FileSystemProvider fsp, Path path) {
//		if ("file".equals(fsp.getScheme())) {
//			addWrittenImpl(path);
//		}
//	}

	private static void addReadWritten(FileSystemProvider fsp, Path path) {
		if ("file".equals(fsp.getScheme())) {
			addReadWrittenImpl(path);
		}
	}

	public static String getScheme(FileSystemProvider fsp) {
		return fsp.getScheme();
	}

	public static FileSystem newFileSystem(FileSystemProvider fsp, URI uri, Map<String, ?> env) throws IOException {
		//TODO support jar
		return fsp.newFileSystem(uri, env);
	}

	public static FileSystem getFileSystem(FileSystemProvider fsp, URI uri) {
		//nothing to do, it only returns an already existing file system
		return fsp.getFileSystem(uri);
	}

	public static Path getPath(FileSystemProvider fsp, URI uri) {
		//do not add to used, as it doesnt acces the file system
		return fsp.getPath(uri);
	}

	public static FileSystem newFileSystem(FileSystemProvider fsp, Path path, Map<String, ?> env) throws IOException {
		addRead(fsp, path);
		return fsp.newFileSystem(path, env);
	}

	public static InputStream newInputStream(FileSystemProvider fsp, Path path, OpenOption... options)
			throws IOException {
		if (!"file".equals(fsp.getScheme())) {
			return fsp.newInputStream(path, options);
		}
		adder_block:
		{
			for (OpenOption opt : options) {
				if (opt == StandardOpenOption.DELETE_ON_CLOSE || opt == StandardOpenOption.CREATE
						|| opt == StandardOpenOption.CREATE_NEW) {
					addReadWrittenImpl(path);
					break adder_block;
				}
			}
			addReadImpl(path);
		}
		return fsp.newInputStream(path, options);
	}

	public static OutputStream newOutputStream(FileSystemProvider fsp, Path path, OpenOption... options)
			throws IOException {
		if (!"file".equals(fsp.getScheme())) {
			return fsp.newOutputStream(path, options);
		}
		addReadWrittenImpl(path);
		return fsp.newOutputStream(path, options);
	}

	public static FileChannel newFileChannel(FileSystemProvider fsp, Path path, Set<? extends OpenOption> options,
			FileAttribute<?>... attrs) throws IOException {
		addReadWritten(fsp, path);
		return fsp.newFileChannel(path, options, attrs);
	}

	public static AsynchronousFileChannel newAsynchronousFileChannel(FileSystemProvider fsp, Path path,
			Set<? extends OpenOption> options, ExecutorService executor, FileAttribute<?>... attrs) throws IOException {
		addReadWritten(fsp, path);
		return fsp.newAsynchronousFileChannel(path, options, executor, attrs);
	}

	public static SeekableByteChannel newByteChannel(FileSystemProvider fsp, Path path,
			Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
		addReadWritten(fsp, path);
		return fsp.newByteChannel(path, options, attrs);
	}

	public static DirectoryStream<Path> newDirectoryStream(FileSystemProvider fsp, Path dir,
			DirectoryStream.Filter<? super Path> filter) throws IOException {
		if (!"file".equals(fsp.getScheme())) {
			return fsp.newDirectoryStream(dir, filter);
		}
		addReadImpl(dir);
		DirectoryStream<Path> stream;
		try {
			stream = fsp.newDirectoryStream(dir, filter);
		} catch (IOException e) {
			//TODO we should signal differently that a listing has failed and successful listing might change the result here and in java.io proxy
			addDirectoryListedImpl(dir);
			TestFileRequestor.LISTED_DIRECTORY_CONTENTS.putIfAbsent(dir.toString(), Collections.emptyNavigableSet());
			throw e;
		}
		if (TestFileRequestor.LISTED_DIRECTORY_CONTENTS.containsKey(dir.toString())) {
			return new RecordingDirectoryStream(dir, stream);
		}
		return new ListingRecordingDirectoryStream(dir, stream);
	}

	public static void createDirectory(FileSystemProvider fsp, Path dir, FileAttribute<?>... attrs) throws IOException {
		addReadWritten(fsp, dir);
		fsp.createDirectory(dir, attrs);
	}

	public static void createSymbolicLink(FileSystemProvider fsp, Path link, Path target, FileAttribute<?>... attrs)
			throws IOException {
		if ("file".equals(fsp.getScheme())) {
			addReadWrittenImpl(link);
			addReadImpl(target);
		}
		fsp.createSymbolicLink(link, target, attrs);
	}

	public static void createLink(FileSystemProvider fsp, Path link, Path existing) throws IOException {
		if ("file".equals(fsp.getScheme())) {
			addReadWrittenImpl(link);
			addReadImpl(existing);
		}
		fsp.createLink(link, existing);
	}

	public static void delete(FileSystemProvider fsp, Path path) throws IOException {
		if (!"file".equals(fsp.getScheme())) {
			fsp.delete(path);
			return;
		}
		addReadWritten(fsp, path);
		fsp.delete(path);
	}

	public static boolean deleteIfExists(FileSystemProvider fsp, Path path) throws IOException {
		if (!"file".equals(fsp.getScheme())) {
			return fsp.deleteIfExists(path);
		}
		addReadWritten(fsp, path);
		return fsp.deleteIfExists(path);
	}

	public static Path readSymbolicLink(FileSystemProvider fsp, Path link) throws IOException {
		addRead(fsp, link);
		return fsp.readSymbolicLink(link);
	}

	public static void copy(FileSystemProvider fsp, Path source, Path target, CopyOption... options)
			throws IOException {
		if ("file".equals(fsp.getScheme())) {
			addReadImpl(source);
			addReadWrittenImpl(target);
		}
		fsp.copy(source, target, options);
	}

	public static void move(FileSystemProvider fsp, Path source, Path target, CopyOption... options)
			throws IOException {
		if ("file".equals(fsp.getScheme())) {
			addReadWrittenImpl(source);
			addReadWrittenImpl(target);
		}
		fsp.move(source, target, options);
	}

	public static boolean isSameFile(FileSystemProvider fsp, Path path, Path path2) throws IOException {
		if ("file".equals(fsp.getScheme())) {
			addReadImpl(path);
			addReadImpl(path2);
		}
		return fsp.isSameFile(path, path2);
	}

	public static boolean isHidden(FileSystemProvider fsp, Path path) throws IOException {
		addRead(fsp, path);
		return fsp.isHidden(path);
	}

	public static FileStore getFileStore(FileSystemProvider fsp, Path path) throws IOException {
		addRead(fsp, path);
		return fsp.getFileStore(path);
	}

	public static void checkAccess(FileSystemProvider fsp, Path path, AccessMode... modes) throws IOException {
		addRead(fsp, path);
		fsp.checkAccess(path, modes);
	}

	public static <V extends FileAttributeView> V getFileAttributeView(FileSystemProvider fsp, Path path, Class<V> type,
			LinkOption... options) {
		addRead(fsp, path);
		return fsp.getFileAttributeView(path, type, options);
	}

	public static <A extends BasicFileAttributes> A readAttributes(FileSystemProvider fsp, Path path, Class<A> type,
			LinkOption... options) throws IOException {
		addRead(fsp, path);
		return fsp.readAttributes(path, type, options);
	}

	public static Map<String, Object> readAttributes(FileSystemProvider fsp, Path path, String attributes,
			LinkOption... options) throws IOException {
		addRead(fsp, path);
		return fsp.readAttributes(path, attributes, options);
	}

	public static void setAttribute(FileSystemProvider fsp, Path path, String attribute, Object value,
			LinkOption... options) throws IOException {
		addReadWritten(fsp, path);
		fsp.setAttribute(path, attribute, value, options);
	}
}
