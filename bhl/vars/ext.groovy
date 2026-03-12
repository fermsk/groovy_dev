import groovy.json.JsonOutput

def yara(repoURL, commitId, credRepo, tarOptions='', i=0) {
    if ( env.BUILD_ONLY == 'true' ) return;
    try {
        node('master') {
            stage("Prepare source yara") {
                cleanWs()
                checkout([
                    $class: 'GitSCM',
                    userRemoteConfigs:  [[url:           repoURL,
                                      credentialsId: credRepo]],
                    branches:           [[name:          "${commitId}"]],
                    extensions:         [[$class:        'CloneOption', timeout: 60],
                                         [$class:        'SubmoduleOption', parentCredentials: true, recursiveSubmodules: true, trackingSubmodules: true, timeout: 60]]
                ])
                sh "tar -czf source.tar.gz ${tarOptions} --exclude .git --exclude source.tar.gz *"
            }
            def handle = null
            withCredentials([usernamePassword( credentialsId: '24886496-af2c-4fec-bffc-4d685fc5e7ec', usernameVariable: 'USER_CCNET', passwordVariable: 'PASSWORD_CCNET')]) {
                stage("YARA analyze") {
                    def sourceURL = "${addons.getWorkspaceURL()}source.tar.gz"
                    sourceURL = sourceURL.replace('https://', "https://${env.USER_CCNET}:${env.PASSWORD_CCNET}@")
                    handle = triggerRemoteJob ( 
                        job: 'https://git-srv.aladdin.ru/jenkins/job/SDL/job/SDL/job/sdl1%252FYaraAnalyse/job/master/',
                        auth: CredentialsAuth(credentials: '24886496-af2c-4fec-bffc-4d685fc5e7ec'), 
                        parameters: "File url=" + sourceURL + "",
                        blockBuildUntilComplete: true,
                        quietDownstream: true
                    )
                }
                if  (handle.getBuildResult().toString() == 'SUCCESS') {
                    stage('Archive yara artifacts') {
                        sh 'mkdir -p "SDL/Static analysis/Source_check"'
                        sh "curl -fSLo yara.zip https://${env.USER_CCNET}:${env.PASSWORD_CCNET}@git-srv.aladdin.ru/jenkins/job/SDL/job/SDL/job/sdl1%252FYaraAnalyse/job/master/${handle.getBuildNumber().toString()}/artifact/*zip*/archive.zip"
                        sh 'unzip -j yara.zip -d \"SDL/Static analysis/Source_check\"'
                        archiveArtifacts artifacts: 'SDL/Static analysis/Source_check/**', onlyIfSuccessful: false
                        cleanWs()
                    }
                }
            }
        }
    } catch (err) {
        i += 1
        if ( i < 5 ) {
            yara(repoURL, commitId, credRepo, tarOptions, i)
        } else {
            error('YARA analyze FAILED after 5 tries! Read Logs!')
            return
        }
    }
}

def yaraBinaries(labels, vers, linuxDistribs=false, i=0) {
    if ( env.BUILD_ONLY == 'true' ) return;
    try {
        node('master') {
            def handle
            withCredentials([usernamePassword( credentialsId: '24886496-af2c-4fec-bffc-4d685fc5e7ec', usernameVariable: 'USER_CCNET', passwordVariable: 'PASSWORD_CCNET')]) {
                stage("YARA analyze binaries") {
                    cleanWs()
                    labels.each { label ->
                        try {
                            unstash "${addons.getStrLabel(label)}-${vers}-bin"
                            sh "tar -xzf _binaries.tar.gz"
                            sh "rm -f _binaries.tar.gz"
                        } catch (err) {
                            echo "No stash from label ${addons.getStrLabel(label)}"
                        }
                    }
                    def binariesURL
                    if ( !linuxDistribs ) {
                        sh "tar -czf _binaries.tar.gz _binaries"
                        binariesURL = "${addons.getWorkspaceURL()}_binaries.tar.gz"
                        binariesURL = binariesURL.replace('https://', "https://${env.USER_CCNET}:${env.PASSWORD_CCNET}@")
                    } else {
                        def files = findFiles(glob: '**')
                        files.each { file ->
                            tempBinariesURL = "${addons.getWorkspaceURL()}${file.path}"
                            if ( binariesURL == "" ) {
                                binariesURL = tempBinariesURL.replace('https://', "https://${env.USER_CCNET}:${env.PASSWORD_CCNET}@")
                            } else {
                                binariesURL = binariesURL + ";" + tempBinariesURL.replace('https://', "https://${env.USER_CCNET}:${env.PASSWORD_CCNET}@")
                            }
                        }
                    }
                    handle = triggerRemoteJob ( 
                        job: 'https://git-srv.aladdin.ru/jenkins/job/SDL/job/SDL/job/sdl1%252FYaraAnalyse/job/master/',
                        auth: CredentialsAuth(credentials: '24886496-af2c-4fec-bffc-4d685fc5e7ec'), 
                        parameters: "File url=" + binariesURL + "",
                        blockBuildUntilComplete: true,
                        quietDownstream: true
                    )
                }
                if  (handle.getBuildResult().toString() == 'SUCCESS') {
                    stage('Archive yara binaries artifacts') {
                        sh 'mkdir -p "SDL/Static analysis/Bin_check"'
                        sh "curl -fSLo yara_bins.zip https://${env.USER_CCNET}:${env.PASSWORD_CCNET}@git-srv.aladdin.ru/jenkins/job/SDL/job/SDL/job/sdl1%252FYaraAnalyse/job/master/${handle.getBuildNumber().toString()}/artifact/*zip*/archive.zip"
                        sh 'unzip -j yara_bins.zip -d \"SDL/Static analysis/Bin_check\"'
                        archiveArtifacts artifacts: 'SDL/Static analysis/Bin_check/**', onlyIfSuccessful: false
                        cleanWs()
                    }
                }
            }
        }
    } catch (err) {
        i += 1
        if ( i < 5 ) {
            yaraBinaries(labels, vers, linuxDistribs, i)
        } else {
            error('YARA binaries analyze FAILED after 5 tries! Read Logs!')
            return
        }
    }
}

