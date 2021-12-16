package com.github.pfichtner.log4shell.scanner;

import static com.github.pfichtner.log4shell.scanner.detectors.AsmUtil.isClass;
import static com.github.pfichtner.log4shell.scanner.detectors.AsmUtil.readClass;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.pfichtner.log4shell.scanner.CVEDetector.Detections.Detection;
import com.github.pfichtner.log4shell.scanner.io.Detector;
import com.github.pfichtner.log4shell.scanner.io.JarReader;
import com.github.pfichtner.log4shell.scanner.io.JarReader.JarReaderVisitor;

public class CVEDetector {

	private List<Detector<Detections>> visitors;

	public static class Detections {

		public static class Detection {

			private final Detector<?> detector;
			private final Path filename;
			private final Object object;

			public Detection(Detector<?> detector, Path filename, Object object) {
				this.detector = detector;
				this.filename = filename;
				this.object = object;
			}

			public String format() {
				return detector.format(this) + " found in class " + filename;
			}

			public Detector<?> getDetector() {
				return detector;
			}

			public Object getObject() {
				return object;
			}

		}

		private final List<Detection> detections = new ArrayList<>();

		public void add(Detector<?> detector, Path filename) {
			add(detector, filename, null);
		}

		public void add(Detector<?> detector, Path filename, Object object) {
			this.detections.add(new Detection(detector, filename, object));
		}

		public List<Detection> getDetections() {
			return detections;
		}

		public List<String> getFormatted() {
			return detections.stream().map(Detection::format).collect(toList());
		}

	}

	@SafeVarargs
	public CVEDetector(Detector<Detections>... visitors) {
		this(Arrays.asList(visitors));
	}

	public CVEDetector(List<Detector<Detections>> visitors) {
		this.visitors = unmodifiableList(new ArrayList<>(visitors));
	}

	public List<Detector<Detections>> getVisitors() {
		return visitors;
	}

	public void check(String jar) throws IOException {
		for (Detection detection : analyze(jar).getDetections()) {
			System.out.println(detection.format());
		}
	}

	public Detections analyze(String jar) throws IOException {
		Detections detections = new Detections();
		new JarReader(jar).accept(new JarReaderVisitor() {
			@Override
			public void visitFile(Path file, byte[] bytes) {
				for (Detector<Detections> visitor : visitors) {
					if (isClass(file)) {
						visitor.visitClass(detections, file, readClass(bytes, 0));
					} else {
						visitor.visitFile(detections, file, bytes);
					}
				}
			}
		});
		return detections;
	}

}
