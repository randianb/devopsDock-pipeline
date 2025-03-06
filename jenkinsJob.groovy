// Monitor build status with retry mechanism
timeout(time: 15, unit: 'MINUTES') {
    def retryCount = 0
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

        if (buildStatus == "FAILURE" && retryCount == 0) {
            echo "Job ${jobPath} failed on first attempt. Retrying..."
            retryCount++

            // Re-trigger the job with the same parameters
            def retryResponse = sh(
                script: """
                curl -s -X POST --user "\$JENKINS_USER:\$JENKINS_TOKEN" "${triggerUrl}"
                """,
                returnStdout: true
            ).trim()

            echo "Retry Trigger Response: ${retryResponse}"

            sleep 8

            // Wait for the retried job to start
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
                    def newRunningBuild = buildsJson.builds.find { it.building == true }

                    if (newRunningBuild) {
                        runningJobNumber = newRunningBuild.number.toString()
                        echo "Retry job started with build number: ${runningJobNumber}"
                        break
                    }
                }
            }
        } else if (buildStatus in ["SUCCESS", "FAILURE", "ABORTED"]) {
            jobResult['status'] = buildStatus
            jobResult['buildNumber'] = runningJobNumber
            echo "Remote job ${jobPath} completed with status: ${buildStatus}."
            break
        } else {
            echo "Waiting for remote job ${jobPath} to finish..."
        }

        sleep 15
    }
}