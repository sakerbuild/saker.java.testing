package saker.java.testing.api.test.exc;

/**
 * Thrown if the test execution failed for some reason.
 */
public class JavaTestingFailedException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * @see Exception#Exception()
	 */
	public JavaTestingFailedException() {
		super();
	}

	/**
	 * @see Exception#Exception(String, Throwable, boolean, boolean)
	 */
	protected JavaTestingFailedException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @see Exception#Exception(String, Throwable)
	 */
	public JavaTestingFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @see Exception#Exception(String)
	 */
	public JavaTestingFailedException(String message) {
		super(message);
	}

	/**
	 * @see Exception#Exception(Throwable)
	 */
	public JavaTestingFailedException(Throwable cause) {
		super(cause);
	}

}
