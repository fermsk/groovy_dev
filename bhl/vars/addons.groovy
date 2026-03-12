import jenkins.model.Jenkins
import jenkins.scm.api.SCMRevisionAction
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode
import org.jenkinsci.plugins.workflow.graph.FlowNode
import org.jenkinsci.plugins.workflow.cps.CpsThread
import com.nirima.jenkins.plugins.docker.DockerCloud
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.transform.Synchronized
import hudson.plugins.git.util.BuildData

def retryWithDelay(int maxRetries = 5, int delaySeconds = 300, Closure action) {
    def retval = null
    def attemptNumber = 0
    retry(maxRetries) {
        if ( attemptNumber > 0) sleep(300)
        retval = action()
        attemptNumber++
    }
    return retval
}

def getDockerCloudIp(cloudName) {
    def cloudByName = DockerCloud.getCloudByName(cloudName) 
    def dockerUri = cloudByName.dockerApi.dockerHost.uri
    String dockerCloudIp = dockerUri.replace("tcp://","").replace(":2375","")
    return dockerCloudIp
}

def getGitlabSHA1Jenkinsfile(script) {
    def scmRevisionActions = script.currentBuild.rawBuild.getActions(SCMRevisionAction)

    if ( scmRevisionActions == null ) {
        return null
    }

    def scmRevisionAction = null
    scmRevisionActions.each { scmra ->
        scmRevisionAction = scmra
        return true
    }

    if ( scmRevisionAction == null ) {
        return null
    }

    return scmRevisionAction.revision.toString()
}

def getRepoUrl(script) {
    try {
        return script.currentBuild.rawBuild.parent.parent.sources[0].source.httpRemote;
    } catch(e) {
        println ('ERROR!!! Сan\'t get the repository url!')
        throw e
    }
}

def getTimestamp() {
    def now = new Date()
    return now.format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('UTC')) + " (UTC)"
}

def getStrLabel( l ) {
    def strLabel = l.os
    if ( l.debug ) {
        strLabel = "${strLabel}-debug"
    }
    strLabel = "${strLabel}-${l.arch}"
    return strLabel
}

def checkPromotionAlpha(def build) {
    def buildPromoted = false
    def actions = build.actions
    actions.each() {
        if ( it.class?.name == 'hudson.plugins.promoted_builds.PromotedBuildAction' ) {
            buildPromoted = it.getPromotion('Alpha')?.name != null
            return buildPromoted
        }
    }
    return buildPromoted
}

def getLastPromotedBuild(def jobName) {
    def job = jenkins.model.Jenkins.instance.getItemByFullName(jobName)
    def build = job.lastBuild
    while (build != null) {
        if (checkPromotionAlpha(build)) {
            return build
        } else {
            build = build.previousBuild
        }
    }
    return null
}

def getLastPromotionParams(def build) {
    def lastTesterId = ""
    def lastParentIssue = ""
    build.actions.each { buildAction ->
        if (buildAction.class?.name == 'hudson.plugins.promoted_builds.PromotedBuildAction') {
            def lastSuccsessfullPromotion = buildAction.getPromotion('Alpha')?.getLastSuccessful()
            lastSuccsessfullPromotion.actions.each { promotionAction ->
                if (promotionAction.class?.name == 'hudson.plugins.promoted_builds.Promotion$PromotionParametersAction') {
                    def parameters = promotionAction.getParameters()
                    parameters.each { parameter ->
                        if ( parameter.name == "REDMINE_PARENT_ISSUE" ) {
                            lastParentIssue = parameter.getValue().toString()
                        } else if ( parameter.name == "REDMINE_TESTER_ID" ) {
                            lastTesterId = parameter.getValue().toString()
                        }
                    }
                }
            }
        }
    }
    return [lastParentIssue, lastTesterId]
}

def addPromotionAlpha(script, options = [:]) {
    def build = getLastPromotedBuild(env.JOB_NAME)
    def opts = []
    if (options["additionalEmails"] != null) {
        opts << stringParam(name: 'SMTP_NOTIFICATION_ADD', defaultValue: 's.ranchin@aladdin-rd.ru' + ' ' + options["additionalEmails"] , description: 'SMTP recipients')
    } else {
        opts << stringParam(name: 'SMTP_NOTIFICATION_ADD', defaultValue: 's.ranchin@aladdin-rd.ru', description: 'SMTP recipients')
    }

    if (options["notCopyToSMBShare"] != null && options["notCopyToSMBShare"]) {
        opts << booleanParam(name: 'DO_NOT_COPY_TO_SMB_SHARE', defaultValue: 'true', description: 'Do not copy artifacts to smb share')
    }

    if (options["artifactRepo"] != null) {
        opts << stringParam(name: 'ARTIFACT_REPO', defaultValue: options["artifactRepo"], description: 'Artifacts Git repository')
    }

    if (options["artifactSMBPath"] != null) {
        opts << stringParam(name: 'PROJECT_ARTIFACT_SMB_PATH', defaultValue: options["artifactSMBPath"], description: 'Artifacts path relative \\\\ALADDIN.RU\\main\\S_RnD\\Artifacts\\Test')
    }

    if (options["redmineProjectID"] != null) {
        opts << stringParam(name: 'REDMINE_PROJECT_ID', defaultValue: options["redmineProjectID"], description: 'Redmine project identifier')
    }

    if (options["pushToAppStoreDir"] != null) {
        opts << stringParam(name: 'PUSH_TO_APPSTORE_DIR', defaultValue: options["pushToAppStoreDir"], description: 'AppStore deploy dir')
    }
    
    if (options["autotestJobs"] != null) {
        opts << stringParam(name: 'AUTOTEST_JOBS', defaultValue: options["autotestJobs"], description: 'Jobs for running AutoTest')
    }

    if (options["createRedmineBuildIssue"]) {
        opts << booleanParam(name: 'CREATE_BUILD_IN_REDMINE', defaultValue: options["createRedmineBuildIssue"], description: 'Создать сборку в Redmine')
        opts << stringParam(name: 'REDMINE_PARENT_ISSUE', defaultValue: getLastPromotionParams(build)[0], description: 'Родительская задача\\требование')
        opts << stringParam(name: 'REDMINE_TESTER_ID', defaultValue: getLastPromotionParams(build)[1], description: 'ID тестировщика')

        if (options["redmineBuildIssueName"]) {
            opts << stringParam(name: 'REDMINE_ISSUE_NAME', defaultValue: options["redmineBuildIssueName"], description: 'Наименование задачи')
        } 
        
        if (options["redmineBuildIssueDescription"]) {
            opts << textParam(name: 'REDMINE_ISSUE_DESCRIPTION', defaultValue: options["redmineBuildIssueDescription"], description: 'Описание задачи')
        } 
    }

    script.promotions([[
        name: "Alpha",
        label: "master",
        conditions: [[
            $class: "ManualCondition",
            parameterDefinitions: opts
        ]],
        buildSteps: [[
            $class: "SystemGroovy",
            source: [
                $class: "FileSystemScriptSource",
                scriptFile: '/var/lib/jenkins/scriptler/scripts/promotion.groovy'
            ]
        ]]
    ]]) { }
}

