import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import groovy.text.SimpleTemplateEngine

class JsonProcessor {
    static void main(String[] args) {
        def processor = new JsonProcessor()

        // Example URL - replace with your actual JSON URL
        String url = "https://example.com/data.json"

        try {
            // Download and process JSON
            String jsonContent = processor.downloadJson(url)
            def jsonData = processor.parseJson(jsonContent)

            // Generate HTML
            processor.generateHtml(jsonData)

            // Convert to XML
            processor.convertToXml(jsonData)

            println "Processing completed successfully!"
        } catch (Exception e) {
            println "Error occurred: ${e.message}"
            e.printStackTrace()
        }
    }

    String downloadJson(String url) {
        println "Downloading JSON from: $url"
        def connection = new URL(url).openConnection()
        connection.requestMethod = 'GET'
        return connection.inputStream.text
    }

    def parseJson(String jsonContent) {
        println "Parsing JSON content"
        def jsonSlurper = new JsonSlurper()
        return jsonSlurper.parseText(jsonContent)
    }

    void generateHtml(def jsonData) {
        println "Generating HTML file"

        def template = '''
        <div>
            <div id="employee">
                <p>${name}</p><br/>
                <p>${age}</p><br/>
                <p>${secretIdentity}</p><br/>
                <ul id="powers">
                    <% powers.each { power -> %>
                        <li>${power}</li>
                    <% } %>
                </ul>
            </div>
        </div>
        '''

        def engine = new SimpleTemplateEngine()
        def result = engine.createTemplate(template).make(jsonData)

        new File('output.html').text = result.toString()
    }

    void convertToXml(def jsonData) {
        println "Converting to XML"

        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        xml.employee {
            name(jsonData.name)
            age(jsonData.age)
            secretIdentity(jsonData.secretIdentity)
            powers {
                jsonData.powers.each { power ->
                    power(power)
                }
            }
        }

        new File('output.xml').text = writer.toString()
    }
}