i got this error : 
[Pipeline] }
[Pipeline] // node
[Pipeline] End of Pipeline
Also:   org.jenkinsci.plugins.workflow.actions.ErrorAction$ErrorId: 20303cb4-25bc-4c3c-9eff-2b1da37c5ffd
groovy.lang.MissingPropertyException: No such property: TRIGRAM for class: java.lang.String
	at org.codehaus.groovy.runtime.ScriptBytecodeAdapter.unwrap(ScriptBytecodeAdapter.java:66)
	at org.codehaus.groovy.runtime.ScriptBytecodeAdapter.getProperty(ScriptBytecodeAdapter.java:471)
	at com.cloudbees.groovy.cps.sandbox.DefaultInvoker.getProperty(DefaultInvoker.java:39)
	at org.jenkinsci.plugins.workflow.cps.LoggingInvoker.getProperty(LoggingInvoker.java:133)
	at com.cloudbees.groovy.cps.impl.PropertyAccessBlock.rawGet(PropertyAccessBlock.java:20)
	at jenkins_cron.call(jenkins_cron.groovy:51)
	at ___cps.transform___(Native Method)
	at com.cloudbees.groovy.cps.impl.PropertyishBlock$ContinuationImpl.get(PropertyishBlock.java:73)
	at com.cloudbees.groovy.cps.LValueBlock$GetAdapter.receive(LValueBlock.java:30)
	at com.cloudbees.groovy.cps.impl.PropertyishBlock$ContinuationImpl.fixName(PropertyishBlock.java:65)
	at jdk.internal.reflect.GeneratedMethodAccessor421.invoke(Unknown Source)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:568)
	at com.cloudbees.groovy.cps.impl.ContinuationPtr$ContinuationImpl.receive(ContinuationPtr.java:72)
	at com.cloudbees.groovy.cps.impl.ConstantBlock.eval(ConstantBlock.java:21)
	at com.cloudbees.groovy.cps.Next.step(Next.java:83)
	at com.cloudbees.groovy.cps.Continuable.run0(Continuable.java:147)
	at org.jenkinsci.plugins.workflow.cps.SandboxContinuable.access$001(SandboxContinuable.java:17)
	at org.jenkinsci.plugins.workflow.cps.SandboxContinuable.run0(SandboxContinuable.java:49)
	at org.jenkinsci.plugins.workflow.cps.CpsThread.runNextChunk(CpsThread.java:180)
	at org.jenkinsci.plugins.workflow.cps.CpsThreadGroup.run(CpsThreadGroup.java:423)
	at org.jenkinsci.plugins.workflow.cps.CpsThreadGroup$2.call(CpsThreadGroup.java:331)
	at org.jenkinsci.plugins.workflow.cps.CpsThreadGroup$2.call(CpsThreadGroup.java:295)
	at org.jenkinsci.plugins.workflow.cps.CpsVmExecutorService.lambda$wrap$4(CpsVmExecutorService.java:140)
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at hudson.remoting.SingleLaneExecutorService$1.run(SingleLaneExecutorService.java:139)
	at jenkins.util.ContextResettingExecutorService$1.run(ContextResettingExecutorService.java:28)
	at jenkins.security.ImpersonatingExecutorService$1.run(ImpersonatingExecutorService.java:68)
	at jenkins.util.ErrorLoggingExecutorService.lambda$wrap$0(ErrorLoggingExecutorService.java:51)
	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539)
	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
	at org.jenkinsci.plugins.workflow.cps.CpsVmExecutorService$1.call(CpsVmExecutorService.java:53)
	at org.jenkinsci.plugins.workflow.cps.CpsVmExecutorService$1.call(CpsVmExecutorService.java:50)
	at org.codehaus.groovy.runtime.GroovyCategorySupport$ThreadCategoryInfo.use(GroovyCategorySupport.java:136)
	at org.codehaus.groovy.runtime.GroovyCategorySupport.use(GroovyCategorySupport.java:275)
	at org.jenkinsci.plugins.workflow.cps.CpsVmExecutorService.lambda$categoryThreadFactory$0(CpsVmExecutorService.java:50)
	at java.base/java.lang.Thread.run(Thread.java:833)
/var/jenkins_home/jobs/DJD/jobs/INFRA/workspace/JenkinsCron@tmp/jfrog/16/.jfrog deleted
Finished: FAILURE

here's my full groovy : 
import java.net.URLEncoder
import groovy.json.JsonSlurper

def call(Map config) {

    def sharedLibVersion = config['v_sharedlib'] ?: 'main'
    def jenkinsUrl = "https://cdp-jenkins-paas-xsf.fr.world.socgen"
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
                            [$class: 'RelativeTargetDirectory',  relativeTargetDir: '.'], 
                        ],
                        userRemoteConfigs: [[credentialsId: 'sgithub', url: "https://sgithub.fr.world.socgen/SGABS/xsf-devops-pipeline"]]
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
                            def account = "${env.TRIGRAM}_${env.IRT}_${env.ENV}"
                            echo "${account}"
                            def jobResults = []

                            jobPaths.each { jobPath ->
                                def jobResult = [:]
                                jobResult['jobPath'] = jobPath

                                // Get the last successful build number
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
                            echo "Job Results: ${jobResults}"
                            // def account = "${env.TRIGRAM}_${env.IRT}_${env.ENV}"

                            sh(
                                script: """
                                cd pipeline/scripts/notify
                                ./jenkins_report.sh '${jobResults}' '${account}'
                                """,
                                returnStdout: true
                            ).trim()

                        }
                    }
                }
            }
        }
    }
}
