package test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Echo {
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			return;
		}
		Path p = Paths.get(args[0]).toAbsolutePath();
		Files.createDirectories(p.getParent());
		Files.write(p, args[1].getBytes());
	}
}