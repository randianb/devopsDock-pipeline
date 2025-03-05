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
                        // Trigger the job
                        def triggerUrl = "${jenkinsUrl}/${jobPath}/build"
                        echo "Triggering job with URL: ${triggerUrl}"

                        def triggerResponse = sh(
                            script: """
                            curl -s -X POST --user "\$JENKINS_USER:\$JENKINS_TOKEN" "${triggerUrl}"
                            """,
                            returnStdout: true
                        ).trim()

                        echo "Build Trigger Response: ${triggerResponse}"

                        // Wait until the job starts by checking the running jobs
                        def runningJobNumber = ""
                        timeout(time: 5, unit: 'MINUTES') {
                            while (true) {
                                def builds = sh(
                                    script: """
                                    curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                                    '${jenkinsUrl}/${jobPath}/api/json?tree=builds[number,status,building]'
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

                                sleep 10
                            }
                        }

                        // Monitor build status
                        timeout(time: 15, unit: 'MINUTES') {
                            while (true) {
                                def buildStatus = sh(
                                    script: """
                                    curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                                    '${jenkinsUrl}/${jobPath}/${runningJobNumber}/api/json?tree=result' | jq -r .result
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