def addPromotionsAlphaAndPublish(script, options = [:]) {
    def opts = []
    if (options["additionalEmails"] != null) {
        opts << stringParam(name: 'SMTP_NOTIFICATION_ADD', defaultValue: 's.ranchin@aladdin-rd.ru' + ' ' + options["additionalEmails"] , description: 'SMTP recipients')
    } else {
        opts << stringParam(name: 'SMTP_NOTIFICATION_ADD', defaultValue: 's.ranchin@aladdin-rd.ru', description: 'SMTP recipients')
    }

    if (options["notCopyToSMBShare"] != null && options["notCopyToSMBShare"]) {
        opts << booleanParam(name: 'DO_NOT_COPY_TO_SMB_SHARE', defaultValue: 'true', description: 'Do not copy artifacts to smb share')
    }

    if (options["artifactRepo"] != null) {
        opts << stringParam(name: 'ARTIFACT_REPO', defaultValue: options["artifactRepo"], description: 'Artifacts Git repository')
    }

    if (options["artifactSMBPath"] != null) {
        opts << stringParam(name: 'PROJECT_ARTIFACT_SMB_PATH', defaultValue: options["artifactSMBPath"], description: 'Artifacts path relative \\\\ALADDIN.RU\\main\\S_RnD\\Artifacts\\Test')
    }

    if (options["redmineProjectID"] != null) {
        opts << stringParam(name: 'REDMINE_PROJECT_ID', defaultValue: options["redmineProjectID"], description: 'Redmine project identifier')
    }

    if (options["pushToAppStoreDir"] != null) {
        opts << stringParam(name: 'PUSH_TO_APPSTORE_DIR', defaultValue: options["pushToAppStoreDir"], description: 'AppStore deploy dir')
    }
    
    if (options["autotestJobs"] != null) {
        opts << stringParam(name: 'AUTOTEST_JOBS', defaultValue: options["autotestJobs"], description: 'Jobs for running AutoTest')
    }

    script.promotions([[
        name: "Alpha",
        label: "master",
        conditions: [[
            $class: "ManualCondition",
            parameterDefinitions: opts
        ]],
        buildSteps: [[
            $class: "SystemGroovy",
            source: [
                $class: "FileSystemScriptSource",
                scriptFile: '/var/lib/jenkins/scriptler/scripts/promotion.groovy'
            ]
        ]]
    ],[
        name: "Publish",
        label: "master",
        conditions: [[
            $class: "ManualCondition"
        ]],
        buildSteps: [[
            $class: "SystemGroovy",
            source: [
                $class: "FileSystemScriptSource",
                scriptFile: '/var/lib/jenkins/scriptler/scripts/publish.groovy'
            ]
        ]]
    ]]) { }
}

def getDockerImage(cloudName, projectName, targetName, remoteFs = '') {

    if (remoteFs == '') {
        if (cloudName == "docker-linux") { remoteFs = '/var/lib/jenkins'}
        if (cloudName == "docker-windows") { remoteFs = 'C:/ProgramData/Jenkins'}
    }

    def cloudByName = DockerCloud.getCloudByName(cloudName) 
    def dockerUri = cloudByName.dockerApi.dockerHost.uri

    def dockerImage = [
        dockerHost: dockerUri,
        image:      "vmrm.aladdin.ru:4000/devops/build-images/${projectName}:${targetName}".toLowerCase(),
        remoteFs:   remoteFs
    ]
    return dockerImage
}


def setDockerTimeout(script, tout = 36000) {
    def dockerStrategy = new com.nirima.jenkins.plugins.docker.strategy.DockerOnceRetentionStrategy(tout) // Workaround don't disconnect after 10 min
    Jenkins.instance.getNode("${script.env.NODE_NAME}").setRetentionStrategy(dockerStrategy)
}

def getRecursiveWorkspaceURL(fn) {
    if ( fn instanceof StepStartNode && fn.typeFunctionName == 'node' && fn.isActive() ) {
        return "${Jenkins.instance.rootUrl}${fn.url}ws/"
    } else {
        def pars = fn.parents
        for ( FlowNode fni: pars ) {
            return getRecursiveWorkspaceURL(fni)
        }
        return ""
    }
}

def getWorkspaceURL() {
    def fn = CpsThread.current().head.get()
    return getRecursiveWorkspaceURL(fn)
}

def getHostWithPortFromURL(URL) {
    if ( URL == null ) {
        return null
    } else {
        return URL.split('/')[2]
    }
}

def saveDockerContainerK8s( script, projectName, branch, label, version, credentialsId = '879c6da5-0a0b-4a83-9044-09a124747a06' ) {
    def hostIP          = "${NODE_IP}"
    def podName         = "${NODE_NAME}"
    def containerName   = "${POD_CONTAINER}"
    def nameDockerImage = projectName.toLowerCase()
    def dockerImageName = "vmrm.aladdin.ru:4000/devops/builds/${nameDockerImage}:${branch}-${label}-${version}"
    if ( isUnix() ) {
        sh "mkdir -p /tmp/jenkins/workspace"
        sh "cp -r ${WORKSPACE}/ /tmp/jenkins/workspace"
        script.node('master') {
            containerId = script.sh(script: "docker -H ${hostIP}  ps -aqf \"name=k8s_${containerName}_${podName}*\"", returnStdout: true).trim()
            script.sh "docker -H ${hostIP} commit ${containerId} ${dockerImageName}"
            withCredentials([usernamePassword(
                            credentialsId: credentialsId,
                            usernameVariable: 'USER_DOCKER_REGISTRY',
                            passwordVariable: 'PASSWORD_DOCKER_REGISTRY')]) {
                script.sh "docker -H ${hostIP} login -u \$USER_DOCKER_REGISTRY -p \$PASSWORD_DOCKER_REGISTRY vmrm.aladdin.ru:4000"
            }
            script.sh "docker -H ${hostIP} push ${dockerImageName}"
            script.sh "docker -H ${hostIP} logout"
            script.sh "docker -H ${hostIP} rmi ${dockerImageName}"
        }
    } else {
        bat "mkdir C:\\workspace"
        bat "xcopy /E /Y . C:\\workspace"
        script.node('master') {
            containerId = script.sh(script: "docker -H ${hostIP}  ps -aqf \"name=k8s_${containerName}_${podName}*\"", returnStdout: true).trim()
            script.sh "docker -H ${hostIP} container stop ${containerId}"
            script.sh "docker -H ${hostIP} commit ${containerId} ${dockerImageName}"
            withCredentials([usernamePassword(
                            credentialsId: credentialsId,
                            usernameVariable: 'USER_DOCKER_REGISTRY',
                            passwordVariable: 'PASSWORD_DOCKER_REGISTRY')]) {
                script.sh "docker -H ${hostIP} login -u \$USER_DOCKER_REGISTRY -p \$PASSWORD_DOCKER_REGISTRY vmrm.aladdin.ru:4000"
            }
            script.sh "docker -H ${hostIP} push ${dockerImageName}"
            script.sh "docker -H ${hostIP} logout"
            script.sh "docker -H ${hostIP} rmi ${dockerImageName}"
        }
    }
    return dockerImageName
}

def saveDockerContainer( script, projectName, branch, label, version, cloud, containerId, credentialsId = '879c6da5-0a0b-4a83-9044-09a124747a06' ) {
    def nameDockerImage = projectName.toLowerCase()
    def dockerImageName = "vmrm.aladdin.ru:4000/devops/builds/${nameDockerImage}:${branch}-${label}-${version}"
    script.node('master') {
        script.step([
            $class: 'DockerBuilderControl', option: [
                $class:      'DockerBuilderControlOptionStop',
                cloudName:   cloud,
                containerId: containerId,
                remove:      false ]
        ])
        def dockerUri = DockerCloud.getCloudByName(cloud).dockerApi.dockerHost.uri
        script.sh "docker -H ${dockerUri} commit ${containerId} ${dockerImageName}"
        withCredentials([usernamePassword(
                        credentialsId: credentialsId,
                        usernameVariable: 'USER_DOCKER_REGISTRY',
                        passwordVariable: 'PASSWORD_DOCKER_REGISTRY')]) {
            script.sh "docker -H ${dockerUri} login -u \$USER_DOCKER_REGISTRY -p \$PASSWORD_DOCKER_REGISTRY vmrm.aladdin.ru:4000"
        }
        script.sh "docker -H ${dockerUri} push ${dockerImageName}"
        script.sh "docker -H ${dockerUri} logout"
        script.sh "docker -H ${dockerUri} rmi ${dockerImageName}"
    }
    return dockerImageName
}

