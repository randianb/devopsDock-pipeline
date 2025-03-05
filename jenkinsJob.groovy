def jenkinsUrl = "https://cdp-jenkins-paas-xsf.fr.world.socgen"
def jobPath = "job/DJD/job/CD-Deploy/job/openr-pipeline-int"

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
                            curl -s --user \${JENKINS_USER}:\${JENKINS_TOKEN} \
                            '${jenkinsUrl}/${jobPath}/lastSuccessfulBuild/buildNumber'""",
                            returnStdout: true
                        ).trim()

                        echo "Latest Successful Build Number: ${buildNumber}"

                        // Fetch build details (parameters and environment variables)
                        def buildInfoJson = sh(
                            script: """
                            curl -v -s --user \${JENKINS_USER}:\${JENKINS_TOKEN} \
                            '${jenkinsUrl}/${jobPath}/${buildNumber}/api/json?tree=actions[parameters[*]]'
                            """,
                            returnStdout: true,
                            returnStatus: true
                        ).trim()

                        // Check if curl returned an error code
                        if (buildInfoJson == '3' || buildInfoJson.isEmpty()) {
                            echo "Error: Received invalid response or error code 3"
                            error "Failed to fetch valid build info"
                        }

                        echo "Build Info JSON: ${buildInfoJson}"

                        // Attempt to parse the JSON and extract parameters
                        def buildInfo = [:]
                        try {
                            buildInfo = readJSON text: buildInfoJson
                        } catch (Exception e) {
                            echo "Error parsing JSON: ${e.message}"
                            error "Failed to parse JSON response"
                        }

                        def parameters = []
                        buildInfo.actions.each { action ->
                            if (action.parameters) {
                                parameters.addAll(action.parameters)
                            }
                        }

                        // Construct parameters string for the new build
                        def paramString = parameters.collect { 
                            "${it.name}=${it.value}"
                        }.join('&')

                        echo "Parameters for New Build: ${paramString}"

                        // Fetch Jenkins crumb for CSRF protection
                        def crumbResponse = sh(
                            script: """
                            curl -s --user \${JENKINS_USER}:\${JENKINS_TOKEN} \
                            '${jenkinsUrl}/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)'""",
                            returnStdout: true
                        ).trim()

                        def crumbHeader = crumbResponse.split(":")[0]
                        def crumbValue = crumbResponse.split(":")[1]

                        // Trigger a new build with the same parameters
                        def triggerUrl = "${jenkinsUrl}/${jobPath}/buildWithParameters?${paramString}"
                        def triggerResponse = sh(
                            script: """
                            curl -s -X POST --user \${JENKINS_USER}:\${JENKINS_TOKEN} \
                            -H "${crumbHeader}: ${crumbValue}" \
                            '${triggerUrl}'""",
                            returnStdout: true
                        ).trim()

                        echo "Build Trigger Response: ${triggerResponse}"
                    }
                }
            }
        }
    }
}
