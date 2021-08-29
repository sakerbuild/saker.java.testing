package saker.java.testing.impl.test;

import saker.java.testing.impl.test.launching.RemoteJavaRMIProcess;

public class RemoteTester {
	private RemoteJavaRMIProcess rmiProcess;

	public RemoteTester(RemoteJavaRMIProcess rmiProcess) {
		this.rmiProcess = rmiProcess;
	}

	public RemoteJavaRMIProcess getRMIProcess() {
		return rmiProcess;
	}
}