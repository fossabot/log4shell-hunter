package com.github.pfichtner.log4shell.scanner.detectors;

import static com.github.pfichtner.log4shell.scanner.DetectionCollector.Detection.getFormatted;
import static com.github.pfichtner.log4shell.scanner.util.Util.analyse;
import static com.github.pfichtner.log4shell.scanner.util.Util.withDetections;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.github.pfichtner.log4shell.scanner.DetectionCollector;
import com.github.pfichtner.log4shell.scanner.util.Log4jJars;

public class IsJndiEnabledPropertyAccessTest {

	Log4jJars log4jJars = Log4jJars.getInstance();

	IsJndiEnabledPropertyAccess sut = new IsJndiEnabledPropertyAccess();

	@Test
	void propertyAccessWasIntroducedLog4J216() throws Exception {
		DetectionCollector detector = new DetectionCollector(sut);
		assertThat(getFormatted(detector.analyze(log4jJars.version("2.16.0").getAbsolutePath())))
				.hasOnlyOneElementSatisfying(s -> s.startsWith(
						"log4j2.enableJndi access found in class org.apache.logging.log4j.core.net.JndiManager"));
	}

	@Test
	void canDetectAccess() throws Exception {
		assertThat(withDetections(analyse(log4jJars, sut)))
				.containsOnlyKeys(log4jJars.versions("2.17.0", "2.16.0", "2.12.2", "2.12.3").toArray(File[]::new));
	}

}
