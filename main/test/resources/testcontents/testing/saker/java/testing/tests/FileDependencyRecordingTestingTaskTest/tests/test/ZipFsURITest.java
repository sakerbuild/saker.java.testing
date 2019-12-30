package test;

import java.io.FileOutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

public class ZipFsURITest {

	@org.junit.Test
	public void testzipfsuri() throws Throwable {
		Map<String, String> env = new HashMap<>();
		String p = Paths.get("nonexistent/path/to/archive.zip").toAbsolutePath().toString().replace('\\', '/');
		if (p.startsWith("/")) {
			p = p.substring(1);
		}
		URI uri = URI.create("jar:file:/" + p);
		FileSystem fs = FileSystems.newFileSystem(uri, env);
	}

}