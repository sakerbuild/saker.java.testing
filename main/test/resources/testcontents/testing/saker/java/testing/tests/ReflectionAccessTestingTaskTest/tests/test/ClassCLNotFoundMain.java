package test;

public class ClassCLNotFoundMain {
	public static void main(String[] args) throws Throwable {
		Class.forName("test.ClassPathMainAddedCL", false, ClassCLNotFoundMain.class.getClassLoader());
	}

}