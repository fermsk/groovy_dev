package ru.aladdin.jenkins.bhl
import com.nirima.jenkins.plugins.docker.DockerCloud

class AssemblyEnvironmentDocker {

    def steps
    def dockerTags
    def repoURL
    def credRepo
    def gitCommitSHA1

    def AssemblyEnvironmentDocker(steps, repoURL, credRepo, gitCommitSHA1) { 
        this.steps = steps
        this.repoURL = repoURL
        this.credRepo = credRepo
        this.gitCommitSHA1 = gitCommitSHA1
        dockerTags = [:]
    }
    
    def addDockerTag(tag, label, cloud, credentialsId) {
        dockerTags[label] = [
            tag:           tag,
            credentialsId: credentialsId,
            cloud:         cloud
        ]
    }
    
    def addDockerDescription(tag, projectName, targetName, labels, cloud, credentialsId) {
        tag = tag.toLowerCase()
        def nodeName = "${projectName.replaceAll('/','-')}-${targetName}".toLowerCase()
        addDockerTag(tag, targetName, cloud, credentialsId)
        dockerTags[targetName]['labels'] = labels
    }
    
    def addDockerDescriptionVMRM(projectName, targetName, labels, cloud, credentialsId = '879c6da5-0a0b-4a83-9044-09a124747a06') {
        def tag = "vmrm.aladdin.ru:4000/devops/build-images/${projectName}:${targetName}".toLowerCase()
        addDockerDescription(tag, projectName, targetName, labels, cloud, credentialsId)
    }
    
    def addDockerDescriptionEXT(projectName, targetName, labels, cloud, credentialsId = '17620d2f-6c59-4295-bbf6-d0ba685ecb26') {
        def tag = "nexus.ext.aladdin-rd.ru:4000/devops/build-images/${projectName}:${targetName}".toLowerCase()
        addDockerDescription(tag, projectName, targetName, labels, cloud, credentialsId)
    }
    
    @NonCPS
    def getDockercloudURI(cloud) {
        def cloudByName = DockerCloud.getCloudByName(cloud) 
        def dockerUri = cloudByName.dockerApi.dockerHost.uri
        return dockerUri
    }
    
    def getListBuildDockers(buildArgs = null) {
        def listBuildDockers = [:]
        def dockerFiles = steps.findFiles(glob: 'Dockerfiles/**')
        def print4 = steps.sh (returnStdout: true, script: "echo dockerFILES=${dockerFiles}").trim()
        dockerFiles.each { df ->
            def elements = df.path.split('/')

            assert elements.size() == 3            : 'Directory \'Dockerfiles\' structure is invalid.'
            assert elements[2] == 'Dockerfile'     : 'Directory \'Dockerfiles\' structure is invalid.'
            assert dockerTags[elements[1]] != null : 'Directory \'Dockerfiles\' structure is invalid.'

            def label = elements[1]
            def dockerUri = getDockercloudURI(dockerTags[label].cloud)
            listBuildDockers[label] = {
                def shadocker = ''
                def rv = steps.sh (returnStatus: true, script: "docker manifest inspect --insecure ${dockerTags[label].tag}")
                def shagit = steps.sh (returnStdout: true, script: "git ls-files -s ${steps.WORKSPACE}/Dockerfiles/${label}/Dockerfile | awk '{print \$2}'").trim()
                if ( rv == 0 ) {
                    try { steps.sh "docker -H ${dockerUri} pull ${dockerTags[label].tag}" } catch (e) {}
                    try { shadocker = steps.sh (returnStdout: true, script: "docker -H ${dockerUri} inspect -f {{.Config.Labels.HASHDOCKERFILE}} ${dockerTags[label].tag}").trim() }catch (e) {}
                }
                if ( ( rv != 0 ) || ( "${shagit}" != "${shadocker}" ) ) {
                    steps.sh "echo \"\nLABEL HASHDOCKERFILE=${shagit}\" >>  ${steps.WORKSPACE}/Dockerfiles/${label}/Dockerfile"
                    
                    if ( buildArgs == "security.insecure" ) {
                        steps.dir("Dockerfiles/${label}") {
                            steps.sh "docker buildx build --allow security.insecure --builder insecure-builder --load -t ${dockerTags[label].tag} ."
                        }
                        steps.sh "docker push ${dockerTags[label].tag}"
                    } else {
                        steps.step([
                            $class: 'DockerBuilderPublisher',
                            dockerFileDirectory: "Dockerfiles/${label}",
                            cloud:               dockerTags[label].cloud,
                            tagsString:          dockerTags[label].tag,
                            pushOnSuccess:       true,
                            pushCredentialsId:   dockerTags[label].credentialsId,
                            pull:                true,
                            noCache:             true,
                            buildArgsString:     buildArgs
                        ])
                    }
                }
            }
        }
        return listBuildDockers
    }
    
    def getDockerDescriptions() {
        return dockerTags
    }

    // Функция подготовки сборочного окружения
    // Сборка образов докер
    
    def buildDockers(BRANCH_NAME = null , CHANGE_ID = null, buildArgs = null) { 
        steps.node('k8s-linux1'){
            steps.stage("Prepare build environment") {
                steps.step([$class: 'WsCleanup'])
                    if ( "${BRANCH_NAME}" ==~ /^MR-.*/) {
                        steps.println "BUILD MR"
                        steps.checkout([
                                $class: 'GitSCM',
                                userRemoteConfigs:  [[url:          repoURL, credentialsId: credRepo, refspec      : '+refs/heads/*:refs/remotes/origin/* +refs/merge-requests/*/head:refs/remotes/origin/merge-requests/*']],
                                branches:           [[name:         "origin/merge-requests/${CHANGE_ID}" ]],
                                extensions:         [[$class:        'CloneOption', timeout: 60],
                                                    [$class:         'SubmoduleOption', parentCredentials: true,  recursiveSubmodules: true, timeout: 60]]
                        ])
                    } else {
                        steps.checkout([
                            $class:            'GitSCM',
                            userRemoteConfigs: [[url: repoURL, credentialsId: credRepo]],
                            branches:          [[name:   "${gitCommitSHA1}"]],
                            extensions:        [
                                                [$class: 'CloneOption',    timeout: 60],
                                                [$class: 'CheckoutOption', timeout: 60],
                                                [$class: 'GitLFSPull']
                                                ]
                        ]) 
                    }      
                // Для сборки докеров заполняется listBuildDockers,
                // а затем запускается их параллельная сборка
                def listBuildDockers = getListBuildDockers(buildArgs)
                steps.parallel( listBuildDockers )
            }
            steps.step([$class: 'WsCleanup'])
        }
    }

}
