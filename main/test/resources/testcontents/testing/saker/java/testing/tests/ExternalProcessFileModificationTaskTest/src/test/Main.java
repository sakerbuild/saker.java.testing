package test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

@TestAnnot
@SecondAnnot
public class Main {
	public static void main(String[] args) throws Exception {
		System.out.println("Main.main()");

		Path cp = Paths.get("").toAbsolutePath();
		Path outcfile = cp.resolve(Echo.class.getCanonicalName().replace('.', '/') + ".class");
		Files.createDirectories(outcfile.getParent());

		try (InputStream is = Echo.class.getClassLoader()
				.getResourceAsStream(Echo.class.getCanonicalName().replace(".", "/") + ".class")) {
			Files.copy(is, outcfile, StandardCopyOption.REPLACE_EXISTING);
		}

		Path outfile = Paths.get("output.txt");

		String outfileexpectedcontents = "contents";

		ProcessBuilder pb = new ProcessBuilder("java", "-cp", cp.toString(), Echo.class.getCanonicalName(),
				"output.txt", outfileexpectedcontents);
		pb.redirectErrorStream(true);

		int res = pb.start().waitFor();
		if (res != 0) {
			throw new AssertionError(res);
		}

		BasicFileAttributes attrs = Files.readAttributes(outfile, BasicFileAttributes.class);
		if (!attrs.isRegularFile()) {
			throw new AssertionError(attrs);
		}
		byte[] outfilebytes = Files.readAllBytes(outfile);
		if (!outfileexpectedcontents.equals(new String(outfilebytes))) {
			throw new AssertionError(new String(outfilebytes));
		}
	}

}