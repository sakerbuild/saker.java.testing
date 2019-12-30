package saker.java.testing.bootstrapagent;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;

public class InstrumentationData {
	public static final Object TESTING_INSTRUMENTATION_LOCK = new Object();

	private static final int CLASSES_INITIAL_SIZE = 1024 * 4;
	private static volatile boolean[] marker_inclusions = new boolean[CLASSES_INITIAL_SIZE];

	private static volatile int currentClassesSize = 0;

	private static volatile String[] referenced_classes = new String[InstrumentationData.CLASSES_INITIAL_SIZE];
	private static final Map<String, Integer> CLASSNAME_INDICES = new ConcurrentSkipListMap<>();

	public static void addUsedClass(Class<?> c) {
		if (c == null) {
			return;
		}
		addUsedClass(c.getName());
	}

	//takes the binary name as the argument
	public static void addUsedClass(String cname) {
		if (cname == null) {
			return;
		}
		cname = cname.replace('.', '/');
		int index = getClassIndex(cname);
		setUsedIndex(index);
	}

	public static void setUsedIndex(int idx) {
		boolean[] inclusions = marker_inclusions;
		while (true) {
			inclusions[idx] = true;
			boolean[] ninclusions = marker_inclusions;
			if (inclusions == ninclusions) {
				//the array wasn't changed between updates
				break;
			}
			//set the used index in the new array
			inclusions = ninclusions;
		}
	}

	//takes the / separated name of the class (internal name)
	public static int getClassIndex(String classname) {
		if (classname == null) {
			return -1;
		}
		Integer got = CLASSNAME_INDICES.get(classname);
		if (got != null) {
			return got;
		}
		synchronized (InstrumentationData.class) {
			int index = currentClassesSize++;
			String[] refclasses = referenced_classes;
			if (index >= refclasses.length) {
				int nlen = refclasses.length * 2;
				refclasses = Arrays.copyOf(refclasses, nlen);

				boolean[] prevmi = InstrumentationData.marker_inclusions;
				boolean[] newmi = new boolean[nlen];
				InstrumentationData.marker_inclusions = newmi;
				for (int i = 0; i < prevmi.length; i++) {
					//update the inclusions after setting the static field
					//this is in order to avoid concurrency issues
					if (prevmi[i]) {
						newmi[i] = true;
					}
				}
				referenced_classes = refclasses;
			}
			refclasses[index] = classname.replace('/', '.');
			CLASSNAME_INDICES.put(classname, index);
			return index;
		}
	}

	public static void resetClassLoggerInstrumentation() {
		boolean[] inclusions = InstrumentationData.marker_inclusions;
		int len = currentClassesSize;
		for (int i = 0; i < len; i++) {
			inclusions[i] = false;
		}
	}

	public static Set<String> getDependentClasses() {
		Set<String> result = new TreeSet<>();
		boolean[] inclusions = InstrumentationData.marker_inclusions;
		String[] classnames = referenced_classes;
		int len = currentClassesSize;
		for (int i = 0; i < len; i++) {
			if (inclusions[i]) {
				result.add(classnames[i]);
			}
		}
		return result;
	}

	public static void exitRequest(int exitcode) throws InstrumentationProcessExitRequestedException {
		throw new InstrumentationProcessExitRequestedException(exitcode);
	}

}
