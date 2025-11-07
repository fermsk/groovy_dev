package com.example.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.file.DirectoryProperty
import javax.inject.Inject

/**
 * Extension for configuring the code generator plugin.
 */
abstract class CodeGeneratorExtension {

    /**
     * The value of the field to be generated in the class
     */
    abstract Property<String> getFieldValue()

    /**
     * The name of the class to generate
     */
    abstract Property<String> getClassName()

    /**
     * The package name for the generated class
     */
    abstract Property<String> getPackageName()

    /**
     * The root directory for code generation
     */
    abstract DirectoryProperty getOutputDirectory()

    @Inject
    CodeGeneratorExtension(ObjectFactory objects) {
        fieldValue.convention("Hello, Generated World!")
        className.convention("GeneratedClass")
        packageName.convention("com.example.generated")
        outputDirectory.convention(objects.directoryProperty().fileValue(
                objects.projectLayout().buildDirectory().dir("generated/sources/main/java").get().asFile
        ))
    }
}