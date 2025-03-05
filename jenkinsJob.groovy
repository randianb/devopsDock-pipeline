import java.net.URLEncoder

def jenkinsUrl = "https://cdp-jenkins-paas-xsf.fr.world.socgen"
def jobPath = "job/DJD/job/CD-Deploy/job/Reboot"

pipeline {
    agent any

    stages {
        stage('Trigger and Monitor Remote Job') {
            steps {
                withCredentials([
                    string(credentialsId: 'jenkins-user', variable: 'JENKINS_USER'),
                    string(credentialsId: 'jenkins-token', variable: 'JENKINS_TOKEN')
                ]) {
                    script {
                        // Get the last successful build number
                        def lastSuccessBuild = sh(
                            script: """
                            curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                            '${jenkinsUrl}/${jobPath}/lastSuccessfulBuild/buildNumber'
                            """,
                            returnStdout: true
                        ).trim()

                        if (!lastSuccessBuild.isInteger()) {
                            error "Failed to retrieve valid last successful build number. Response: ${lastSuccessBuild}"
                        }

                        echo "Latest Successful Build Number: ${lastSuccessBuild}"

                        // Fetch build parameters
                        def buildInfoJson = sh(
                            script: """
                            curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                            '${jenkinsUrl}/${jobPath}/${lastSuccessBuild}/api/json?tree=actions[parameters[*]]'
                            """,
                            returnStdout: true
                        ).trim()

                        if (!buildInfoJson) {
                            error "Failed to fetch build information."
                        }

                        echo "Build Info JSON: ${buildInfoJson}"

                        def buildInfo = readJSON text: buildInfoJson
                        def parameters = buildInfo.actions.findAll { it.parameters }.collectMany { it.parameters }

                        if (parameters.isEmpty()) {
                            error "No parameters found in the last successful build."
                        }

                        // Encode parameters safely
                        def paramString = parameters.collect { 
                            "${URLEncoder.encode(it.name, 'UTF-8')}=${URLEncoder.encode(it.value.toString(), 'UTF-8')}"
                        }.join('&')

                        echo "Parameters for New Build: ${paramString}"

                        // Trigger new build and capture queue item URL
                        def queueUrl = sh(
                            script: """
                            curl -s -X POST --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                            --data-urlencode "${paramString}" \
                            "${jenkinsUrl}/${jobPath}/buildWithParameters" -D - | grep -Fi location | awk '{print \$2}' | tr -d '\\r'
                            """,
                            returnStdout: true
                        ).trim()

                        if (!queueUrl) {
                            error "Failed to trigger build. No queue URL received."
                        }

                        echo "Build queued at: ${queueUrl}"

                        // Extract queue item ID
                        def queueId = queueUrl.tokenize('/').last()
                        echo "Queue ID: ${queueId}"

                        // Poll until the build starts
                        def newBuildNumber = ""
                        timeout(time: 5, unit: 'MINUTES') {
                            while (true) {
                                newBuildNumber = sh(
                                    script: """
                                    curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                                    '${jenkinsUrl}/queue/item/${queueId}/api/json?tree=executable[number]' | jq -r .executable.number
                                    """,
                                    returnStdout: true
                                ).trim()
                                
                                if (newBuildNumber.isInteger()) {
                                    echo "Build started with number: ${newBuildNumber}"
                                    break
                                }
                                
                                sleep 10
                            }
                        }

                        // Monitor build status
                        timeout(time: 15, unit: 'MINUTES') {
                            while (true) {
                                def buildStatus = sh(
                                    script: """
                                    curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                                    '${jenkinsUrl}/${jobPath}/${newBuildNumber}/api/json?tree=result' | jq -r .result
                                    """,
                                    returnStdout: true
                                ).trim()

                                if (buildStatus == "SUCCESS") {
                                    echo "Remote job completed successfully."
                                    break
                                } else if (buildStatus == "FAILURE" || buildStatus == "ABORTED") {
                                    error "Remote job failed with status: ${buildStatus}"
                                }
                                
                                echo "Waiting for remote job to finish..."
                                sleep 15
                            }
                        }
                    }
                }
            }
        }
    }
}