def saveBuildLog(script, buildUrl, credentialsId = '879c6da5-0a0b-4a83-9044-09a124747a06') {
    node("master") {
        if ( credentialsId == "EXT" ) {
            credentialsId = '2cddd379-9aae-4acb-914b-32b7df5e2b1f'
        }
        def ccnetCredentials  = credentialsId
        withCredentials([usernamePassword(
                        credentialsId: ccnetCredentials,
                        usernameVariable: 'USER_CCNET',
                        passwordVariable: 'PASSWORD_CCNET')]) {
            script.sh "curl -u \$USER_CCNET:\$PASSWORD_CCNET -fSLo build_log.txt  ${buildUrl}consoleText"
            script.sh "mkdir -p SDL/Build && cp build_log.txt SDL/Build"
            script.archiveArtifacts artifacts: 'SDL/**', onlyIfSuccessful: true
        }
    }
}

def installSquishCoco(script, squish_url="") {
    
    if (squish_url == "") {
        if (isUnix()) {
            squish_url = "http://vmrm.aladdin.ru/repository/distrib/codecoverage/linux/SquishCocoSetup_5.1.0_Linux_x86_64.run"
        } else {
            squish_url = "http://vmrm.aladdin.ru/repository/distrib/codecoverage/windows/squishcoco_5.1.0/SquishCocoSetup_5.1.0_Windows_x64.exe"
        }
    }

    if ( script.isUnix() ) {
        def rhel = null 
        try {
            rhel = script.sh(script: 'cat /etc/redhat-release', returnStdout: true).trim()
        } catch (e) {
            rhel = null
        }
        if (rhel == null ) {
            script.sh "apt update && apt install -y libsm6 libxext6 libxrandr2 libxrender-dev libglib2.0-0 libfreetype6 libfontconfig1" 
        } else {
            sh "yum install -y libSM libXext libXrandr libXrender glib2"
        }
        script.sh "curl -fSLo SquishCocoSetup.run ${squish_url}"
        script.sh "chmod a+x SquishCocoSetup.run"
        script.sh "./SquishCocoSetup.run --noexec --target /opt/1"
        script.sh "cd /opt/1 && su -c \"/opt/1/install_squishcoco.sh root /opt/SquishCoco\""
        script.sh "rm -rf SquishCocoSetup.run /opt/1/ "
        script.sh "/opt/SquishCoco/bin/cocolic  --license-server=10.0.1.83:49344"

    } else {
        script.bat "curl -fSLo SquishCocoSetup.exe ${squish_url}"
        script.bat 'start /wait SquishCocoSetup.exe /S'
        script.bat '"C:\\Program Files\\squishcoco\\cocolic.exe" --license-server=10.0.1.83:49344'
    }
}

def userFileInput(inputFileName) {
    while ( true ) {
        def inputFileSize = null
        try {
            def inputFile = input message: 'Upload signed file', parameters: [base64File(inputFileName)]
            writeFile(file: inputFileName, text: inputFile, encoding: "Base64")
        } catch(err) {
            currentBuild.result = 'FAILED'
            error('File input has been Failed!')
        }
        try {
            inputFileSize = sh returnStdout: true, script: "stat ${inputFileName} | grep Size: | awk \'{print\$2}\'"
        } catch(err) {
            currentBuild.result = 'FAILED'
            error('Input file not found!')
        }
        if ( inputFileSize.trim() != "0" ) {
            echo "Файл успешно загружен!"
            break
        } else {
            echo "Файл не загружен, попробуйте снова!"
            continue
        }
    }
}

def findAndReplaceInFile ( filePath, valueFrom, valueTo, encoding ) {
    def file = readFile(file: filePath, encoding: encoding)
    def newText = file.replaceAll ( valueFrom, valueTo )
    writeFile(file: filePath, text: newText, encoding: encoding)
}

def yara(repoURL, commitId, credRepo, tarOptions = '', remote = 'false') {
    if (env.BUILD_ONLY == 'true') return
    def attempt = 0
    retryWithDelay(){
        attempt++
        node('master') {

            stage("Prepare source yara - Attempt ${attempt}") {
                cleanWs()

                def submoduleTracking = remote == "true"
                checkout([
                    $class: 'GitSCM',
                    userRemoteConfigs: [[url: repoURL, credentialsId: credRepo]],
                    branches: [[name: commitId]],
                    extensions: [
                        [$class: 'CloneOption', timeout: 60],
                        [$class: 'SubmoduleOption', parentCredentials: true, recursiveSubmodules: true, trackingSubmodules: submoduleTracking, timeout: 60]
                    ]
                ])
                    
                sh "tar -czf source.tar.gz ${tarOptions} --exclude .git --exclude source.tar.gz *"
            }

            stage("YARA analyze - Attempt ${attempt}") {
                def sourceURL = "${getWorkspaceURL()}source.tar.gz"
                withCredentials([usernameColonPassword(credentialsId: '879c6da5-0a0b-4a83-9044-09a124747a06', variable: 'USERPASS')]) {
                    sourceURL = sourceURL.replace('http://', "http://${env.USERPASS}@")
                }

                def yaraJob = build(
                    job: 'SDL/SDL/sdl1%2FYaraAnalyse/master',
                    wait: true,
                    parameters: [string(name: 'File url', value: sourceURL)]
                )

                step([
                    $class: 'CopyArtifact',
                    projectName: "/${yaraJob.fullProjectName}",
                    selector: [$class: 'SpecificBuildSelector', buildNumber: yaraJob.number.toString()],
                    target: "SDL/Static analysis/Source_check"
                ])
            }

            stage("Archive yara artifacts - Attempt ${attempt}") {
                archiveArtifacts artifacts: 'SDL/Static analysis/Source_check/**', onlyIfSuccessful: false
                cleanWs()
            }
        }
    }
}

def sast(repoURL, commitId, branch, sastJobName) {
    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
        try {
            if ( env.BUILD_ONLY == 'true' ) return;
            def sast_job = null
            stage('SAST analyze') {
                    sast_job = build (
                    job: sastJobName,
                    wait: true,
                    parameters: [
                        string( name: 'UPSTREAM_GIT_URL',    value: repoURL ),
                        string( name: 'UPSTREAM_GIT_COMMIT', value: commitId ),
                        string( name: 'UPSTREAM_GIT_BRANCH', value: branch ),
                        string( name: 'UPSTREAM_VERSION',    value: env.VERSION )
                    ]
                )
            }
            stage('Archive svace artifacts') {
                podTemplate(containers: [
                    containerTemplate(name: 'jnlp', image: 'vmrm.aladdin.ru:4000/devops/build-images/jenkins-base-build-image:jenkins-agent-latest-jdk8', args: '${computer.jnlpmac} ${computer.name}')
                ]) {
                    node(POD_LABEL) {
                        cleanWs()
                        copyArtifacts (
                            projectName: sastJobName,
                            selector: specific("${sast_job.number}")
                        )
                        archiveArtifacts artifacts: 'SDL/Static analysis/**', onlyIfSuccessful: false
                        cleanWs()
                    }
                }
            }
        } catch (Exception err) {
            println err.message
            error('SAST analyze FAILED! Reads Logs!')
        }   
    } 
}

