package test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ClassPathMain {
	public static void main(String[] args) throws Throwable {
		Files.newInputStream(Paths.get("nonexistent/path/to/file.txt"));
	}
}