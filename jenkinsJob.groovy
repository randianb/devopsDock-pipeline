import java.net.URLEncoder
import groovy.json.JsonSlurper

def call(Map config) {
    def sharedLibVersion = config['v_sharedlib'] ?: 'main'
    def jenkinsUrl = config["jenkinsUrl"]
    def jobPaths = [
        "job/DJD/job/CD-Deploy/job/Reboot",
        "job/DJD/job/CD-Deploy/job/openr-pipeline-int"
    ]
    def trigram = config["trigram"]
    def irt = config["irt"]
    def component = config["component"]
    def env = config['env']
    def pipelineVersion = config['v_pipeline'] ?: 'v0'
    def pipelineAppLocation = "pipeline/app/${pipelineVersion}"
    def pipelineLibLocation = "pipeline/lib"

    pipeline {
        agent any
        environment {
            LIB_DIR = "${WORKSPACE}/$pipelineLibLocation"
            PYTHONPATH = "${WORKSPACE}/$pipelineLibLocation"
            COMPONENT = "${component}"
            ENV = "${env}"
            TRIGRAM = "${trigram}"
            IRT = "${irt}"
        }
        stages {
            stage('Checkout') {
                steps {
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${sharedLibVersion}"]],
                        extensions: [
                            [$class: 'CloneOption', noTags: true, shallow: true, depth: 1, timeout: 30],
                            [$class: 'RelativeTargetDirectory', relativeTargetDir: '.']
                        ],
                        userRemoteConfigs: [[credentialsId: 'sgithub', url: "$DF_PIPELINE_REPO"]]
                    ])
                }
            }
            stage('Trigger and Monitor Remote Jobs') {
                steps {
                    withCredentials([
                        string(credentialsId: 'jenkins-user', variable: 'JENKINS_USER'),
                        string(credentialsId: 'jenkins-token', variable: 'JENKINS_TOKEN')
                    ]) {
                        script {
                            def account = "${TRIGRAM}_${IRT}_${ENV}"
                            def jobResults = []

                            jobPaths.each { jobPath ->
                                def jobResult = [:]
                                jobResult['jobPath'] = jobPath

                                // Get last successful build number
                                def buildNumber = sh(
                                    script: """
                                    curl -s --user "\$JENKINS_USER:\$JENKINS_TOKEN" \
                                    '${jenkinsUrl}/${jobPath}/lastSuccessfulBuild/buildNumber'
                                    """,
                                    returnStdout: true
                                ).trim()

                                if (!buildNumber.isInteger()) {
                                    jobResult['error'] = "Failed to retrieve valid build number. Response: ${buildNumber}"
                                    jobResults.add(jobResult)
                                    return
                                }

                                echo "Latest Successful Build Number for ${jobPath}: ${buildNumber}"

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
                                    jobResult['error'] = "Failed to fetch build info. HTTP Status: ${buildInfoStatus}"
                                    jobResults.add(jobResult)
                                    return
                                }

                                def buildInfo = readJSON text: buildInfoJson
                                def parameters = buildInfo.actions.findAll { it.parameters }.collectMany { it.parameters }

                                if (parameters.isEmpty()) {
                                    jobResult['error'] = "No parameters found in the last successful build."
                                    jobResults.add(jobResult)
                                    return
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

                                // Wait for the job to start
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

                                            // Re-trigger the job
                                            sh(
                                                script: """
                                                curl -s -X POST --user "\$JENKINS_USER:\$JENKINS_TOKEN" "${triggerUrl}"
                                                """,
                                                returnStdout: true
                                            ).trim()

                                            sleep 8
                                            continue
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

                                jobResults.add(jobResult)
                            }

                            // Generate a summary report
                            try {
                                echo "Job Results: ${jobResults}"
                                
                                if (jobResults.isEmpty()) {
                                    error("No job results found. Skipping report generation.")
                                }

                                echo "Sending summary report email.."

                                sh(
                                    script: """
                                    cd $LIB_DIR/scripts/notify
                                    chmod +x jenkins_report.sh 
                                    ./jenkins_report.sh '${jobResults}' '${account}'
                                    """,
                                    returnStdout: true
                                ).trim()
                                
                            } catch (Exception e) {
                                echo "Error while generating/sending summary report: ${e.message}"
                            }
                        }
                    }
                }
            }
        }
    }
}