def yaraBinaries(labels, vers, linuxDistribs = false) {

    if (env.BUILD_ONLY == 'true') return

    def attempt = 0
    retryWithDelay(){
        attempt++
        node('master') {
            stage("YARA analyze binaries - Attempt ${attempt}") {

                cleanWs()

                labels.each { label ->
                    try {
                        unstash "${getStrLabel(label)}-${vers}-bin"
                        sh "tar -xzf _binaries.tar.gz"
                        sh "rm -f _binaries.tar.gz"
                    } catch (err) {
                        echo "No stash from label ${getStrLabel(label)}"
                    }
                }

                def binariesURL = ""
                withCredentials([usernameColonPassword(credentialsId: '879c6da5-0a0b-4a83-9044-09a124747a06', variable: 'USERPASS')]) {
                    if (!linuxDistribs) {
                        sh "tar -czf _binaries.tar.gz _binaries"
                        binariesURL = "${getWorkspaceURL()}_binaries.tar.gz"
                        binariesURL = binariesURL.replace('http://', "http://${env.USERPASS}@")
                    } else {
                        findFiles(glob: '**').each { file ->
                            def tempBinariesURL = "${getWorkspaceURL()}${file.path}".replace('http://', "http://${env.USERPASS}@")
                            binariesURL = binariesURL ? "${binariesURL};${tempBinariesURL}" : tempBinariesURL
                        }
                    }
                }

                def yaraBinariesJob = build(
                    job: 'SDL/SDL/sdl1%2FYaraAnalyse/master',
                    wait: true,
                    parameters: [string(name: 'File url', value: binariesURL)]
                )

                step([
                    $class: 'CopyArtifact',
                    projectName: "/${yaraBinariesJob.fullProjectName}",
                    selector: [$class: 'SpecificBuildSelector', buildNumber: yaraBinariesJob.number.toString()],
                    target: "SDL/Static analysis/Bin_check"
                ])
            }

            stage("Archive yara binaries artifacts - Attempt ${attempt}") {
                archiveArtifacts artifacts: 'SDL/Static analysis/Bin_check/**', onlyIfSuccessful: false
                cleanWs()
            }
        }
    }
}

def binaryAnalyse(labels, vers, linuxDistribs = false) {
    if (env.BUILD_ONLY == 'true') return
    def attempt = 0
    retryWithDelay(){
        attempt++
        node('master') {
            stage("Binary analyse - Attempt ${attempt}") {
                cleanWs()

                labels.each { label ->
                    try {
                        unstash "${getStrLabel(label)}-${vers}-bin"
                        sh "tar -xzf _binaries.tar.gz"
                        sh "rm -f _binaries.tar.gz"
                    } catch (err) {
                        echo "No stash from label ${getStrLabel(label)}"
                    }
                }

                def binariesURL = ""
                withCredentials([usernameColonPassword(credentialsId: '879c6da5-0a0b-4a83-9044-09a124747a06', variable: 'USERPASS')]) {
                    if (!linuxDistribs) {
                        sh "tar -czf _binaries.tar.gz _binaries"
                        binariesURL = "${getWorkspaceURL()}_binaries.tar.gz".replace('http://', "http://${env.USERPASS}@")
                    } else {
                        binariesURL = findFiles(glob: '**')
                            .collect { file -> "${getWorkspaceURL()}${file.path}".replace('http://', "http://${env.USERPASS}@") }
                            .join(';')
                    }
                }

                def binaryAnalyseJob = build(
                    job: 'SDL/SDL/sdl1%2FBinaryAnalyse/master',
                    wait: true,
                    parameters: [string(name: 'File url', value: binariesURL)]
                )

                step([
                    $class: 'CopyArtifact',
                    projectName: "/${binaryAnalyseJob.fullProjectName}",
                    selector: [$class: 'SpecificBuildSelector', buildNumber: binaryAnalyseJob.number.toString()],
                    target: "SDL/Static analysis/Bin_check"
                ])
            }

            stage("Archive binary analyse artifacts - Attempt ${attempt}") {
                archiveArtifacts artifacts: 'SDL/Static analysis/Bin_check/**', onlyIfSuccessful: false
                cleanWs()
            }
        }
    }
}

def prepareBinaiesForAnalyze (label, vers, dir, stashname='bin') {
    if ( env.BUILD_ONLY == 'true' ) return;
    if ( isUnix() ) {
        sh "mkdir -p _binaries"
        sh "cp -r ${dir}/* _binaries"
        sh "tar -czf _binaries.tar.gz _binaries"
        stash name: "${getStrLabel(label)}-${vers}-${stashname}", includes: "_binaries.tar.gz"
    } else {
        bat "mkdir _binaries"
        bat "xcopy /E /Y ${dir} _binaries"
        bat "tar -czf _binaries.tar.gz _binaries"
        stash name: "${getStrLabel(label)}-${vers}-${stashname}", includes: "_binaries.tar.gz"
    }
}

def prepareDistribsForAnalyze (label, vers, dir) {
    if ( env.BUILD_ONLY == 'true' ) return;
    if ( isUnix() ) {
        sh "mkdir -p _distribs"
        sh "cp -r ${dir}/* _distribs"
        sh "tar -czf _distribs.tar.gz _distribs"
        stash name: "${getStrLabel(label)}-${vers}-distr", includes: "_distribs.tar.gz"
    } else {
        bat "mkdir _distribs"
        bat "xcopy /E /Y ${dir} _distribs"
        bat "tar -czf _distribs.tar.gz _distribs"
        stash name: "${getStrLabel(label)}-${vers}-distr", includes: "_distribs.tar.gz"
    }
}


