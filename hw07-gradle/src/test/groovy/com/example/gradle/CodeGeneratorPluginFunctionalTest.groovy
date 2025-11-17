package com.example.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

/**
 * Functional tests for the CodeGenerator plugin
 */
class CodeGeneratorPluginFunctionalTest extends Specification {

    @TempDir
    Path testProjectDir

    def "generated code can be compiled and used"() {
        given: "a project with generated code"
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
    className = 'FunctionalTestClass'
    packageName = 'com.example.functional'
    fieldValue = 'Functional Test Value 123'
}

dependencies {
    testImplementation 'junit:junit:4.13.2'
}
"""

        def mainDir = testProjectDir.resolve("src/main/java/com/example")
        Files.createDirectories(mainDir)

        def mainFile = mainDir.resolve("Main.java")
        mainFile.text = """
package com.example;

import com.example.functional.FunctionalTestClass;

public class Main {
    public static void main(String[] args) {
        FunctionalTestClass test = new FunctionalTestClass();
        System.out.println(test.getFormattedMessage());
    }
}
"""

        def testDir = testProjectDir.resolve("src/test/java/com/example")
        Files.createDirectories(testDir)

        def testFile = testDir.resolve("FunctionalTest.java")
        testFile.text = """
package com.example;

import org.junit.Test;
import com.example.functional.FunctionalTestClass;
import static org.junit.Assert.*;

public class FunctionalTest {
    @Test
    public void testGeneratedClass() {
        FunctionalTestClass test = new FunctionalTestClass();
        assertEquals("Functional Test Value 123", test.getValue());
        assertTrue(test.getFormattedMessage().contains("FunctionalTestClass"));
    }
}
"""

        when: "the project is built and tested"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments('build', 'test')
                .withPluginClasspath()
                .build()

        then: "all tasks succeed"
        result.task(':generateCode').outcome == TaskOutcome.SUCCESS
        result.task(':compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':test').outcome == TaskOutcome.SUCCESS
        result.task(':build').outcome == TaskOutcome.SUCCESS
    }

    def "plugin respects custom configuration"() {
        given: "a project with custom plugin configuration"
        def buildFile = testProjectDir.resolve("build.gradle")
        buildFile.text = """
plugins {
    id 'java'
    id 'com.example.codegenerator'
}

codeGenerator {
    className = 'CustomClass'
    packageName = 'org.custom.pkg'
    fieldValue = 'Custom configured value!'
    outputDirectory = file('src/generated/java')
}
"""

        when: "generateCode is executed"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments('generateCode')
                .withPluginClasspath()
                .build()

        then: "the task succeeds"
        result.task(':generateCode').outcome == TaskOutcome.SUCCESS

        and: "the file is generated in custom location"
        def customFile = testProjectDir.resolve("src/generated/java/org/custom/pkg/CustomClass.java")
        customFile.toFile().exists()

        and: "the content matches configuration"
        def content = customFile.toFile().text
        content.contains('package org.custom.pkg;')
        content.contains('public class CustomClass')
        content.contains('FIELD_VALUE = "Custom configured value!";')
    }

    def "clean removes generated sources"() {
        given: "a project with generated code"
        def buildFile = testProjectDir.resolve("build.gradle")
        buildFile.text = """
plugins {
    id 'java'
    id 'com.example.codegenerator'
}

codeGenerator {
    className = 'CleanTestClass'
    packageName = 'com.example.clean'
}
"""

        def buildDir = testProjectDir.resolve("build")

        when: "code is generated"
        def generateResult = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments('generateCode')
                .withPluginClasspath()
                .build()

        then: "generated file exists"
        generateResult.task(':generateCode').outcome == TaskOutcome.SUCCESS
        testProjectDir.resolve("build/generated/sources/main/java/com/example/clean/CleanTestClass.java")
                .toFile().exists()

        when: "clean is executed"
        def cleanResult = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments('clean')
                .withPluginClasspath()
                .build()

        then: "clean succeeds and generated sources are removed"
        cleanResult.task(':clean').outcome == TaskOutcome.SUCCESS
        !testProjectDir.resolve("build/generated/sources/main/java/com/example/clean/CleanTestClass.java")
                .toFile().exists()
    }
}