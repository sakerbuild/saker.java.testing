package testrunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.lang.annotation.*;

public class TestRunnerMain {
	public static void main(String[] args) throws Throwable {
		System.out.println("TestRunnerMain.main() " + Arrays.toString(args));
		for (String cname : args) {
			Class<?> clazz = Class.forName(cname, false, Thread.currentThread().getContextClassLoader());
			boolean hastest = false;
			boolean hassecond = false;
			for (Annotation a : clazz.getAnnotations()) {
				if (a.annotationType().getName().equals("test.TestAnnot")) {
					hastest = true;
				}
				if (a.annotationType().getName().equals("test.SecondAnnot")) {
					hassecond = true;
				}
			}
			if (hastest != hassecond) {
				throw new RuntimeException("Inconsistent annotations on: " + clazz.getName());
			}
			try {
				Method method = clazz.getMethod("main", String[].class);
				method.invoke(null, (Object) new String[] { "hello" });
			} catch (NoSuchMethodException e) {
			}
			System.exit(0);
//			throw new RuntimeException("TestRunnerMain.main() " + Arrays.toString(args));
		}
	}
}