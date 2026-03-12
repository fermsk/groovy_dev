/*
Parameters:
    - script
    - libURL
    - branch
*/
def call( Map config ) {
    def script = null
    def libUrl = ''
    def branch = ''
    try {

        script = config.script
        libUrl = config.libUrl
        branch = config.branch

        def identifier = branch.trim() ? "build@${branch.trim()}" : 'build'
        library identifier: identifier, retriever: modernSCM ([
            $class: 'GitSCMSource',
            remote: libUrl,
            credentialsId: 'b5460d17-0940-42c0-94aa-0696de3381c4'
        ])
    } catch(e) {
        println e
        throw e
    }
    build.build(script)
}
