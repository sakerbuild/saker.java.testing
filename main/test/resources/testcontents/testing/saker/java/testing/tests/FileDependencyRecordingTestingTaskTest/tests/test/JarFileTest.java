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

public class JarFileTest {

	@org.junit.Test
	public void testjar() throws Throwable {
		new JarFile("nonexistent/path/to/archive.jar");
	}

}