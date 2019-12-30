package test;

import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class DirectoryListTest {
	@org.junit.Test
	public void test() throws Throwable {
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get("listing"))) {
			Set<String> names = new TreeSet<>();
			for (Path p : ds) {
				names.add(p.getFileName().toString());
			}
			if (!names.equals(new TreeSet<>(Arrays.asList("a.txt", "b.txt")))) {
				throw new IllegalStateException("Invalid files: " + names);
			}
		}
	}
}