import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def retryWithDelay(int maxRetries, int delaySeconds, Closure action) {
    def retval = null
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            retval = action()
            break
        } catch (Exception e) {
            if (attempt < maxRetries) {
                echo "Ошибка при выполнении команды. Попытка ${attempt} из ${maxRetries}. Ждём ${delaySeconds} секунд перед повтором."
                sleep delaySeconds
            } else {
                throw e
            }
        }
    }
    return retval
}

def installSvace(svace_url='', svacer_url='', svace_win_url='', svacer_win_url='') {
    
    if (env.JENKINS_URL == "https://jenkins.ext.aladdin-rd.ru/") {
        svace_url = svace_url ? svace_url : 'https://nexus.ext.aladdin-rd.ru/repository/distrib/softwarequalitytools/sast/svace/current/svace-x64-linux.tar.bz2'
        svacer_url = svacer_url ? svacer_url : 'https://nexus.ext.aladdin-rd.ru/repository/distrib/softwarequalitytools/sast/svacer/current/svacer-release.zip'
        svace_win_url = svace_win_url ? svace_win_url : ''
        svacer_win_url = svacer_win_url ? svacer_win_url : ''
    } else {
        svace_url = svace_url ? svace_url : 'http://vmrm.aladdin.ru/repository/distrib/softwarequalitytools/sast/svace/current/svace-x64-linux.tar.bz2'
        svacer_url = svacer_url ? svacer_url : 'http://vmrm.aladdin.ru/repository/distrib/softwarequalitytools/sast/svacer/current/svacer_release.zip'
        svace_win_url = svace_win_url ? svace_win_url : 'http://vmrm.aladdin.ru/repository/distrib/softwarequalitytools/sast/svace/current/svace-x64-windows.zip'
        svacer_win_url = svacer_win_url ? svacer_win_url : 'http://vmrm.aladdin.ru/repository/distrib/softwarequalitytools/sast/svacer/current/svacer_release.zip'
    }

    def svaceBins = [:]
    if ( isUnix() ) {
        // Determine Redhat system
        def rhel = null 
        try {
            rhel = sh(script: 'cat /etc/redhat-release', returnStdout: true).trim()
        } catch (e) {
            rhel = null
        }
        parallel([
            svace: {
                sh "curl -fSLo svace.tar.bz2 ${svace_url}"
                sh 'tar -xjf svace.tar.bz2'
            },
            svacer: {
                def svacer_url_ext = svacer_url.tokenize('.').last()
                if (svacer_url_ext == "zip") {
                    sh "curl -fSLo svacer.zip ${svacer_url}"
                    sh 'unzip svacer.zip'
                } else if (svacer_url_ext == "gz") {
                    sh "curl -fSLo svacer.tar.gz ${svacer_url}"
                    sh 'tar -xvf svacer.tar.gz'
                }
            }
        ])
        def svaceFile  = findFiles(glob: '**/bin/svace')
        if ( svaceFile.size() > 0 ) {
            svaceBins['svace'] = "${env.WORKSPACE}/${svaceFile[0].path}"
        }
        def svacerFile = findFiles(glob: '**/bin/svacer')
        if ( svacerFile.size() > 0 ) {
            svaceBins['svacer'] = "${env.WORKSPACE}/${svacerFile[0].path}"
        }
    } else {
        def unzip = null
        try {
            unzip = bat(script: 'where unzip', returnStdout: true).trim()
        } catch(e) {
            unzip = null
        }
        println "UNZIP ${unzip}"
        parallel([
            svace: {
                bat "curl -fSLo svace.zip ${svace_win_url}"
                if ( unzip == null ) {
                    bat 'tar -xzf svace.zip'
                } else {
                    bat 'unzip svace.zip'
                }
            },
            svacer: {
                bat "curl -fSLo svacer.zip ${svacer_win_url}"
                if ( unzip == null ) {
                    bat 'tar -xzf svacer.zip'
                } else {
                    bat 'unzip svacer.zip'
                }
            }
        ])
        def svaceFile  = findFiles(glob: '**/bin/svace.exe')
        if ( svaceFile.size() > 0 ) {
            svaceBins['svace'] = "${env.WORKSPACE}\\${svaceFile[0].path}"
        }
        def svacerFile = findFiles(glob: '**/bin/svacer.exe')
        if ( svacerFile.size() > 0 ) {
            svaceBins['svacer'] = "${env.WORKSPACE}\\${svacerFile[0].path}"
        }
    }
    return svaceBins
}

