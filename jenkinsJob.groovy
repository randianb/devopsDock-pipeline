uild Trigger Response: 
[Pipeline] }
[Pipeline] // script
[Pipeline] }
[Pipeline] // withCredentials
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] // node
[Pipeline] End of Pipeline
Also:   org.jenkinsci.plugins.workflow.actions.ErrorAction$ErrorId: ade3a72d-6b4b-4779-af47-b12823f60570
java.util.NoSuchElementException: Cannot access last() element from an empty List
	at org.codehaus.groovy.runtime.DefaultGroovyMethods.last(DefaultGroovyMethods.java:8998)
	at org.codehaus.groovy.runtime.dgm$383.doMethodInvoke(Unknown Source)
	at groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1225)
	at groovy.lang.MetaClassImpl.invokeMethod(MetaClassImpl.java:1034)
	at org.codehaus.groovy.runtime.callsite.PojoMetaClassSite.call(PojoMetaClassSite.java:46)
	at org.codehaus.groovy.runtime.callsite.CallSiteArray.defaultCall(CallSiteArray.java:47)
	at org.codehaus.groovy.runtime.callsite.AbstractCallSite.call(AbstractCallSite.java:116)
	at org.kohsuke.groovy.sandbox.impl.Checker$1.call(Checker.java:180)
	at org.kohsuke.groovy.sandbox.GroovyInterceptor.onMethodCall(GroovyInterceptor.java:23)
	at org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxInterceptor.onMethodCall(SandboxInterceptor.java:113)
	at org.kohsuke.groovy.sandbox.impl.Checker$1.call(Checker.java:178)
	at org.kohsuke.groovy.sandbox.impl.Checker.checkedCall(Checker.java:182)
	at com.cloudbees.groovy.cps.sandbox.SandboxInvoker.methodCall(SandboxInvoker.java:17)
	at org.jenkinsci.plugins.workflow.cps.LoggingInvoker.methodCall(LoggingInvoker.java:117)
	at WorkflowScript.run(WorkflowScript:83)
	at ___cps.transform___(Native Method)
	at com.cloudbees.groovy.cps.impl.ContinuationGroup.methodCall(ContinuationGroup.java:90)
	at com.cloudbees.groovy.cps.impl.FunctionCallBlock$ContinuationImpl.dispatchOrArg(FunctionCallBlock.java:116)
	at com.cloudbees.groovy.cps.impl.FunctionCallBlock$ContinuationImpl.fixName(FunctionCallBlock.java:80)
	at jdk.internal.reflect.GeneratedMethodAccessor419.invoke(Unknown Source)
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
/var/jenkins_home/jobs/DJD/jobs/Build-Archived/workspace/JenkinsCron@tmp/jfrog/61/.jfrog deleted
Finished: FAILURE


here's the full script please keep this don't change it (%5Bparameters%5B*%5D%5D)
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

                        // Extract queue item ID
                        def queueId = triggerResponse.tokenize('/').last()
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
