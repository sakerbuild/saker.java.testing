package saker.java.testing.impl.test;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.java.testing.api.test.JavaTestingOutput;

public class InternalJavaTestingOutput implements JavaTestingOutput, Externalizable {
	private static final long serialVersionUID = 1L;

	public InternalJavaTestingOutput() {
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}
}