def installHasp() {
    def rhel 
    try {
        rhel = sh(script: 'cat /etc/redhat-release', returnStdout: true).trim()
    } catch (e) {
        rhel = null
    }
    if ( isUnix() ) {
        sh 'mkdir \"\$WORKSPACE/hasp\"'
        if (rhel == null ) {
            sh 'curl -fSLo \"\$WORKSPACE/hasp/Sentinel_LDK_Ubuntu_DEB_Run-time_Installer.tar.gz\" http://vmrm.aladdin.ru/repository/distrib/drivers/sentinel/hasp/svace-3.2.2/Sentinel_LDK_Ubuntu_DEB_Run-time_Installer.tar.gz'
            sh 'tar -C \"\$WORKSPACE/hasp\" -xzf \"\$WORKSPACE/hasp/Sentinel_LDK_Ubuntu_DEB_Run-time_Installer.tar.gz\"'
            sh 'dpkg -i \"\$WORKSPACE/hasp/Sentinel_LDK_Ubuntu_DEB_Run-time_Installer/aksusbd_8.31-1_amd64.deb\"'
            sh 'curl -fSLo /etc/hasplm/hasplm.ini http://gitlab.aladdin.ru/docker/docker-extra-scripts/raw/master/hasplm.ini'
            sh 'rm -rf \"\$WORKSPACE/hasp\"'
            sh 'bash /etc/init.d/aksusbd stop && exit 0'
            sh 'bash /etc/init.d/aksusbd start'
        } else {
            sh 'curl -fSLo \"\$WORKSPACE/hasp/Sentinel_LDK_RedHat_and_SuSE_RPM_Run-time_Installer.tar.gz\" http://vmrm.aladdin.ru/repository/distrib/drivers/sentinel/hasp/svace-3.2.2/Sentinel_LDK_RedHat_and_SuSE_RPM_Run-time_Installer.tar.gz'
            sh 'tar -C \"\$WORKSPACE/hasp\" -xzf \"\$WORKSPACE/hasp/Sentinel_LDK_RedHat_and_SuSE_RPM_Run-time_Installer.tar.gz\"'
            sh 'rpm -ihv \"\$WORKSPACE/hasp/Sentinel_LDK_RedHat_and_SuSE_RPM_Run-time_Installer/aksusbd-8.31-1.x86_64.rpm\"' 
            sh 'curl -fSLo /etc/hasplm/hasplm.ini http://gitlab.aladdin.ru/docker/docker-extra-scripts/raw/master/hasplm.ini'
            sh 'rm -rf \"\$WORKSPACE/hasp\"'
            // sh 'bash /etc/init.d/aksusbd stop && bash /etc/init.d/aksusbd start'
        }
    } else {
        //проверка контейнер или нет
        def String isDocker = bat(script: 'powershell -Command "(Get-Service -Name cexecsvc -ErrorAction SilentlyContinue)"', returnStdout: true).trim()
        def hasplms_run = bat returnStatus: true, script: 'sc query hasplms | find "RUNNING"'
        if ( hasplms_run != 0 ) { // Если HASPLM не запущен, устанавливаем и запускаем
            // если не контейнер то вычистить всё старое
            if (isDocker != null && isDocker != '') {
                try {
                    bat(script: 'net stop hasplms', returnStatus: true)
                } catch (Exception e) {
                    echo "Failed to stop service 'hasplms'. Ignoring error and continuing..."
                }
                if (fileExists("C:\\HASP")) {
                    bat "rd /q /s C:\\HASP"
                }
                if (fileExists("C:\\Program Files (x86)\\Common Files\\Aladdin Shared")) {
                    bat 'rd /q /s "C:\\Program Files (x86)\\Common Files\\Aladdin Shared"'
                }
                //удалить старые версии svace и svacer
            }

            // Add sentinel hasp binaries, config and create a service 
            bat "mkdir C:\\HASP"
            bat "curl -fSLo C:\\HASP\\Sentinel_HASP_windows_in_docker_svace.tar.gz http://vmrm.aladdin.ru/repository/distrib/drivers/sentinel/hasp/Sentinel_HASP_windows_in_docker_svace.tar.gz"
            bat "cd C:\\HASP && tar -xzf Sentinel_HASP_windows_in_docker_svace.tar.gz -C \"C:\\Program Files (x86)\\Common Files\""
            bat "curl -fSLo \"C:\\Program Files (x86)\\Common Files\\Aladdin Shared\\HASP\\hasplm.ini\" http://gitlab.aladdin.ru/docker/docker-extra-scripts/raw/master/hasplm.ini"
            bat "rd /q /s C:\\HASP"
            bat "\"C:\\Program Files (x86)\\Common Files\\Aladdin Shared\\HASP\\register_service.bat\""
        }
    }
}


