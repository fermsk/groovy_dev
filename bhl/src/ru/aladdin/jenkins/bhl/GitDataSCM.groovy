package ru.aladdin.jenkins.bhl

class GitDataSCM {

    def steps

    def GitDataSCM(steps){ this.steps = steps }

    def getGitSHA1(gitURL) {

        def bds = steps.currentBuild.rawBuild.getActions(hudson.plugins.git.util.BuildData)

        if ( bds == null ) {
            return null
        }

        def gitBuildData = null
        bds.each { bd -> 
            bd.remoteUrls.each { rURL ->
                steps.println rURL
                if ( rURL == gitURL ) {
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

        return gitBuildData.lastBuiltRevision.sha1String
    }

    def getGitSHA1Jenkinsfile() {

        def scmRevisionActions = steps.currentBuild.rawBuild.getActions(jenkins.scm.api.SCMRevisionAction)

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
}
