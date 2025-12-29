import jenkins.model.Jenkins
import jenkins.util.http.HtmlPageGenerator
import javax.servlet.http.HttpServletResponse

def jenkins = Jenkins.getInstanceOrNull()
if (jenkins != null) {
    // This will be handled by Traefik middleware instead
    println "Root redirect configuration ready (handled by Traefik)"
} else {
    println "WARNING: Jenkins instance not ready yet"
}