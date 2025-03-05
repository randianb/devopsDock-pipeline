import java.net.URLEncoder

def jenkinsUrl = "https://cdp-jenkins-paas-xsf.fr.world.socgen"
def jobPath = "job/DJD/job/CD-Deploy/job/Reboot"

pipeline {
    agent any

    stages {
        stage('Get Last Success Build') {
            steps {
                withCredentials([
                    string(credentialsId: 'jenkins-user', variable: 'JENKINS_USER'),
                    string(credentialsId: 'jenkins-token', variable: 'JENKINS_TOKEN')
                ]) {
                    script {
                        // Get the last successful build number
                        def buildNumber = sh(
                            script: """
                            curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                            '${jenkinsUrl}/${jobPath}/lastSuccessfulBuild/buildNumber'""",
                            returnStdout: true
                        ).trim()

                        echo "Latest Successful Build Number: ${buildNumber}"

                        // Fetch build details (parameters and environment variables)
                        def buildInfoJson = sh(
                            script: """
                            curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                            '${jenkinsUrl}/${jobPath}/${buildNumber}/api/json?tree=actions[parameters[*]]'""",
                            returnStdout: true
                        ).trim()

                        // Check if curl returned an error code
                        def buildInfoStatus = sh(
                            script: """
                            curl -s -o /dev/null -w "%{http_code}" --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                            '${jenkinsUrl}/${jobPath}/${buildNumber}/api/json?tree=actions[parameters[*]]'
                            """,
                            returnStdout: true
                        ).trim()

                        echo "Curl Status: ${buildInfoStatus}"

                        if (buildInfoStatus != "200") {
                            echo "Error: Received invalid response or error code ${buildInfoStatus}"
                            error "Failed to fetch valid build info"
                        }

                        echo "Build Info JSON: ${buildInfoJson}"

                        // Parse the JSON response
                        def buildInfo = readJSON text: buildInfoJson

                        def parameters = []
                        buildInfo.actions.each { action ->
                            if (action.parameters) {
                                parameters.addAll(action.parameters)
                            }
                        }

                        // Encode parameters safely
                        def paramString = parameters.collect { 
                            def encodedName = URLEncoder.encode(it.name, "UTF-8")
                            def encodedValue = URLEncoder.encode(it.value.toString(), "UTF-8")
                            return "${encodedName}=${encodedValue}"
                        }.join('&')

                        echo "Parameters for New Build: ${paramString}"

                        // Fetch Jenkins crumb for CSRF protection
                        def crumbResponse = sh(
                            script: """
                            curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                            "\${jenkinsUrl}/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,':',//crumb)"
                            """,
                            returnStdout: true
                        ).trim()

                        def crumbHeader = crumbResponse.split(":")[0]
                        def crumbValue = crumbResponse.split(":")[1]

                        // Trigger a new build with the same parameters
                        def triggerUrl = "${jenkinsUrl}/${jobPath}/buildWithParameters?${paramString}"
                        echo "Triggering build with URL: ${triggerUrl}"

                        def triggerResponse = sh(
                            script: """
                            curl -s -X POST --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                            -H "\${crumbHeader}:\${crumbValue}" \
                            "\${triggerUrl}"
                            """,
                            returnStdout: true
                        ).trim()

                        echo "Build Trigger Response: ${triggerResponse}"
                    }
                }
            }
        }
    }
}