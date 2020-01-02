/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package saker.java.testing.api.test.invoker;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;

/**
 * Immutable data class holding the parameter information for a single Java test case.
 * <p>
 * The class contains the test case class name and the stirng key-value pairs specified by the user.
 * 
 * @see JavaTestingInvoker#invokeTest(JavaTestingFileProvider, TestInvocationParameters)
 */
public final class TestInvocationParameters implements Externalizable {
	private static final long serialVersionUID = 1L;

	private String testClassName;
	private Map<String, String> parameters;

	/**
	 * For {@link Externalizable}.
	 */
	public TestInvocationParameters() {
	}

	/**
	 * Creates a new instance containing the specified test class name and no parameters.
	 * 
	 * @param testClassName
	 *            The test class name.
	 * @throws NullPointerException
	 *             If the test class name is <code>null</code>.
	 */
	public TestInvocationParameters(String testClassName) throws NullPointerException {
		Objects.requireNonNull(testClassName, "test class name");
		this.testClassName = testClassName;
		this.parameters = Collections.emptyNavigableMap();
	}

	/**
	 * Creates a new instance containing the specified test class name and parameters.
	 * 
	 * @param testClassName
	 *            The test class name.
	 * @param parameters
	 *            The parameters for the test case.
	 * @throws NullPointerException
	 *             If the test class name is <code>null</code>.
	 */
	public TestInvocationParameters(String testClassName, Map<String, String> parameters) throws NullPointerException {
		Objects.requireNonNull(testClassName, "test class name");
		this.testClassName = testClassName;
		this.parameters = parameters == null ? Collections.emptyNavigableMap()
				: ImmutableUtils.makeImmutableNavigableMap(parameters);
	}

	/**
	 * Gets the name of the test case that should be invoked.
	 * 
	 * @return The binary name of the test class.
	 * @see Class#getName()
	 */
	public String getTestClassName() {
		return testClassName;
	}

	/**
	 * Gets the test case parameters.
	 * 
	 * @return The parameters.
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	/**
	 * Gets the parameter for the given name.
	 * 
	 * @param key
	 *            The key.
	 * @return The associated parameter value or <code>null</code> if not present.
	 */
	public String get(String key) {
		if (key == null) {
			return null;
		}
		return parameters.get(key);
	}

	/**
	 * Gets the parameter for the given name or a default value if not present.
	 * 
	 * @param key
	 *            The key.
	 * @param defaultvalue
	 *            The default value.
	 * @return The associated parameter value or <code>defaultvalue</code> if not present.
	 */
	public String get(String key, String defaultvalue) {
		if (key == null) {
			return defaultvalue;
		}
		return parameters.getOrDefault(key, defaultvalue);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(testClassName);
		SerialUtils.writeExternalMap(out, parameters);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		testClassName = in.readUTF();
		parameters = SerialUtils.readExternalImmutableNavigableMap(in);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (testClassName != null ? "testClassName=" + testClassName + ", " : "")
				+ (parameters != null ? "parameters=" + parameters : "") + "]";
	}

}