//test_on parameter: "RedOS 7.2", "RedOS 7.3", "Astra 1.6" (empty = windows)
def installChecks(labels, vers, fileNames, test_on='', i=0) {
    if ( env.BUILD_ONLY == 'true' ) return;
    try {
        node ("master") {
            stage("Install checks") {
                cleanWs()
                labels.each { label ->
                    try {
                        unstash "${getStrLabel(label)}-${vers}-distr"
                        sh "tar -xzf _distribs.tar.gz"
                        sh "rm -f _distribs.tar.gz"
                    } catch (err) {
                        echo "No stash from label ${getStrLabel(label)}"
                    }
                }
                def distrURL = ""
                withCredentials([usernameColonPassword(credentialsId: '879c6da5-0a0b-4a83-9044-09a124747a06', variable: 'USERPASS')]) {
                    if ( test_on == 'RedOS 7.2' || test_on == 'RedOS 7.3' ) {
                        fileNames.each { fileName -> 
                            def rpmFile = findFiles(glob: "**/*${fileName}*.rpm")
                            tempDistrURL = "${getWorkspaceURL()}${rpmFile[0].path}"
                            if ( distrURL == "" ) {
                                distrURL = tempDistrURL.replace('http://', "http://${env.USERPASS}@")
                            } else {
                                distrURL = distrURL + ";" + tempDistrURL.replace('http://', "http://${env.USERPASS}@")
                            }
                        }
                    } else if ( test_on == 'Astra 1.6' ) {
                        fileNames.each { fileName -> 
                            def debFile = findFiles(glob: "**/*${fileName}*.deb")
                            echo "${debFile[0].path}"
                            tempDistrURL = "${getWorkspaceURL()}${debFile[0].path}"
                            if ( distrURL == "" ) {
                                distrURL = tempDistrURL.replace('http://', "http://${env.USERPASS}@")
                            } else {
                                distrURL = distrURL + ";" + tempDistrURL.replace('http://', "http://${env.USERPASS}@")
                            }                       
                        }
                    } else {
                        def file = findFiles(glob: "**/${fileNames[0]}*.msi")
                        distrURL = "${getWorkspaceURL()}${file[0].path}"
                        distrURL = distrURL.replace('http://', "http://${env.USERPASS}@")
                    }
                }
                timeout(time: 60, unit: 'MINUTES') {
                    def installChecks_job = null
                    if (test_on == '') {    
                        installChecks_job = build(
                            job: 'SDL/SDL/sdl1%2FInstallChecks/master',
                            wait: true,
                            parameters: [
                                string( name: 'Msi url', value: distrURL )
                            ]
                        )
                        step ([$class: 'CopyArtifact',
                            projectName: "/${installChecks_job.fullProjectName}",
                            selector: [$class: 'SpecificBuildSelector', buildNumber: installChecks_job.number.toString() ],
                            target: "SDL/Dynamic analysis/${fileNames[0]}"
                        ])
                    } else {
                        installChecks_job = build(
                            job: 'SDL/SDL/sdl1%2FInstallChecks/master',
                            wait: true,
                            parameters: [
                                string( name: 'Msi url', value: distrURL ),
                                string( name: 'Test on', value: test_on )
                            ]
                        )
                        step ([$class: 'CopyArtifact',
                            projectName: "/${installChecks_job.fullProjectName}",
                            selector: [$class: 'SpecificBuildSelector', buildNumber: installChecks_job.number.toString() ],
                            target: "SDL/Dynamic analysis/${test_on}"
                        ])
                    }
                }
            }
            stage('Archive Install Checks artifacts') {
                archiveArtifacts artifacts: 'SDL/Dynamic analysis/**', onlyIfSuccessful: false
                cleanWs()
            }
        }
    } catch (err) {
        i += 1
        if ( i < 5 ) {
            installChecks(labels, vers, fileNames, test_on, i)
        } else {
            error('Install checks FAILED after 5 tries! Read Logs!')
            return
        }
    }
}

def debug_info(labels,vers) {
    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
        if ( env.BUILD_ONLY == 'true' ) return;  
        node('devops_tools') {
            stage("Debug analyse") {
                cleanWs()
                labels.each { label ->
                    try {
                        unstash "${getStrLabel(label)}-$vers-debug"
                        sh 'tar -xvzf _binaries.tar.gz'
                        sh 'rm -f _binaries.tar.gz'
                    } catch (err) {
                        echo "No stash from label ${getStrLabel(label)}"
                    }      
                }
                withCredentials([usernameColonPassword(credentialsId: '879c6da5-0a0b-4a83-9044-09a124747a06', variable: 'USERPASS')]) {
                    sh 'tar -C _binaries -cvzf _binaries.tar.gz .'
                    def binariesURL = "${getWorkspaceURL()}_binaries.tar.gz"
                    binariesURL = binariesURL.replace('http://', "http://${env.USERPASS}@")

                    def debug_job = build(
                        job: 'zDevops/Extract-debug-info/1.0.1',
                        wait: true,
                        parameters: [
                            string( name: 'File url', value: binariesURL )
                        ]
                    )
                    step ([$class: 'CopyArtifact',
                        projectName: "/${debug_job.fullProjectName}",
                        selector: [$class: 'SpecificBuildSelector', buildNumber: debug_job.number.toString() ],
                        target: "SDL/"
                    ])
                }
            }
            stage('Archive artifacts') {
                archiveArtifacts artifacts: 'SDL/Source_files/**', onlyIfSuccessful: false
                cleanWs()
            }
        }
    }
}

@NonCPS
def updateFileNumber(numerator) {
    def jenkinsRootDir = jenkins.model.Jenkins.instance.getRootDir()
    def numeratorsDir = new File(new File(jenkinsRootDir, "userContent"), "Numerators")
    if ( ! numeratorsDir.exists() ) {
        numeratorsDir.mkdirs()
    }
    def number = 0
    def numeratorFile = new File(numeratorsDir, numerator)
    if ( numeratorFile.exists() ) {
        numeratorFile.withReader { number = it.readLine() }
        number = number as Integer
    } else {
        numeratorFile.write "1\n"
        number = 1
    }
    def newNumber = number + 1
    numeratorFile.write "${newNumber}\n"
    return number
}

def setBuildNumberByNumerator(script, numerator) {
    def number = 0
    script.lock('Numerator') {
        number = updateFileNumber(numerator)
    }
    script.currentBuild.displayName = "#${number}"
    script.env.BUILD_NUMBER = number
    return number
}

def podRun ( dir_with_yaml,  fn, Object... args) {
    podTemplate(yaml: readTrusted( dir_with_yaml + getStrLabel(args[0]) + '.yaml')) {
        node( POD_LABEL ) {
            container('build') {
                fn.call(args)
            }
        }
    }
}

def runPod ( script, String yamlPod, groovy.lang.Closure fn, Object... args) {
    script.podTemplate(yaml: script.readTrusted( yamlPod )) {
        script.node( script.POD_LABEL ) {
            script.container('build') {
                fn.call(args)
            }
        }
    }
}

def runPod ( script, String yamlPodTemplate, String dockerImage, groovy.lang.Closure fn, Object... args) {
    def yamlPod = readYaml text: libraryResource( yamlPodTemplate )
    yamlPod.spec.containers.each {
        if ( it.name == 'build' ) it.image = dockerImage
    }
    script.podTemplate(yaml: writeYaml (data: yamlPod, returnText: true)) {
        script.node( script.POD_LABEL ) {
            script.container('build') {
                fn.call(args)
            }
        }
    }
}

def runPodLinux ( script, String dockerImage, groovy.lang.Closure fn, Object... args) {
    runPod ( script, 'ru/aladdin/jenkins/bhl/podTemplates/linux.yaml', dockerImage, fn, args)
}

def runPodWindows ( script, String dockerImage, groovy.lang.Closure fn, Object... args) {
    runPod ( script, 'ru/aladdin/jenkins/bhl/podTemplates/windows.yaml', dockerImage, fn, args)
}

def ProcmonStart(filename) {

    withEnv([
            "PSEXEC=c:\\Exe\\Sysinternals\\psexec.exe"
    ]) {
        withCredentials([usernamePassword(
                credentialsId: 'a207b744-489a-4aa9-a233-b416f9dff133',
                usernameVariable: 'USER',
                passwordVariable: 'PASS')]) {
            catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                bat "mkdir C:\\temp && curl -fSLo C:\\temp\\SysinternalsSuite.zip http://vmrm.aladdin.ru/repository/distrib/win_tools/SysinternalsSuite.zip"
                bat "mkdir C:\\Exe\\Sysinternals && tar -C C:\\Exe\\Sysinternals -xzf C:\\temp\\SysinternalsSuite.zip"
                bat "RD /S /Q C:\\temp"
                bat "%PSEXEC% -sd -u %USER% -p %PASS% \\\\${getDockerCloudIp('docker-windows')} c:\\exe\\procmon64.exe -accepteula -backingfile c:\\temp\\${filename}.pml -quiet"
            }
        }
    }
}

