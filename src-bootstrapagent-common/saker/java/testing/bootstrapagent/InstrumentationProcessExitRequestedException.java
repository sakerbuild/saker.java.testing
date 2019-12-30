package saker.java.testing.bootstrapagent;

public class InstrumentationProcessExitRequestedException extends Throwable {
	private static final long serialVersionUID = 1L;

	private int resultCode;

	public InstrumentationProcessExitRequestedException(int resultcode) {
		super("JVM process exit requested with code: " + resultcode);
		this.resultCode = resultcode;
	}

	public int getResultCode() {
		return resultCode;
	}

}
