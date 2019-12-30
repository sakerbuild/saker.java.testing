package test;

public class LongTest extends TestBase {
	@org.junit.Test
	public void test() {
		try {
			Thread.sleep(200);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}