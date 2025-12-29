import jenkins.model.JenkinsLocationConfiguration
import jenkins.model.Jenkins
import hudson.Functions

def jenkins = Jenkins.getInstanceOrNull()
if (jenkins != null) {
    // Configure Jenkins URL
    def locationConfiguration = JenkinsLocationConfiguration.get()
    if (locationConfiguration != null) {
        locationConfiguration.setUrl("https://git-srv.aladdin.ru/jenkins/")
        locationConfiguration.save()
        println "Jenkins URL configured: https://git-srv.aladdin.ru/jenkins/"
    }
    
    println "Jenkins context path configuration completed"
} else {
    println "WARNING: Jenkins instance not ready yet"
}