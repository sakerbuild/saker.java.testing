package testing.saker.java.testing;

public class TestFlag {
	private static final JavaTestingTestMetric NULL_METRIC_INSTANCE = new JavaTestingTestMetric() {
	};
	public static final boolean ENABLED = true;

	public static JavaTestingTestMetric metric() {
		Object res = testing.saker.build.flag.TestFlag.metric();
		if (res instanceof JavaTestingTestMetric) {
			return (JavaTestingTestMetric) res;
		}
		return NULL_METRIC_INSTANCE;
	}

}