def binaryAnalyse(labels, vers, linuxDistribs=false, i=0) {
    return;
    if ( env.BUILD_ONLY == 'true' ) return;
    try {
        node('master') {
            def handle
            withCredentials([usernamePassword( credentialsId: '24886496-af2c-4fec-bffc-4d685fc5e7ec', usernameVariable: 'USER_CCNET', passwordVariable: 'PASSWORD_CCNET')]) {
                stage("Binary analyse") {
                    cleanWs()
                    labels.each { label ->
                        try {
                            unstash "${addons.getStrLabel(label)}-${vers}-bin"
                            sh "tar -xzf _binaries.tar.gz"
                            sh "rm -f _binaries.tar.gz"
                        } catch (err) {
                            echo "No stash from label ${addons.getStrLabel(label)}"
                        }
                    }
                    def binariesURL = ""
                    if ( !linuxDistribs ) {
                        sh "tar -czf _binaries.tar.gz _binaries"
                        binariesURL = "${addons.getWorkspaceURL()}_binaries.tar.gz"
                        binariesURL = binariesURL.replace('https://', "https://${env.USER_CCNET}:${env.PASSWORD_CCNET}@")
                    } else {
                        def files = findFiles(glob: '**')
                        files.each { file ->
                            temBinariesURL = "${addons.getWorkspaceURL()}${file.path}"
                            if ( binariesURL == "" ) {
                                binariesURL = temBinariesURL.replace('https://', "https://${env.USER_CCNET}:${env.PASSWORD_CCNET}@")
                            } else {
                                binariesURL = binariesURL + ";" + temBinariesURL.replace('https://', "https://${env.USER_CCNET}:${env.PASSWORD_CCNET}@")
                            }
                        }
                    }
                    handle = triggerRemoteJob ( 
                        job: 'https://git-srv.aladdin.ru/jenkins/job/SDL/job/SDL/job/sdl1%252FBinaryAnalyse/job/master/',
                        auth: CredentialsAuth(credentials: '24886496-af2c-4fec-bffc-4d685fc5e7ec'), 
                        parameters: "File url=" + binariesURL + "",
                        blockBuildUntilComplete: true,
                        quietDownstream: true
                    )
                }
                if  (handle.getBuildResult().toString() == 'SUCCESS') {
                    stage('Archive binary analyse artifacts') {
                        sh 'mkdir -p "SDL/Static analysis/Bin_check"'
                        sh "curl -fSLo binary.zip https://${env.USER_CCNET}:${env.PASSWORD_CCNET}@git-srv.aladdin.ru/jenkins/job/SDL/job/SDL/job/sdl1%252FBinaryAnalyse/job/master/${handle.getBuildNumber().toString()}/artifact/*zip*/archive.zip"
                        sh 'unzip -j binary.zip -d \"SDL/Static analysis/Bin_check\"'
                        archiveArtifacts artifacts: 'SDL/Static analysis/Bin_check/**', onlyIfSuccessful: false
                        cleanWs()
                    }
                }
            }
        }
    } catch (err) {
        i += 1
        if ( i < 5 ) {
            binaryAnalyse(labels, vers, linuxDistribs, i)
        } else {
            error('Binary analyze FAILED after 5 tries! Read Logs!')
            return
        }
    }
}

