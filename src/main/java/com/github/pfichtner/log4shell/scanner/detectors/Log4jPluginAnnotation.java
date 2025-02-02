package com.github.pfichtner.log4shell.scanner.detectors;

import static com.github.pfichtner.log4shell.scanner.util.AsmTypeComparator.obfuscatorComparator;
import static com.github.pfichtner.log4shell.scanner.util.AsmTypeComparator.typeComparator;
import static com.github.pfichtner.log4shell.scanner.util.AsmUtil.isAnno;
import static com.github.pfichtner.log4shell.scanner.util.AsmUtil.nullSafety;
import static com.github.pfichtner.log4shell.scanner.util.AsmUtil.toMap;
import static com.github.pfichtner.log4shell.scanner.util.LookupConstants.PLUGIN_TYPE;
import static java.util.stream.Collectors.toList;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.github.pfichtner.log4shell.scanner.util.AsmTypeComparator;

/**
 * Searches for all classes that are annotated using log4j's
 * <code>@Plugin</code> annotation, where the annotation has
 * {@value #VALUE_NAME} for the attribute {@value #NAME_NAME} and
 * {@value #VALUE_CATEGORY} for the attribute {@value #NAME_CATEGORY}.
 */
public class Log4jPluginAnnotation extends AbstractDetector {

	private static final String NAME_NAME = "name";
	private static final String NAME_CATEGORY = "category";

	public static final String VALUE_NAME = "jndi";
	public static final String VALUE_CATEGORY = "Lookup";

	private static final Map<Object, Object> expectedAnnoContent = toMap(
			Arrays.asList(NAME_NAME, VALUE_NAME, NAME_CATEGORY, VALUE_CATEGORY));

	@Override
	public void visitClass(Path filename, ClassNode classNode) {
		if (hasPluginAnnotation(classNode)) {
			addDetection(filename, classNode,
					String.format("@Plugin(name = \"%s\", category = \"%s\")", VALUE_NAME, VALUE_CATEGORY));
		}
	}

	private static boolean hasPluginAnnotation(ClassNode classNode) {
		AsmTypeComparator typeComparator = typeComparator();
		return (obfuscatorComparator.equals(typeComparator()) && isAnno(classNode) && couldBePluginAnno(classNode))
				|| (hasPluginAnnotation(classNode, n -> typeComparator.isClass(Type.getType(n.desc), PLUGIN_TYPE)));
	}

	private static boolean hasPluginAnnotation(ClassNode classNode, Predicate<AnnotationNode> predicate) {
		return nullSafety(classNode.visibleAnnotations).stream()
				.anyMatch(predicate.and(n -> typeComparator().annotationIs(n, expectedAnnoContent)));
	}

	// ----------------------------------------------------------------------------------------------------------

	private static boolean couldBePluginAnno(ClassNode classNode) {
		if (!hasRetentionPolicy(classNode, "RUNTIME")) {
			return false;
		}
		// String name(), String category(), String elementType() default EMPTY
		List<MethodNode> methodsThatReturnsStrings = classNode.methods.stream()
				.filter(n -> n.desc.equals("()Ljava/lang/String;")).collect(toList());
		return methodsThatReturnsStrings.size() == 3 //
				&& methodsWithEmptyStringAsDefault(methodsThatReturnsStrings) == 1;
	}

	private static long methodsWithEmptyStringAsDefault(List<MethodNode> methods) {
		return methods.stream().filter(n -> "".equals(n.annotationDefault)).count();
	}

	private static boolean hasRetentionPolicy(ClassNode classNode, String policy) {
		return nullSafety(classNode.visibleAnnotations).stream()
				.filter(a -> a.desc.equals("Ljava/lang/annotation/Retention;"))
				.anyMatch(an -> an.desc.equals("Ljava/lang/annotation/Retention;") && an.values.size() == 2
						&& an.values.get(0).equals("value") && an.values.get(1) instanceof String[]
						&& Arrays.equals((String[]) an.values.get(1),
								new String[] { "Ljava/lang/annotation/RetentionPolicy;", policy }));
	}

}
