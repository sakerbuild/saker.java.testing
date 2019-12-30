package test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DirectoryDeleteTest {
	@org.junit.Test
	public void test() throws Throwable {
		Files.delete(Paths.get("deldirectory"));
	}
}