def svaceInit ( warnings = [:] ) {
    def svaceBins = [:]
    if ( isUnix() ) {
        def svaceFile  = findFiles(glob: '**/bin/svace')
        if ( !fileExists(".svace-dir") ) {
            sh "${env.WORKSPACE}/${svaceFile[0].path} init"
        }
        for ( warning in warnings ) {
            sh "${env.WORKSPACE}/${svaceFile[0].path} warning ${warning.key} ${warning.value}"
        }
        // добавляем в file svace.ignore строку /usr/*
        def file = readFile(file: ".svace-dir/svace.ignore", encoding: 'utf8')
        if (!file.contains('/usr/.*')) {
           writeFile(file: ".svace-dir/svace.ignore", text: file +  "/usr/.*\n")  
        }
        def file1 = readFile(file: ".svace-dir/svace.ignore", encoding: 'utf8')
        if (!file1.contains('/opt/.*')) {
           writeFile(file: ".svace-dir/svace.ignore", text: file1 +  "/opt/.*\n")  
        }
        def file2 = readFile(file: ".svace-dir/svace.ignore", encoding: 'utf8')
        if (!file2.contains('/srv/.*')) {
           writeFile(file: ".svace-dir/svace.ignore", text: file2 +  "/srv/.*\n")  
        }
    } else {
        def svaceFile  = findFiles(glob: '**/bin/svace.exe')
        if ( !fileExists(".svace-dir") ) {
            bat "${env.WORKSPACE}\\${svaceFile[0].path} init"
        }
        for ( warning in warnings ) {
            bat "${env.WORKSPACE}\\${svaceFile[0].path} warning ${warning.key} ${warning.value}"
        }
    }
}

def exportSvaceData(svacerProject, label, svaceBin) {
    if(!isUnix()) {
        bat "\"${svaceBin}\" export-build --path ${label}-svace-build"
        bat "tar -czf ${label}-svace-build.tar.gz ${label}-svace-build"
    } else {
        sh "\"${svaceBin}\" export-build --path ${label}-svace-build"
        sh "tar -czf ${label}-svace-build.tar.gz ${label}-svace-build"
    }
    stash name: "${svacerProject}-${label}-svace-build", includes: "${label}-svace-build.tar.gz"
}

def importSvaceData(svacerProject, label, svaceBin, hashesFilename) {
    unstash "${svacerProject}-${label}-svace-build"
    if (isUnix()) {
        sh "tar -xzf ${label}-svace-build.tar.gz"
        sh "rm -rf ${label}-svace-build.tar.gz"
        sh "\"${svaceBin}\" history import-data --type build --path ${label}-svace-build | grep -oE '\\[BUILD\\].+' | sed 's/\\[BUILD\\]//' | tee -a ${hashesFilename}"
    } else {
        //скорее всего не понадобится
        echo "Глобальная функция importSvaceData пока что не поддерживает окружение  c ОС Windows, необходимо доработать!"
    }
}

def getMergedBuildHash(svaceBin, hashesFilename) {

    sh "grep . ${hashesFilename} | awk '{printf \"%s \", \$0}' > all_hashes.txt"
    sh "cat all_hashes.txt"

    def linesCount = sh(script: "grep . ${hashesFilename} | wc -l", returnStdout: true).trim().toInteger()

    if (linesCount > 1) {
        sh "cat all_hashes.txt | xargs \"${svaceBin}\" merge-build | grep Resulting | sed 's/Resulting build object: \\[BUILD\\]//' > full.txt"
        sh "cat full.txt"
        return sh(script: 'cat full.txt', returnStdout: true).trim()
    } else {
        return sh(script: 'cat all_hashes.txt', returnStdout: true).trim()
    }
}

