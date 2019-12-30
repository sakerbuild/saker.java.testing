package test;

public class LoadCLNotFoundMain {
	public static void main(String[] args) throws Throwable {
		LoadCLNotFoundMain.class.getClassLoader().loadClass("test.ClassPathMainLoadCL");
	}

}