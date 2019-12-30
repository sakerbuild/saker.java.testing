package test;

@SecondAnnot
public class ClassPathMain {
	public static void main(String[] args) {
		System.out.println("Main.main()");
		Sub.function();
	}

	public static class Sub {
		public static void function() {

		}
	}
}