def createSvaceServerGroup(svacerProject) {
    node("vmsvace") {
        try {
            sh "cd ~/server-svace; /opt/svace/bin/svace server admin create --project ${svacerProject} --group Projects"
        } catch (Exception err) {
            echo "Ошибка добавления проекта на сервер. Возможно проект ${svacerProject} уже существует!"
        }
    }
}

def svaceAnalyze(svacerProject, svaceBuildHash='', svaceBin='', branch=env.UPSTREAM_GIT_BRANCH, version=env.UPSTREAM_VERSION, vmsvaceAddress='10.0.1.149' )  {
    def ccnetCredentials = '879c6da5-0a0b-4a83-9044-09a124747a06'
    
    if (svaceBin == '')  {
        def svaceFile
        if (isUnix()) {
            svaceFile = findFiles(glob: '**/bin/svace')
        } else {
            svaceFile = findFiles(glob: '**/bin/svace.exe')
        }
        try {
            svaceBin = svaceFile[0].path
        } catch (Exception e) {
            error("Ошибка поиска svace bin, 3-м аргументом явно укажите путь к svace")
        }
    }
    
    String buildOption = ''
    if (svaceBuildHash) {
        buildOption = "--build ${svaceBuildHash}"
    }

    withCredentials([usernamePassword(credentialsId: ccnetCredentials, usernameVariable: "SVACE_USER", passwordVariable: "SVACE_PASSWD")]) {    
        if(isUnix()) {
            sh "\"${svaceBin}\" config ZIP_ANALYZE_RES true"
            retryWithDelay(5, 60) {
                sh("\"${svaceBin}\" remote --host ${vmsvaceAddress} --login ${env.SVACE_USER} --password ${env.SVACE_PASSWD} --path ${svacerProject} analyze ${buildOption} --enable-language all")
            }
        } else {
            bat "\"${svaceBin}\" config ZIP_ANALYZE_RES true"
            retryWithDelay(10, 60) {
                bat("\"${svaceBin}\" remote --host ${vmsvaceAddress} --login ${env.SVACE_USER} --password ${env.SVACE_PASSWD} --path ${svacerProject} analyze ${buildOption} --enable-language all")
            }
        }
    }
}


def importDataToSvacer(svacerProject, snapshotSvaceName, branch=env.UPSTREAM_GIT_BRANCH, version=env.UPSTREAM_VERSION, svacerBin='', svaceBin='', svacerAddress='vmsvacer.aladdin.ru') {
    def svacerCredentials = '324180ad-ceb3-4d12-855f-56d81e702ee7'
    if (svacerBin == '')  {
        def svacerFile
        if (isUnix()) {
            svacerFile = findFiles(glob: '**/bin/svacer')
        } else {
            svacerFile = findFiles(glob: '**/bin/svacer.exe')
        }
        svacerBin = svacerFile[0].path
    }
    
    if (svaceBin == '')  {
        def svaceFile
        if (isUnix()) {
            svaceFile = findFiles(glob: '**/bin/svace')
        } else {
            svaceFile = findFiles(glob: '**/bin/svace.exe')
        }
        svaceBin = svaceFile[0].path
    }

    withCredentials([usernamePassword(credentialsId: svacerCredentials, usernameVariable: 'SVACER_CREDS_USR', passwordVariable: 'SVACER_CREDS_PSW')]) {
        if (isUnix()) {
            retryWithDelay(5, 60) {
                sh("\"${svacerBin}\" import --project ${svacerProject} --branch ${env.UPSTREAM_GIT_BRANCH} --snapshot \"${snapshotSvaceName}\" --svace \"${svaceBin}\" \"${pwd()}\"")
            }
            retryWithDelay(5, 60) {
                sh("\"${svacerBin}\" upload --host ${svacerAddress} --port 443 --ssl --user ${env.SVACER_CREDS_USR} --password \"${env.SVACER_CREDS_PSW}\"")
            }
        } else {
            retryWithDelay(5, 60) {
                bat("\"${svacerBin}\" import --project ${svacerProject} --branch ${env.UPSTREAM_GIT_BRANCH} --snapshot \"${snapshotSvaceName}\" --svace \"${svaceBin}\" \"${pwd()}\"")
            }
            retryWithDelay(5, 60) {
                bat("\"${svacerBin}\" upload --host ${svacerAddress} --port 443 --ssl --user ${env.SVACER_CREDS_USR} --password \"${env.SVACER_CREDS_PSW}\"")
            }
        }
    }
}

