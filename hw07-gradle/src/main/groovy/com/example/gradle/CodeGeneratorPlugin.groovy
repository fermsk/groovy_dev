package com.example.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile

/**
 * Gradle plugin that generates Java code based on configuration.
 */
class CodeGeneratorPlugin implements Plugin<Project> {

    void apply(Project project) {
        // Create the extension
        def extension = project.extensions.create('codeGenerator', CodeGeneratorExtension, project.objects)

        // Register the code generation task
        def generateTask = project.tasks.register('generateCode', GenerateCodeTask) { task ->
            task.description = 'Generates Java code based on plugin configuration'
            task.group = 'build'

            // Configure task inputs from extension
            task.outputDirectory.set(extension.outputDirectory)
            task.className.set(extension.className)
            task.packageName.set(extension.packageName)
            task.fieldValue.set(extension.fieldValue)
        }

        // Configure source sets to include generated sources
        project.afterEvaluate {
            // Add generated sources directory to main source set
            project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).java {
                srcDir(extension.outputDirectory)
            }

            // Make compileJava task depend on generateCode task
            project.tasks.withType(JavaCompile) { compileTask ->
                if (compileTask.name == JavaCompile.COMPILE_JAVA_TASK_NAME) {
                    compileTask.dependsOn(generateTask)
                }
            }

            // Make sure the task runs before compile
            generateTask.configure {
                onlyIf { !project.gradle.startParameter.taskNames.contains('clean') }
            }
        }

        // Set up clean task to remove generated sources
        project.tasks.named('clean') { cleanTask ->
            cleanTask.doLast {
                def outputDir = extension.outputDirectory.get().asFile
                if (outputDir.exists()) {
                    outputDir.deleteDir()
                }
            }
        }

        project.logger.lifecycle("CodeGeneratorPlugin applied to ${project.name}")
    }
}