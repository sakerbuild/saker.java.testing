package test;

@TestAnnot
@SecondAnnot
public class Main {
	public static void main(String[] args) {
		System.out.println("Main.main()");
//		System.exit(111);
		Sub.function();
	}

	public static class Sub {
		public static void function() {

		}
	}
}