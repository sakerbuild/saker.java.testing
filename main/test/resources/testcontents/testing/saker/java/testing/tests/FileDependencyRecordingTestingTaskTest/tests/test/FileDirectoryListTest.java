package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public class FileDirectoryListTest {
	@org.junit.Test
	public void testfiles() throws Throwable {
		File thefile = new File("listing/subdir");
		String[] files = thefile.list();
		if (files != null) {
			for (String name : files) {
				System.out.println("FullPathDirectoryListTest.testfiles() " + name);
			}
		}
		throw new RuntimeException("fail");
	}
}