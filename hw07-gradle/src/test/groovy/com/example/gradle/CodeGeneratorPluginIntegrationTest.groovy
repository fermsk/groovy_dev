package com.example.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

/**
 * Integration tests for the CodeGenerator plugin
 */
class CodeGeneratorPluginIntegrationTest extends Specification {

    @TempDir
    Path testProjectDir

    def "plugin can be applied successfully"() {
        given: "a minimal Gradle project"
        def buildFile = testProjectDir.resolve("build.gradle")
        buildFile.text = """
plugins {
    id 'java'
    id 'com.example.codegenerator'
}

codeGenerator {
    className = 'TestGeneratedClass'
    packageName = 'com.test.generated'
    fieldValue = 'Integration Test Value'
    outputDirectory = file("\${buildDir}/generated/sources/main/java")
}
"""

        when: "the build is executed"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments('tasks')
                .withPluginClasspath()
                .build()

        then: "the plugin is applied successfully"
        result.output.contains('generateCode')
        result.task(':tasks').outcome == TaskOutcome.SUCCESS
    }

    def "code generation task creates expected files"() {
        given: "a configured Gradle project"
        def buildFile = testProjectDir.resolve("build.gradle")
        buildFile.text = """
plugins {
    id 'java'
    id 'com.example.codegenerator'
}

repositories {
    mavenCentral()
}

codeGenerator {
    className = 'GeneratedTestClass'
    packageName = 'com.example.test'
    fieldValue = 'Test Value With "Quotes" and \\n new line'
}
"""

        when: "the generateCode task is executed"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments('generateCode')
                .withPluginClasspath()
                .build()

        then: "the task succeeds"
        result.task(':generateCode').outcome == TaskOutcome.SUCCESS

        and: "the generated file exists"
        def generatedFile = testProjectDir.resolve("build/generated/sources/main/java/com/example/test/GeneratedTestClass.java")
        generatedFile.toFile().exists()

        and: "the generated file contains expected content"
        def content = generatedFile.toFile().text
        content.contains('package com.example.test;')
        content.contains('public class GeneratedTestClass')
        content.contains('private static final String FIELD_VALUE = "Test Value With \\"Quotes\\" and \\n new line";')
        content.contains('public String getValue()')
        content.contains('public String getFormattedMessage()')
    }

    def "compileJava depends on generateCode"() {
        given: "a configured project"
        def buildFile = testProjectDir.resolve("build.gradle")
        buildFile.text = """
plugins {
    id 'java'
    id 'com.example.codegenerator'
}

repositories {
    mavenCentral()
}

codeGenerator {
    className = 'DependencyTestClass'
    packageName = 'com.example.dep'
    fieldValue = 'Dependency Test'
}
"""

        when: "compileJava is executed"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments('compileJava')
                .withPluginClasspath()
                .build()

        then: "both tasks succeed and generateCode runs first"
        result.task(':generateCode').outcome == TaskOutcome.SUCCESS
        result.task(':compileJava').outcome == TaskOutcome.SUCCESS
    }
}