def getSvacePublishData(projectName, projectBranch, projectSnapshot, outputFileName, outputFilePath, credentialsId, svacerUrl) {

    withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'USER_SVACER', passwordVariable: 'PASSWORD_SVACER')]) {

        if (svacerUrl == "https://vmsvacer.aladdin.ru/") {
            copyArtifacts (
                projectName: 'SDL/SDL/sdl1%2FSvacePublish/master',
                selector: lastSuccessful(),
                filter: "**",
                flatten: true,
                target: 'SvacePublishBin'
            )
        } else { 
            if ( isUnix() ){
                sh "mkdir -p SvacePublishBin"
                sh "curl -o SvacePublishBin/artifact.zip http://${env.USER_SVACER}:${env.PASSWORD_SVACER}@git-srv.aladdin.ru/jenkins/job/SDL/job/SDL/job/sdl1%252FSvacePublish/job/master/lastSuccessfulBuild/artifact/*zip*/archive.zip"
                sh "cd SvacePublishBin && unzip -j artifact.zip"
            } else {
                error "Windows проектов ещё не было для ext jenkins, если получили данное сообщение, то следует доработать ф-юю getSvacePublishData в bhl"
            }
        }

        if ( isUnix() ){
            sh "chmod a+x \"${env.WORKSPACE}/SvacePublishBin/svacepublish_x64\""
            retryWithDelay(5, 60) {
                sh "\"${env.WORKSPACE}/SvacePublishBin/svacepublish_x64\" --login \$USER_SVACER --password \$PASSWORD_SVACER --url ${svacerUrl} --project_name ${projectName} --project_branch ${projectBranch} --project_snapshot_id \"${projectSnapshot}\" --output_file_name \"${outputFilePath}/${outputFileName}\""
            }
        } else {
            retryWithDelay(5, 60) {
                bat "\"${env.WORKSPACE}\\SvacePublishBin\\svacepublish_x86.exe\" --login ${env.USER_SVACER} --password ${env.PASSWORD_SVACER} --url ${svacerUrl} --project_name ${projectName} --project_branch ${projectBranch} --project_snapshot_id \"${projectSnapshot}\" --output_file_name \"${outputFilePath}\\${outputFileName}\""
            }
        }
    }
}

def getSvacerPdf(projectName, projectBranch, projectSnapshot, outputFilePath, outputFileName, credentialsId, svacerUrl) {

    def svacerToken = null
    def response = null
    def idProject  = null
    def idBranch   = null
    def idShapshot = null

    response = retryWithDelay(10, 300) {
        httpRequest (
            url:"${svacerUrl}api/login?auth_type=ldap&server=ALADDIN.RU",
            authentication: credentialsId, 
            httpMode: 'POST', validResponseCodes: '200',
            ignoreSslErrors: true
        )
    }

    def output = readJSON(text: response.content)
    svacerToken = output.token
    assert svacerToken != null
    
    response = retryWithDelay(10, 300) {
        httpRequest (
            url:"${svacerUrl}api/public/projects" , 
            httpMode: 'GET', validResponseCodes: '200', 
            ignoreSslErrors: true,
            customHeaders: [[ name:'Authorization' ,value: "Bearer ${svacerToken}" ]]
        )
    }

    output = readJSON(text: response.content)
    output.each { it ->
        def proj = it["project"]
        if (proj["name"] == projectName){
            idProject = proj["id"]
            branches = it["branches"]
            branches.each {branch ->
                if (branch["name"] == projectBranch) {
                    idBranch = branch["id"]
                }
            }
        } 
    }

    assert idProject != null && idBranch != null
    
    response = retryWithDelay(10, 300) {
        httpRequest (
            url:"${svacerUrl}api/public/projects/${idProject}/branch/${idBranch}/snapshots" ,
            httpMode: 'GET',
            validResponseCodes: '200',
            ignoreSslErrors: true,
            customHeaders: [[ name:'Authorization' ,value: "Bearer ${svacerToken}" ]]
        )
    }

    output = readJSON(text: response.content)
    output.each { snapshot -> 
        if ( snapshot["name"] == projectSnapshot ) {
            idShapshot = snapshot["id"]
        }
    }

    def request = JsonOutput.toJson([compare_mode: "none", context: ['project': "${idProject}", 'branch' : "${idBranch}", 'snapshot' : "${idShapshot}"], language: "ru" , timezone: 180])
    
    echo "Request body: ${request}"
    response = retryWithDelay(10, 300) {
        httpRequest (
            url:"${svacerUrl}api/public/exportPDF" ,
            httpMode: 'POST', 
            validResponseCodes: '200',
            customHeaders: [[ name:'Authorization' ,value: "Bearer ${svacerToken}" ], [name: 'content-type', value:'application/json;charset=utf-8']],
            requestBody: request,
            ignoreSslErrors: true,
            outputFile:"${env.WORKSPACE}/${outputFilePath}/${outputFileName}"
        )
    }

    def svres = findFiles(glob: '**/analyze-res/*.svres')
    svres.each { 
        if ( isUnix() ) {
            sh "cp -v ${it} \"${outputFilePath}\""
        } else {
            bat "xcopy /E /Y ${it} \"${outputFilePath}\""
        }
    }
}

