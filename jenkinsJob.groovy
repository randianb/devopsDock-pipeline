import java.net.URLEncoder
import groovy.json.JsonSlurper

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
                        def buildNumber = sh(
                            script: """
                            curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                            '${jenkinsUrl}/${jobPath}/lastSuccessfulBuild/buildNumber'
                            """,
                            returnStdout: true
                        ).trim()

                        if (!buildNumber.isInteger()) {
                            error "Failed to retrieve valid build number. Response: ${buildNumber}"
                        }

                        echo "Latest Successful Build Number: ${buildNumber}"

                        // Fetch build details (parameters)
                        def buildInfoJson = sh(
                            script: """
                            curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                            '${jenkinsUrl}/${jobPath}/${buildNumber}/api/json?tree=actions%5Bparameters%5B*%5D%5D'
                            """,
                            returnStdout: true
                        ).trim()

                        def buildInfoStatus = sh(
                            script: """
                            curl -s -o /dev/null -w "%{http_code}" --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                            '${jenkinsUrl}/${jobPath}/${buildNumber}/api/json?tree=actions%5Bparameters%5B*%5D%5D'
                            """,
                            returnStdout: true
                        ).trim()

                        if (buildInfoStatus != "200") {
                            error "Failed to fetch build info. HTTP Status: ${buildInfoStatus}"
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

                        // Trigger a new build with parameters
                        def triggerUrl = "${jenkinsUrl}/${jobPath}/buildWithParameters?${paramString}"
                        echo "Triggering build with URL: ${triggerUrl}"

                        def triggerResponse = sh(
                            script: """
                            curl -s -X POST --user "\$JENKINS_USER:\$JENKINS_TOKEN" "${triggerUrl}"
                            """,
                            returnStdout: true
                        ).trim()

                        echo "Build Trigger Response: ${triggerResponse}"

                        sleep 8

                        // Wait until the job starts by checking the running jobs
                        def runningJobNumber = ""
                        timeout(time: 5, unit: 'MINUTES') {
                            while (true) {
                                def builds = sh(
                                    script: """
                                    curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                                    '${jenkinsUrl}/${jobPath}/api/json?tree=builds%5Bnumber,status,building%5D'
                                    """,
                                    returnStdout: true
                                ).trim()

                                def buildsJson = readJSON text: builds
                                def runningBuild = buildsJson.builds.find { it.building == true }

                                if (runningBuild) {
                                    runningJobNumber = runningBuild.number.toString()
                                    echo "Running job started with build number: ${runningJobNumber}"
                                    break
                                }
                            }
                        }

                        // Monitor build status
                        timeout(time: 15, unit: 'MINUTES') {
                            while (true) {
                                def buildStatusJson = sh(
                                    script: """
                                    curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                                    '${jenkinsUrl}/${jobPath}/${runningJobNumber}/api/json?tree=result'
                                    """,
                                    returnStdout: true
                                ).trim()

                                def jsonSlurper = new JsonSlurper()
                                def buildStatus = jsonSlurper.parseText(buildStatusJson).result

                                if (buildStatus == "SUCCESS" || buildStatus == "FAILURE" || buildStatus == "ABORTED") {
                                    echo "Remote job completed with status: ${buildStatus}."
                                    break
                                } else {
                                    echo "Waiting for remote job to finish..."
                                }

                                sleep 15
                            }
                        }
                    }
                }
            }
        }
    }
}