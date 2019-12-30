package test;

import java.io.InputStream;

public class Main {
	public static void main(String[] args) throws Throwable {
		ClassLoader cl = Main.class.getClassLoader();
		try {
			cl.getResourceAsStream(null);
		} catch (NullPointerException e) {
		}
		try (InputStream is = cl.getResourceAsStream("stream/resource")) {
		}
		cl.getResource("url/resource");
		cl.getResources("url/resources");
		try (InputStream is = Main.class.getResourceAsStream("/abs/resource")) {
		}
		Main.class.getResource("/abs/url/resource");
		try (InputStream is = Main.class.getResourceAsStream("class/resource")) {
		}
		Main.class.getResource("class/url/resource");
	}

}