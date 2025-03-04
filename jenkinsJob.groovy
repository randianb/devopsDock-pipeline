def jenkinsUrl = "https://cdp-jenkins-paas-xsf.fr.com"
def jobPath = "job/DJD/job/CD-Deploy/job/openr-pipeline-int"

pipeline {
    agent any

    stages {
        stage('Get Last Success Build') {
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'jenkins-user', usernameVariable: 'JENKINS_USER', passwordVariable: 'JENKINS_TOKEN')
                ]) {
                    script {
                        // Get the last successful build number
                        def buildNumber = sh(
                            script: """curl -s --user \${JENKINS_USER}:\${JENKINS_TOKEN} \\
                            '${jenkinsUrl}/${jobPath}/lastSuccessfulBuild/buildNumber'""",
                            returnStdout: true
                        ).trim()
                        
                        echo "Latest Successful Build Number: ${buildNumber}"

                        // Fetch build details (parameters and environment variables)
                        def buildInfoJson = sh(
                            script: """curl -s --user \${JENKINS_USER}:\${JENKINS_TOKEN} \\
                            '${jenkinsUrl}/${jobPath}/${buildNumber}/api/json?tree=actions[parameters[*]]'""",
                            returnStdout: true
                        ).trim()
                        
                        echo "Build Info JSON: ${buildInfoJson}"

                        // Extract parameters (convert JSON response into a map)
                        def buildInfo = readJSON text: buildInfoJson
                        def parameters = buildInfo.actions.find { it.parameters }?.parameters ?: []

                        // Construct parameters string for the new build
                        def paramString = parameters.collect { 
                            "${it.name}=${it.value}"
                        }.join('&')

                        echo "Parameters for New Build: ${paramString}"

                        // Trigger a new build with the same parameters
                        def triggerUrl = "${jenkinsUrl}/${jobPath}/buildWithParameters?${paramString}"
                        def triggerResponse = sh(
                            script: """curl -s -X POST --user \${JENKINS_USER}:\${JENKINS_TOKEN} '${triggerUrl}'""",
                            returnStdout: true
                        ).trim()

                        echo "Build Trigger Response: ${triggerResponse}"
                    }
                }
            }
        }
    }
}
