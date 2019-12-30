package testrunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.lang.annotation.*;

public class TestRunnerMain {
	public static void main(String[] args) throws Throwable {
		for (String cname : args) {
			Class<?> clazz = Class.forName(cname, false, Thread.currentThread().getContextClassLoader());
			Method method = clazz.getMethod("main", String[].class);
			method.invoke(null, (Object) new String[] {});
			System.exit(0);
		}
	}
}