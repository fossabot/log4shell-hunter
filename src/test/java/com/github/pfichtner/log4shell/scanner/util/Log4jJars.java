package com.github.pfichtner.log4shell.scanner.util;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class Log4jJars implements Iterable<File> {

	private final File dir;
	private final List<File> log4jJars;

	private static final Log4jJars instance = new Log4jJars();

	private Log4jJars() {
		try {
			this.dir = new File(getClass().getClassLoader().getResource("log4jars").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException();
		}
		this.log4jJars = Arrays.stream(dir.list()).map(f -> new File(dir, f)).collect(Collectors.toUnmodifiableList());
	}

	public static Log4jJars getInstance() {
		return instance;

	}

	public File getDir() {
		return dir;
	}

	public List<File> getLog4jJars() {
		return log4jJars;
	}

	public File version(String version) {
		String filename = "log4j-core-" + version + ".jar";
		return log4jJars.stream().filter(hasFilename(filename)).findFirst()
				.orElseThrow(() -> new NoSuchElementException(filename));
	}

	public List<File> versions(String... versions) {
		return Arrays.stream(versions).map(this::version).collect(toList());
	}

	private static Predicate<File> hasFilename(String filename) {
		return f -> f.getName().equals(filename);
	}

	@Override
	public Iterator<File> iterator() {
		// TODO use Spliterator
		return log4jJars.stream().collect(toList()).iterator();
	}

	public File[] getLog4jJarsWithout(List<File> ignore) {
		// do NOT use toArray(File[]::new) because this fails using java8 compiler
		List<File> list = Util.ignore(log4jJars, ignore);
		File[] result = new File[list.size()];
		for (int i = 0; i < list.size(); i++) {
			result[i] = list.get(i);
		}
		return result;
	}

}
