package test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
	public static void main(String[] args) throws Throwable {
		System.out.println("Main.main()");
		//attempt to read a file for illegal access
		String content = new String(Files.readAllBytes(Paths.get("file.txt")));
		if (!"123".equals(content)) {
			throw new AssertionError(content);
		}
		File f = new File("file.txt");
		System.out.println("Main.main() " + f.exists());
		try (FileInputStream fis = new FileInputStream("file.txt")) {
			if (fis.read() != '1' || fis.read() != '2' || fis.read() != '3') {
				throw new AssertionError();
			}
		}

	}

}