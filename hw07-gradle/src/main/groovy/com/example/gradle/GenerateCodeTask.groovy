package com.example.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Internal

/**
 * Custom task that generates a Java class based on configuration parameters.
 */
abstract class GenerateCodeTask extends DefaultTask {

    @Input
    abstract DirectoryProperty getOutputDirectory()

    @Input
    abstract String getClassName()

    @Input
    abstract String getPackageName()

    @Input
    abstract String getFieldValue()

    @OutputDirectory
    DirectoryProperty getGeneratedDir() {
        return outputDirectory
    }

    @TaskAction
    void generate() {
        // Ensure output directory exists
        def outputDir = outputDirectory.get().asFile
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        // Create package directory structure
        def packageDir = new File(outputDir, packageName.replace('.', '/'))
        if (!packageDir.exists()) {
            packageDir.mkdirs()
        }

        // Generate the Java source file
        def sourceFile = new File(packageDir, "${className}.java")

        def timestamp = new Date().format('yyyy-MM-dd HH:mm:ss')

        def content = """
package ${packageName};

/**
 * Auto-generated class by Gradle Plugin
 * Generated at: ${timestamp}
 */
public class ${className} {
    
    private static final String FIELD_VALUE = "${escapeJavaString(fieldValue)}";
    
    /**
     * Returns the configured field value
     */
    public String getValue() {
        return FIELD_VALUE;
    }
    
    /**
     * Returns a formatted message
     */
    public String getFormattedMessage() {
        return "Instance of ${className}: " + getValue();
    }
}"""

        sourceFile.text = content.trim()

        logger.lifecycle("Generated class: ${className} in package ${packageName}")
        logger.lifecycle("Generated file location: ${sourceFile.absolutePath}")
    }

    /**
     * Escapes Java string literals
     */
    private String escapeJavaString(String input) {
        return input
                .replace('\\', '\\\\')
                .replace('"', '\\"')
                .replace('\n', '\\n')
                .replace('\t', '\\t')
                .replace('\r', '\\r')
    }
}