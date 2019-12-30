package saker.java.testing.api.test.exc;

/**
 * Thrown by if the test runner failed to execute an operation.
 */
public class JavaTestRunnerFailureException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * @see Exception#Exception()
	 */
	public JavaTestRunnerFailureException() {
		super();
	}

	/**
	 * @see Exception#Exception(String, Throwable, boolean, boolean)
	 */
	protected JavaTestRunnerFailureException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	/**
	 * @see Exception#Exception(String, Throwable)
	 */
	public JavaTestRunnerFailureException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @see Exception#Exception(String)
	 */
	public JavaTestRunnerFailureException(String message) {
		super(message);
	}

	/**
	 * @see Exception#Exception(Throwable)
	 */
	public JavaTestRunnerFailureException(Throwable cause) {
		super(cause);
	}

}