def ProcmonStop() {

    withEnv([
            "PSEXEC=c:\\Exe\\Sysinternals\\psexec.exe"
    ]) {
        withCredentials([usernamePassword(
                credentialsId: 'a207b744-489a-4aa9-a233-b416f9dff133',
                usernameVariable: 'USER',
                passwordVariable: 'PASS')]) {
            catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                bat "%PSEXEC% -si -u %USER% -p %PASS% \\\\${getDockerCloudIp('docker-windows')} cmd /c \"start /i /wait c:\\exe\\procmon64.exe -terminate -quiet\""
            }
        }
    }
}

def ProcmonToCSV (filename, outdir) {
    def wsp = "C:" + pwd().replace("/", "\\")
    def nodeIp = "${getDockerCloudIp('docker-windows')}"

    withEnv([
            "PSEXEC=c:\\Exe\\Sysinternals\\psexec.exe",
            "WORKSPACE=${wsp}",
            "fl=${filename}",
            "dir=${outdir}"
    ]) {
        withCredentials([usernamePassword(
                credentialsId: 'a207b744-489a-4aa9-a233-b416f9dff133',
                usernameVariable: 'USER',
                passwordVariable: 'PASS')]) {
            catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS') {
                bat "%PSEXEC% -si -u %USER% -p %PASS% \\\\${nodeIp} cmd /c \"start /i /wait c:\\exe\\procmon64.exe -accepteula -openlog c:\\temp\\${filename}.pml -saveas c:\\temp\\${filename}.csv -quiet\""
                bat '''
                 net use \\\\${nodeIp}\\c$ /user:%USER% %PASS%
                 mkdir %dir%
                 xcopy \\\\${nodeIp}\\c$\\temp\\%fl%.csv %dir%\\ /e
                 cd %dir% && tar -czf Procmon_%fl%.tar.gz %fl%.csv && del %fl%.csv
                 del \\\\${nodeIp}\\c$\\temp\\*.pml
                 net use /del \\\\${nodeIp}\\c$
                 '''
            }
        }
    }
}


def setSignVar () {
    def CODESIGN_SHA256 = 'A4B261BFDC29FEC598EF3A83443A1B22FB92950E' 
    def defaultSigntoolTimestamp = 'http://timestamp.digicert.com'
    //'http://rfc3161timestamp.globalsign.com/advanced'
    def SIGNTOOL_TIMESTAMP = env.SIGNTOOL_TIMESTAMP ?: defaultSigntoolTimestamp

    return [CODESIGN_SHA256, SIGNTOOL_TIMESTAMP]
}

def sendErrorMessageToEmail(err) {
    println err.message
    currentBuild.result = 'FAILURE'
    emailext(
        recipientProviders: [requestor()],
        subject: "Сборка ${JOB_NAME} провалилась.",
        body: """
            Результаты сборки:
            Ошибка: "${err.message}"
            URL: ${BUILD_URL}
        """,
        attachLog: false,
    )
}

def prepareSources(repoURL, credRepo, branch) {
    retryWithDelay(5, 30) {
        cleanWs()
        if (env.BRANCH_NAME ==~ /^MR-.*/) {
            echo "Build Merge Request."
            checkout([
                $class: 'GitSCM',
                userRemoteConfigs:  [[url: repoURL, credentialsId: credRepo, refspec: '+refs/heads/*:refs/remotes/origin/* +refs/merge-requests/*/head:refs/remotes/origin/merge-requests/*']],
                branches:           [[name: "origin/merge-requests/${env.CHANGE_ID}" ]],
                extensions:         [[$class: 'CloneOption', timeout: 60],
                                    [$class: 'CheckoutOption', timeout: 60],
                                    [$class: 'GitLFSPull'],
                                    [$class: 'SubmoduleOption', parentCredentials: true,  recursiveSubmodules: true, timeout: 60]]
                ])
        } else {
            checkout([
                $class: 'GitSCM',
                userRemoteConfigs:  [[url: repoURL, credentialsId: credRepo]],
                branches:           [[name: branch]],
                extensions:         [[$class: 'CloneOption', timeout: 60],
                                    [$class: 'CheckoutOption', timeout: 60],
                                    [$class: 'GitLFSPull'],
                                    [$class: 'SubmoduleOption', parentCredentials: true,  recursiveSubmodules: true, timeout: 60]]
            ])
        }
    }
}

def startTcpdump(script) {
    def OSId = ''
    try {
        OSId = script.sh(returnStdout: true, script: 'cat /etc/os-release | grep ID=alpine').trim()
    } catch (Exception e) {}
    if ( OSId == 'ID=alpine' ) {
        script.sh '''
            apk add tcpdump 2>&1 > /dev/null
        '''
    } else {
        script.sh '''
            apt-get update 2>&1 > /dev/null
            apt-get install -y tcpdump net-tools 2>&1 > /dev/null
            ifconfig eth0
            tcpdump -U -i eth0 -s 100 -w /tcpdump.pcap &
            sleep 5
        '''
    }
    script.sh '''
        ifconfig eth0
        tcpdump -U -i eth0 -s 100 -w /tcpdump.pcap &
        sleep 5
    '''
}

def stopTcpdump(script) {
    script.sh '''
        pid=$(ps -e | pgrep tcpdump)
        kill -2 $pid
        sleep 5
        cp /tcpdump.pcap tcpdump.pcap
    '''
    script.archiveArtifacts artifacts: 'tcpdump.pcap', onlyIfSuccessful: false
}

def callStageWithResourceRequest (buildStage) {
    if ( isUnix() ) {
        def isInfrastructureOverloaded = true           
        while (isInfrastructureOverloaded) {       
            def cpuQuery = 'https://mon.aladdin.ru:9090/api/v1/query?query=' + URLEncoder.encode('100 * (1 - avg by (instance) (rate(node_cpu_seconds_total{instance=\"10.0.1.231:9100\",mode=\"idle\"}[5m])))', 'UTF-8')
            response = httpRequest(
                url: cpuQuery,
                httpMode: 'GET',
                contentType: 'APPLICATION_JSON',
                ignoreSslErrors: true
            )
            output = readJSON(text: response.content)
            cpuOutput = output.data.result[0].value[1].toDouble()

            println "cpuOutput=${cpuOutput}"

            memQuery = 'https://mon.aladdin.ru:9090/api/v1/query?query=' + URLEncoder.encode('100 * (1 - ((avg_over_time(node_memory_MemFree_bytes{instance=\"10.0.1.231:9100\"}[5m]) + avg_over_time(node_memory_Cached_bytes{instance=\"10.0.1.231:9100\"}[5m]) + avg_over_time(node_memory_Buffers_bytes{instance=\"10.0.1.231:9100\"}[5m])) / avg_over_time(node_memory_MemTotal_bytes{instance=\"10.0.1.231:9100\"}[5m])))', 'UTF-8')
            response = httpRequest(
                url: memQuery,
                httpMode: 'GET',
                contentType: 'APPLICATION_JSON',
                ignoreSslErrors: true
            )
            output = readJSON(text: response.content)
            memOutput = output.data.result[0].value[1].toDouble()
            println "memOutput=${memOutput}"    
            // Проверка условий
            if (cpuOutput > 80 || memOutput > 80) {
                println "Загрузка по CPU и Memory превышает 80%, инфраструктура перегружена."
                sleep(time:5 ,unit:"MINUTES")
            } else {
                println "Загрузка по CPU и Memory не превышает 80%, инфраструктура свободна! Запуск стейджа"
                buildStage() // Вызываем переданный stage как функцию
                isInfrastructureOverloaded = false // Выход из цикла
            }
        }
    } else {
        buildStage() // Вызываем переданный stage как функцию   
    }
}