def sonarQube(repoURL, commitId, credRepo, projName, vers) {
    if ( env.BUILD_ONLY == 'true' ) return;
    podTemplate(containers: [
        containerTemplate(name: 'jnlp',      image: 'nexus.ext.aladdin-rd.ru:4000/devops/build-images/jenkins-base-build-image:jenkins-agent-latest-jdk8', args: '${computer.jnlpmac} ${computer.name}'),
        containerTemplate(name: 'sonarqube', image: 'nexus.ext.aladdin-rd.ru:4000/devops/utils/sonarsource/sonar-scanner-cli:latest', command: 'sleep', args: '99d')
    ]){
        node(POD_LABEL) {
            stage('SonarQube test') {
                cleanWs()
                checkout([
                    $class: 'GitSCM',
                    userRemoteConfigs:  [[url:           repoURL,
                                          credentialsId: credRepo]],
                    branches:           [[name:          "${commitId}"]],
                    extensions:         [[$class:        'CloneOption', timeout: 60],
                                         [$class:        'SubmoduleOption', parentCredentials: true, recursiveSubmodules: true, trackingSubmodules: true, timeout: 60]]
                ])
                container('sonarqube') {
                    withEnv(["SONAR_HOST_URL=http://sq.aladdin.ru", "SONAR_TOKEN=6963be9564dd68bb80b6b3281e21b9de2278eb71"]) {
                        sh "sonar-scanner -Dsonar.projectKey=${projName} -Dsonar.projectName=${projName} -Dsonar.projectVersion=${vers} -Dsonar.sources=`pwd`"
                    }
                }
            }    
        }
    }
}

def promotion(repoURL, gitProjectPath, vers, dirs) {
    if ( env.PROMOTE_BUILD != 'true' ) return;
    stage("Promotion") {
        withCredentials([usernamePassword(
            credentialsId: '2cddd379-9aae-4acb-914b-32b7df5e2b1f',
            passwordVariable: 'GIT_PASSWORD',
            usernameVariable: 'GIT_USERNAME')]) {
            
            def credRepoURL = repoURL.replaceAll('https://',"https://${GIT_USERNAME}:${GIT_PASSWORD}@")
            sh ("git config --global user.email \"${GIT_USERNAME}@aladdin.ru\"")
            sh ("git config --global user.name \"Cruise Control\"")
            sh ("git tag -a \"v${vers}\" -m \"Tag for version v${vers}\"")
            sh ("git push ${credRepoURL} --tags")
        }

        if (gitProjectPath.contains("AladdinECA")) {
            def projName = repoURL.tokenize('/').last().replaceAll('.git', '')
            for (locale in ['RU','KG']) {
                def buildDir = "build/${locale}"
                if (fileExists(buildDir)) {
                    echo "Копируем артефакты для ${locale}"
                    cifsPublisher(
                        publishers: [[
                            configName: 'TestArifacts',
                            transfers: [[
                                cleanRemote: false,
                                flatten: false,
                                makeEmptyDirs: false,
                                patternSeparator: '[, ]+',
                                remoteDirectory: "${gitProjectPath}/${locale}/${projName}/${vers}",
                                sourceFiles: "${buildDir}/**, SDL/**"
                            ]],
                            verbose: false
                        ]]
                    )
                } else {
                    echo "Пропускаем ${locale}, т.к. нет папки ${buildDir}"
                }
            }
        } else {
            for (dir in dirs) {
                if (fileExists(dir)) {
                    cifsPublisher(
                        publishers: [[
                            configName: 'TestArifacts',
                            transfers: [[
                                cleanRemote: false,
                                flatten: false,
                                makeEmptyDirs: false,
                                patternSeparator: '[, ]+',
                                remoteDirectory: "${gitProjectPath}/${vers}",
                                sourceFiles: "${dir}/**"
                            ]],
                            verbose: false
                        ]]
                    )
                } else {
                    echo "Пропускаем ${dir}, т.к. нет папки"
                }
            }
        }

        emailext(
            to: 's.ranchin@aladdin.ru',
            subject: "Artifacts ${JOB_NAME} promouted!!!",
            body: """
               Был выполнен промоушн ${JOB_NAME} !!!
               Ссылка на артефакты: \\\\aladdin.ru\\main\\R&D\\Artifacts\\Test\\${gitProjectPath}\\${vers}
               Сборочное задание: ${BUILD_URL}
            """,
            attachLog: false
        )
        addBadge(icon: 'success.gif', text: 'Promote success')
    }
}



def podRun ( dir_with_yaml,  fn, Object... args) {
    podTemplate(yaml: readTrusted( dir_with_yaml + addons.getStrLabel(args[0]) + '.yaml')) {
        node( POD_LABEL ) {
            container('build') {
                fn.call(args)
            }
        }
    }
}

