package testing.saker.java.testing;

import saker.build.file.path.SakerPath;
import testing.saker.nest.util.NestRepositoryCachingEnvironmentTestCase;

public abstract class JavaTestingVariablesMetricEnvironmentTestCase extends NestRepositoryCachingEnvironmentTestCase {
	public static final SakerPath SRC_PATH_BASE = PATH_WORKING_DIRECTORY.resolve("src");

	@Override
	protected TestingCollectingTestMetric createMetricImpl() {
		return new TestingCollectingTestMetric();
	}

	@Override
	protected TestingCollectingTestMetric getMetric() {
		return (TestingCollectingTestMetric) super.getMetric();
	}
}