def getBuildGitSCM(promotedBuild) {

    // Если используется Pipeline Libraries в pipeline, необходимо исключить те SCM,
    // которые ссылаются на Pipeline Libraries, чтобы тэг устанавливался не в этих репозиториях, а 
    // только в репозитории с исходным кодом

    // Находим url всех библиотек и заполняем ими массив pipelineLibrariesURLs

    promotedBuild = promotedBuild.rawBuild

    def libsActions = promotedBuild.getActions(org.jenkinsci.plugins.workflow.libs.LibrariesAction)
    def pipelineLibrariesURLs = []
    if ( libsActions != null ) {
        libsActions.each { la ->
            def libs = la.libraries
            libs.each { l ->
                promotedBuild.checkouts.each { ch ->
                    if ( ch.workspace.find("workspace@libs/${l.name}") ) {
                        def scm = ch.scm
                        if ( scm instanceof hudson.plugins.git.GitSCM ) {
                            scm.userRemoteConfigs.each { urc ->
                                if ( !pipelineLibrariesURLs.contains(urc.url) ) {
                                    pipelineLibrariesURLs << urc.url
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    def bds = promotedBuild.getActions(BuildData)

    if ( bds == null ) {
        return null
    }

    def gitBuildData = null
    bds.each { bd -> 
        bd.remoteUrls.each { rURL ->
            if ( ( pipelineLibrariesURLs.isEmpty() ) || ( pipelineLibrariesURLs.any { it != rURL } ) ) {
                gitBuildData = bd
                return true
            }
        }
        if ( gitBuildData != null ) {
            return true
        }
    }

    if ( gitBuildData == null ) {
        return null
    }

    def gitRemoteURLs = gitBuildData.remoteUrls
    def gitRemoteURL = null
    if ( gitRemoteURLs != null ) {
        if ( !gitRemoteURLs.isEmpty() ) {
            gitRemoteURL = gitRemoteURLs[0]
        }
    }

    def branch = null
    def branches = gitBuildData.lastBuiltRevision.branches
    if ( !(branches.isEmpty()) ) {
        branch = branches[0].name
    }

    def sha1 = gitBuildData.lastBuiltRevision.sha1String
    return [ gitURL: gitRemoteURL,
             branch: branch,
             sha1:   sha1 ]
}


def collectSourcesChecksumsUfix(reportName) {
    if ( env.BUILD_ONLY == 'false' ) {
        stage("Collect sources checksums") {
            
            // install ufix
            if (fileExists('/usr/bin/ufix')) {
                echo "Утилита ufix установлена."
            } else {
                sh "curl http://vmrm.aladdin.ru:1080/distrib/checksumutils/fix-unix/1.0/Fix_unix-gost.iso.dir/ufix_linux/ufix_rus -o /usr/bin/ufix"
                sh "chmod a+x /usr/bin/ufix"
            }
            
            //collect sources checksums
            sh "find . -type d -name '.git' -prune -o -type f -print >> ../\"${reportName}\".lst"
            sh "ufix -e --alg s256 -E ../\"${reportName}\".lst \"${reportName}\".prj"
            sh "mkdir -p SDL/Checksums/Source"
            sh "ufix -h -E \"${reportName}\".prj SDL/Checksums/Source/\"${reportName}\".html"
            archiveArtifacts artifacts: 'SDL/Checksums/Source/**', onlyIfSuccessful: false
            sh "rm -rf SDL"
        }
    }
}

def collectBinaryChecksumsUfix(reportName, binDir, binExtensions) {
    if ( env.BUILD_ONLY == 'false' ) {
        stage("Collect distr checksums") {

            // install ufix
            if (fileExists('/usr/bin/ufix')) {
                echo "Утилита ufix установлена."
            } else {
                sh "curl http://vmrm.aladdin.ru:1080/distrib/checksumutils/fix-unix/1.0/Fix_unix-gost.iso.dir/ufix_linux/ufix_rus -o /usr/bin/ufix"
                sh "chmod a+x /usr/bin/ufix"
            }

            def maskString = binExtensions.collect { "-name \"$it\"" }.join(" -o ")
            println maskString
            sh "find ${binDir} -type f \\( $maskString \\) >> ../\"${reportName}\".lst"
            sh "ufix -e --alg s256 -E ../\"${reportName}\".lst \"${reportName}\".prj"
            sh "mkdir -p SDL/Checksums/Distr"
            sh "ufix -h -E \"${reportName}\".prj SDL/Checksums/Distr/\"${reportName}\".html"
            archiveArtifacts artifacts: 'SDL/Checksums/Distr/**', onlyIfSuccessful: false
        }
    }
}


def mapik(repoURL, commitId, credRepo) {
if ( env.BUILD_ONLY == 'true' ) return;
    podTemplate(containers: [
            containerTemplate(name: 'jnlp', image: 'vmrm.aladdin.ru:4000/devops/build-images/jenkins-base-build-image:devops_tools')
        ]) {
            node(POD_LABEL) {

            stage("Download source") {
                cleanWs()
                checkout([
                    $class: 'GitSCM',
                    userRemoteConfigs:  [[url:           repoURL,
                                        credentialsId: credRepo]],
                    branches:           [[name:          "${commitId}"]],
                    extensions:         [[$class:        'CloneOption', timeout: 60],
                                        [$class:        'SubmoduleOption', parentCredentials: true, recursiveSubmodules: true, trackingSubmodules: true, timeout: 60],
                                        [$class: 'RelativeTargetDirectory', relativeTargetDir: 'source']
                                        ]
                ])
            }
            stage("Install mapik") {
                sh "curl -fSLo /mapik.zip http://vmrm.aladdin.ru/repository/distrib/softwarequalitytools/mapik/%D0%9C%D0%90%D0%9F%D0%98%D0%9A-%D0%A4%D0%B0%D0%B7%D0%B01_%D0%94%D0%B8%D1%81%D1%82%D1%80%D0%B8%D0%B1%D1%83%D1%82%D0%B8%D0%B2_v1.1.zip"
                sh "unzip -d / /mapik.zip"
                sh "cp /dist/converter /usr/sbin/"
                sh "cp /dist/analyser /usr/sbin/"
                sh "chmod +x /usr/sbin/analyser"
                sh "chmod +x /usr/sbin/converter"
                sh "analyser -c /dist/"
            }
            stage('Analyze') {
                sh "ls -lah"
                def options = ["misra2004", "misra2012", "cwe", "sg"]
                options.each { option ->
                    dir ("SDL/Static analysis/MAPIK/${option}"){
                        sh "analyser -r ${option} -d ${WORKSPACE}/source"
                        sh "converter -html *.txt && mv report.html ${option}.html"
                        sh "rm -rf *.txt"
                    }
                    
                }
            }
            stage('Archive yara artifacts') {
                archiveArtifacts artifacts: 'SDL/Static analysis/MAPIK/**', onlyIfSuccessful: false
            }
        }
    }
}

def signAstraLinux(String signFiles, String straceLogPath = '') {
    //def astraVersion = sh(script: "cat /etc/astra_version | awk '{print \$2}'", returnStdout: true).trim()
    //println "Astra Linux ${astraVersion}" 
    //if (astraVersion != "1.5") {
        withEnv(['KEY_ID=A0486A46BF56D4088B0D14D204D37E70FC021263' ]){
            withCredentials([
                file(credentialsId: 'a1f263ea-e87c-4ad4-a072-c0b4b3f4686c', variable: 'GPG_FILE'), 
                file(credentialsId: '2574d4c1-994d-4890-8219-622ec8908c66', variable: 'PASS_FILE')
            ]){
                sh "gpg --batch --pinentry-mode=loopback --passphrase-file=\"$PASS_FILE\" --import \"$GPG_FILE\""
                sh "bsign -N -s --pgoptions \"--default-key=$KEY_ID --passphrase-file=$PASS_FILE\" ${signFiles}"
                if ( straceLogPath != '' ) {
                    sh "strace -s 4096 -ff -ttt -y -yy -f -e \"abbrev=none\" -o ${straceLogPath} bsign -w ${signFiles} || true" 
                } else {
                    sh "bsign -w ${signFiles} || true" 
                }
            }
        }
    //} else {
    //    echo "Подпись для Astra Linux 1.5"
    //}
}

def signRpmInDir(List<String> rpmDirs) {
    if (!rpmDirs || rpmDirs.isEmpty()) {
        error("Необходимо указать хотя бы одну директорию для подписи RPM.")
    }

    def gpgBin = "gpg"
    if (sh(script: "[ -f /etc/altlinux-release ] && echo true || echo false", returnStdout: true).trim() == "true") {
        gpgBin = "gpg2"
    }

    withCredentials([
       file(credentialsId: '2cc60b03-cee3-4dbc-b7e7-60fc30a41ba7', variable: 'GPG_FILE'),
       string(credentialsId: 'bf87d832-c3ed-4b5a-8d35-fdb775b6fe45', variable: 'PASS')
    ]) {
        sh "echo '%_signature gpg' > ~/.rpmmacros"
        sh "echo '%_gpg_name AO ALADDIN R.D. (RPM digital signature) <aladdin@aladdin.ru>' >> ~/.rpmmacros"
        sh "rm -rf ~/.gnupg"
        sh "${gpgBin} --list-keys"
        sh "echo 'allow-preset-passphrase' >> ~/.gnupg/gpg-agent.conf"
        sh "gpg-connect-agent reloadagent /bye"
        sh "${gpgBin} --batch --yes --pinentry-mode loopback --passphrase $PASS --import $GPG_FILE"
        def keygrip = sh(
            script: """
                ${gpgBin} --list-keys --with-keygrip | grep -m1 'Keygrip' | awk '{print \$3}'
            """,
            returnStdout: true
        ).trim()
        def gpgPresetPath
        if (sh(script: "[ -f /etc/astra_version ] && echo true || echo false", returnStdout: true).trim() == "true") {
            gpgPresetPath = '/usr/lib/gnupg2/gpg-preset-passphrase'
        } else if (sh(script: "[ -f /etc/altlinux-release ] && echo true || echo false", returnStdout: true).trim() == "true") {
            gpgPresetPath = '/usr/lib/gnupg/gpg-preset-passphrase'
            sh "echo '%__gpg /usr/bin/gpg2' >> ~/.rpmmacros"
        } else {
            gpgPresetPath = '/usr/libexec/gpg-preset-passphrase'
        }
        sh "${gpgPresetPath} --passphrase $PASS --preset ${keygrip}"
        for (rpmDir in rpmDirs) {
            sh "cd ${rpmDir} && rpmsign --resign *.rpm"
        }
    }
}

def runScriptWithSignWindows(String script, pipeLineScript = null) {
    def SignVar = setSignVar()
    withCredentials([certificate(
        credentialsId: '911d65cf-e23e-4a3a-8547-74f3618d6a8c',
        keystoreVariable: 'SigningKeyStore',
        passwordVariable: 'SigningStorePass')]) 
    {
        bat "powershell.exe -NoP -NonI -Command \"Import-PfxCertificate -FilePath '${env.SigningKeyStore}' -CertStoreLocation Cert:\\CurrentUser\\My  -Password (ConvertTo-SecureString -String \"${env.SigningStorePass}\" -AsPlainText -Force)\""
        withEnv([ 
            'CODESIGN_SHA256=' + SignVar[0],
            'SIGNTOOL_TIMESTAMP=' + SignVar[1]
        ]) {
            if ( pipeLineScript ) {
                pipeLineScript.bat "${script}"
            } else {
                bat "${script}"
            }
            bat 'powershell.exe -command \"Get-ChildItem -Path Cert:\\CurrentUser\\My | Remove-Item -Verbose\"'
        }
    }
}

def microsoftAttestation(String uploadArchive,String outputDir = "Attestation", String downloadArchive = "Signed.zip" ) {
    if ( env.MICROSOFT_ATTESTATION == 'false' ) return;
    
    if(isUnix()) {
        error("Данные действия предназначены для ОС Windows")
    }
    def uploadArchiveUrl = "${getWorkspaceURL()}${uploadArchive}"
    emailext(
        recipientProviders: [requestor()],
        subject: "Необходимо отправить файлы на аттестацию в Microsoft!!!.",
        body: """
            Необходимо отправить файлы на аттестацию в Microsoft, а затем прикрепить результат в задачу для продолжения сборки!\n
            1. Скачайте cab файл\\файлы:   ${uploadArchiveUrl}\n
            2. Если это zip-архив, то распакойте, затем загрузите файлы через личный кабинет партнера microsoft: https://partner.microsoft.com/en-us/dashboard/hardware/driver/New \n
            3. Дождитесь окончания обработки, скачайте результат. \n
            4. К сборке можно прикрепить только один архив. Если файлов несколько, то запакуйте полученные файлы в архив, например выполнив команду, находясь в директории с полученными от microsoft zip-файлами: tar.exe --exclude='Signed.zip' -acf ${downloadArchive} *.zip \n
            (в директории не должно находиться посторонних zip-файлов).\n
               Если архив изначально один, то всё равно выполните этот шаг.\n
            5. Прикрепите архив в задание для продолжения сборки:  ${BUILD_URL}input/
            """,
        attachLog: false,
    )
    node ('master') {
        userFileInput("${downloadArchive}")
        stash name: "${env.JOB_BASE_NAME}-${uploadArchive}-signed", includes: "${downloadArchive}"
    }
    unstash "${env.JOB_BASE_NAME}-${uploadArchive}-signed"

    bat """
        mkdir ${outputDir}
        tar -C ${outputDir} -xvzf ${downloadArchive}
        del ${downloadArchive}
        cd ${outputDir}
        FOR %%Z IN ("*.zip") DO ( tar -xvzf "%%Z" --strip-components=1) || exit 1
        del *.zip
    """
}

def checksecAnalysis(String packagePath, String username, String password) {
    def checksecJob = build(
            job: 'SDL/SDL/sdl1%2FChecksecAnalyse/master',
            wait: true,
            parameters: [
                    string(name: 'CHECKSEC_PACKAGES', value: "http://${username}:${password}@${packagePath}"),
                    string(name: 'BRANCH_NAME', value: 'master')
            ]
    )

    step([
            $class: 'CopyArtifact',
            projectName: "/${checksecJob.fullProjectName}",
            selector: [$class: 'SpecificBuildSelector', buildNumber: checksecJob.number.toString()],
            target: "SDL/Static analysis/Security flags/ChecksecAnalyse"
    ])

    return checksecJob
}

def radare2Analysis(String packagePath, String username, String password) {
    def radare2Job = build(
            job: 'SDL/SDL/sdl1%2FRadare2Analyse/master',
            wait: true,
            parameters: [
                    string(name: 'R2_PACKAGES', value: "http://${username}:${password}@${packagePath}"),
                    string(name: 'BRANCH_NAME', value: 'master')
            ]
    )

    step([
            $class: 'CopyArtifact',
            projectName: "/${radare2Job.fullProjectName}",
            selector: [$class: 'SpecificBuildSelector', buildNumber: radare2Job.number.toString()],
            target: "SDL/Static analysis/Security flags/Radare2Analyse"
    ])

    return radare2Job
}
