package ru.aladdin.jenkins.bhl

class AssemblyEnvironment {
    def steps

    AssemblyEnvironment(steps) {this.steps = steps}

    def BuildAssemblyEnvironment() {
        steps.stage('Test stage') {
            steps.node('master') {
                steps.println 'TEST STAGE'
            }
        }
    }
}


/*
stage("Prepare build environment") {
    node('master') {
        step([$class: 'WsCleanup'])
        checkout([
            $class:            'GitSCM',
            userRemoteConfigs: [[url:    repoURL,
                                         credentialsId: credRepo]],
            branches:          [[name:   "${env.BRANCH_NAME}"]]
        ])
        commitId = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
        // Для сборки докеров заполняется listBuildDockers,
        // а затем запускается их параллельная сборка
        def listBuildDockers = getListBuildDockers( dockerTags )
        parallel( listBuildDockers )
    }
}

*/