// Данный метод работает только с активированой опцией svace config ZIP_ANALYZE_RES true на клиенте Анализа
// Сервер svace после выполнения анализа передает в папку analyze-res архиф *.zip , в котором распологается нужный файл лога
def getSvaceServerAnalyzeLog (projectName, resultPath) {
    def logs = findFiles(glob: '**/analyze-res/*.zip')
    logs.each { 
        if ( isUnix() ) {
            if (projectName.contains("/")) {
                sh "unzip -p ${it} \$(unzip -l ${it} | awk \'\$4 ~ /^analyze-res\\/[^/]+\\.log\$/{print \$4}\') > \"${resultPath}/${it.name[0..-5]}.log\""
            } else {
                sh "unzip -p ${it} analyze-res/${projectName}.log > \"${resultPath}/${projectName}.log\""
            }
        } else {
            bat "tar -xzf ${it} analyze-res/${projectName}.log"
            bat "move analyze-res\\${projectName}.log \"${resultPath}\""
        }
    }
}

def getSvaceReports(svaceXmlPath, svacerPdfPath, svacerProject, snapshotSvaceName, branch=env.UPSTREAM_GIT_BRANCH, credentialsId='879c6da5-0a0b-4a83-9044-09a124747a06', svacerUrl='https://vmsvacer.aladdin.ru/') {
    def svaceXmlParts = svaceXmlPath.split("/")
    def svacerPdfParts = svacerPdfPath.split("/")
    def svaceDir = svaceXmlParts[0..-2].join("/")
    def svacerDir = svacerPdfParts[0..-2].join("/") 
    def svaceXml = svaceXmlParts[-1]
    def svacerPdf = svacerPdfParts[-1]

    if (isUnix()) {
        sh "mkdir -p \"${svaceDir}\" \"${svacerDir}\""
    } else {
        svaceDir = svaceDir.replaceAll("/","\\\\")
        svacerDir = svacerDir.replaceAll("/","\\\\")
        bat "mkdir \"${svaceDir}\" \"${svacerDir}\""
    }
    
    getSvacePublishData(svacerProject, branch, snapshotSvaceName, svaceXml, svaceDir, credentialsId, svacerUrl)
    retryWithDelay(5, 60) {
        getSvacerPdf(svacerProject, branch, snapshotSvaceName, svacerDir, svacerPdf, credentialsId, svacerUrl)
    }
    getSvaceServerAnalyzeLog(svacerProject, svaceDir)
}

