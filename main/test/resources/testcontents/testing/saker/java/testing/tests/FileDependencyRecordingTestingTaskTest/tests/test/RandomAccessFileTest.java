package test;

import java.io.RandomAccessFile;
import java.util.jar.JarFile;

public class RandomAccessFileTest {
	@org.junit.Test
	public void testraf() throws Throwable {
		try (RandomAccessFile f = new RandomAccessFile("nonexistent/path/to/file.txt", "rw")) {
		}
	}
}