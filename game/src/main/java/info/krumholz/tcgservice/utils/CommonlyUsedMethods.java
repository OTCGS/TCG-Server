package info.krumholz.tcgservice.utils;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;

public class CommonlyUsedMethods {

	public static List<String> methodsWithRequestMapping(Class<?> clazz) {
		List<String> list = new ArrayList<String>();
		for (Method method : clazz.getMethods()) {
			if (method.isAnnotationPresent(RequestMapping.class)) {
				RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
				if (requestMapping.value().length > 0) {
					list.add(requestMapping.value()[0]);
				}
			}
		}
		return list;
	}

	public static Path getPath(String... p) {
		String home = System.getenv("CONFIG");
		if (home == null) {
			String[] subarray = Arrays.copyOfRange(p, 1, p.length);
			return Paths.get(p[0], subarray);
		}
		return Paths.get(home, p);
	}

}
