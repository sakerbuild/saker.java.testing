package saker.java.testing.api.test.invoker;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Map;

import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;

/**
 * Immutable data class holding the parameters of a test invoker.
 * <p>
 * The class provides access to the string key-value pairs specified by the user.
 * 
 * @see JavaTestingInvoker#initTestRunner(ClassLoader, TestInvokerParameters)
 */
public final class TestInvokerParameters implements Externalizable {
	private static final long serialVersionUID = 1L;

	private Map<String, String> parameters;

	/**
	 * For {@link Externalizable}.
	 */
	public TestInvokerParameters() {
	}

	/**
	 * Creates a new instance with the specified parameters.
	 * 
	 * @param parameters
	 *            The parameters.
	 */
	public TestInvokerParameters(Map<String, String> parameters) {
		this.parameters = parameters == null ? Collections.emptyNavigableMap()
				: ImmutableUtils.makeImmutableNavigableMap(parameters);
	}

	/**
	 * Gets the test invoker parameters.
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
		SerialUtils.writeExternalMap(out, parameters);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		parameters = SerialUtils.readExternalImmutableNavigableMap(in);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (parameters != null ? "parameters=" + parameters : "") + "]";
	}
}