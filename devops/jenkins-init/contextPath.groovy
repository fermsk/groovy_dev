import jenkins.model.JenkinsLocationConfiguration
import jenkins.model.Jenkins

// Set Jenkins URL with the correct context path
def jenkins = Jenkins.getInstanceOrNull()
if (jenkins != null) {
    def locationConfiguration = JenkinsLocationConfiguration.get()
    if (locationConfiguration != null) {
        locationConfiguration.setUrl("https://git-srv.aladdin.ru/jenkins/")
        locationConfiguration.save()
        println "Jenkins location URL configured successfully: https://git-srv.aladdin.ru/jenkins/"
    } else {
        println "WARNING: JenkinsLocationConfiguration is null"
    }
    
    println "Jenkins context path configured successfully"
} else {
    println "WARNING: Jenkins instance is null"
}