def importMarkup(svacerProject, branch=env.UPSTREAM_GIT_BRANCH, svacerBin='', svacerAddress="vmsvacer.aladdin.ru") {
    if (env.IMPORT_MARKUP == 'false' || !env.IMPORT_MARKUP) return
    
    if (svacerBin == '')  {
        def svacerFile
        if (isUnix()) {
            svacerFile = findFiles(glob: '**/bin/svacer')
        } else {
            svacerFile = findFiles(glob: '**/bin/svacer.exe')
        }
        svacerBin = svacerFile[0].path
    }

    def svacerCredentials = '324180ad-ceb3-4d12-855f-56d81e702ee7'
    withCredentials([usernamePassword(credentialsId: svacerCredentials, usernameVariable: 'SVACER_CREDS_USR', passwordVariable: 'SVACER_CREDS_PSW')]) {
        sh "\"${svacerBin}\" --debug markup --host ${svacerAddress} --port 443 --ssl --user $SVACER_CREDS_USR --password $SVACER_CREDS_PSW  --project ${svacerProject} --branch ${branch} import --template DEFAULT --from-build-object"
    }
}

def publishSvaceDataToRedmine(fileSvaceData, redmineProjectId, version, userId, projectName, redmineUrl = 'https://rm.aladdin.ru', redmineToken = '8f73f326-3fb6-4865-ae8d-abaec0a0b11c' ) {

    if (redmineUrl == 'https://rm.aladdin.ru') {
        copyArtifacts (
            projectName: 'AutoTest/QAJenkins/QAJenkins%2Fredmine-bug-publisher/master',
            selector: lastSuccessful(),
            filter: "**",
            flatten: true,
            target: 'RedminePublishBin'
        )
    } else { 
        withCredentials([usernamePassword(credentialsId: '2cddd379-9aae-4acb-914b-32b7df5e2b1f', usernameVariable: 'USER_JENKINS', passwordVariable: 'PASSWORD_JENKINS')]) {
            if ( isUnix() ){
                sh "mkdir -p RedminePublishBin"
                sh "curl -o RedminePublishBin/artifact.zip http://${env.USER_JENKINS}:${env.PASSWORD_JENKINS}@git-srv.aladdin.ru/jenkins/job/AutoTest/job/QAJenkins/job/QAJenkins%252Fredmine-bug-publisher/job/master/lastSuccessfulBuild/artifact/*zip*/archive.zip"
                sh "cd RedminePublishBin && unzip -j artifact.zip"
            } else {
                error "Windows проектов ещё не было для ext jenkins, если получили данное сообщение, то следует доработать ф-юю publishSvaceDataToRedmine в bhl"
            }
        }
    }
    
    def updateTemplate  = libraryResource 'ru/aladdin/jenkins/bhl/svace/update_template.j2'
    def failureTemplate = libraryResource 'ru/aladdin/jenkins/bhl/svace/failure_template.j2'
    writeFile file: 'RedminePublishBin/update_template.j2', text: updateTemplate, encoding: 'UTF-8'
    writeFile file: 'RedminePublishBin/failure_template.j2', text: failureTemplate, encoding: 'UTF-8' 
        
    withCredentials([string(credentialsId: redmineToken, variable: 'REDMINE_TOKEN')]) {
        if ( isUnix() ){
            sh "chmod a+x \"${env.WORKSPACE}/RedminePublishBin/redmine_bug_publisher_x64\""
            sh "\"${env.WORKSPACE}/RedminePublishBin/redmine_bug_publisher_x64\" --redmine_url ${redmineUrl} --api_key ${env.REDMINE_TOKEN} --project_name SvaceAnalyze --custom_subject \"[${projectName}] Svace errors\" --project_id ${redmineProjectId} --tracker_id 1 --source_build 1 --build_url url --user_id ${userId} --status_id 5  -r --results \"${fileSvaceData}\" --failures_template RedminePublishBin/failure_template.j2 --update_template RedminePublishBin/update_template.j2 --build_version_package ${version} --close_untracked"
        } else {
            bat "\"${env.WORKSPACE}\\RedminePublishBin\\redmine_bug_publisher_x86.exe\" --redmine_url ${redmineUrl} --api_key ${env.REDMINE_TOKEN} --project_name SvaceAnalyze --custom_subject \"[${projectName}] Svace errors\" --project_id ${redmineProjectId} --tracker_id 1 --source_build 1 --build_url url --user_id ${userId} --status_id 5 --results \"${fileSvaceData}\" --failures_template RedminePublishBin\\failure_template.j2 --update_template RedminePublishBin\\update_template.j2 --build_version_package ${version} --close_untracked"
        }
    }
}