def jaCoCo( repoURL, commitId, credRepo ) {
    if ( env.BUILD_ONLY == 'true' ) return;
    podTemplate(containers: [
        containerTemplate(name: 'maven', image: 'nexus.ext.aladdin-rd.ru:4000/devops/utils/maven:3.9.3-sapmachine-17', command: 'sleep', args: '99d'),
        containerTemplate(name: 'jnlp', image: 'nexus.ext.aladdin-rd.ru:4000/devops/build-images/jenkins-base-build-image:jenkins-agent-latest-jdk8')
    ]) {
        node(POD_LABEL) {
            stage ("Checkout for Code Coverage") {
                step([$class: 'WsCleanup'])
                checkout([
                    $class: 'GitSCM',
                    userRemoteConfigs: [[url: repoURL, credentialsId: credRepo ]],
                    branches:          [[name: commitId]],
                    extensions:        [[$class: 'CloneOption', timeout: 60], [$class: 'GitLFSPull']]
                ])
            }
            container('maven') {
                stage ('Configure pom.xml') {
                    def pomXml = readFile('pom.xml')
                    pomXml = pomXml.replaceAll('</project>',
                    '''
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>org.jacoco</groupId>
                                    <artifactId>jacoco-maven-plugin</artifactId>
                                    <version>0.8.7</version>
                                    <executions>
                                        <execution>
                                            <id>prepare-agent</id>
                                            <goals>
                                                <goal>prepare-agent</goal>
                                            </goals>
                                        </execution>
                                        <execution>
                                            <id>report</id>
                                            <phase>test</phase>
                                            <goals>
                                                <goal>report</goal>
                                            </goals>
                                        </execution>
                                    </executions>
                                </plugin>
                            </plugins>
                        </build>
                    </project>''')
                    writeFile file: 'pom.xml', text: pomXml
                }
                stage ('Build with test') {
                    sh "mvn clean test"
                }
                stage ('Archive artifacts CoCo') {
                    def coverageDir = "${env.WORKSPACE}/SDL/CodeCoverage"
                    sh "mkdir -p ${coverageDir}"
                    
                    def pomFiles = findFiles(glob: "**/pom.xml")

                    for (pomFile in pomFiles) {
                        def projectDir = pomFile.path.replace('/pom.xml','')
                        def projectName = "${projectDir.tokenize('/').last()}"
                        echo "${projectName}"
                        def jacocoDir = "${projectDir}/target/site/jacoco"

                        if (fileExists(jacocoDir)) {
                            sh "mkdir -p ${coverageDir}/${projectName}"
                            sh "cp -R ${jacocoDir}/* ${coverageDir}/${projectName}"
                        }
                    }
                    archiveArtifacts artifacts: 'SDL/CodeCoverage/**', onlyIfSuccessful: false
                }
                stage ("JaCoCo Reports") {
                    jacoco(reportBuildDir: '**/jacoco')
                }
            }
        }
    }
}


def aecaBackCoCo(repoURL, commitId, credRepo) {
    if ( env.BUILD_ONLY == 'true' ) return;
    podTemplate(containers: [
        containerTemplate(name: 'jnlp', image: 'nexus.ext.aladdin-rd.ru:4000/devops/build-images/jenkins-base-build-image:jenkins-agent-latest-jdk8')
    ]) {
        node(POD_LABEL) {
            def coco_job = null
            stage('Code Coverage') {
                coco_job = build (
                    job: '/DevOps/DevOps%2FSDL%2Fcodecoverage%2Faeca-2.0.0%2Faeca-ca/master',
                    wait: false,
                    parameters: [
                        string( name: 'UPSTREAM_GIT_URL',    value: repoURL ),
                        string( name: 'UPSTREAM_GIT_COMMIT', value: commitId ),
                        string( name: 'UPSTREAM_CREDENTIALS', value: credRepo )
                    ]
                )
            }
            /*stage('Archive CoCo artifacts') {
                cleanWs()
                copyArtifacts (
                    projectName: sastJobName,
                    selector: specific("${sast_job.number}")
                )
                archiveArtifacts artifacts: 'SDL/Static analysis/**', onlyIfSuccessful: false
            }*/
        }
    }
}

def addPromotionAlphaExt(script, options = [:]) {
    def opts = []
    def buildSteps = [
        $class: "SystemGroovy",
        source: [
            $class: "FileSystemScriptSource",
            scriptFile: '/var/lib/jenkins/scriptler/scripts/promotion_alpha.groovy'
        ]
    ]
    if (options["artifactSMBPath"] != null) {
        opts = options.get("artifactSMBPath")
        buildSteps << [bindings: "artifact_path=${opts}"]
    }
    script.promotions([[
        name: "Alpha",
        label: "master",
        conditions: [[
            $class        : "ParameterizedSelfPromotionCondition",
            evenIfUnstable: true,
            parameterName : 'PROMOTE_BUILD',
            parameterValue: 'true'
        ]],
            buildSteps: [buildSteps]
    ]]) { }
}


