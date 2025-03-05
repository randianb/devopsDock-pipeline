this's the error i got : 
+ curl -s --user '****:****' https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/Reboot/lastSuccessfulBuild/buildNumber
[Pipeline] echo
Latest Successful Build Number: 32
[Pipeline] sh
+ curl -s --user '****:****' 'https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/Reboot/32/api/json?tree=actions%5Bparameters%5B*%5D%5D'
[Pipeline] sh
+ curl -s -o /dev/null -w '%{http_code}' --user '****:****' 'https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/Reboot/32/api/json?tree=actions%5Bparameters%5B*%5D%5D'
[Pipeline] echo
Curl Status: 200
[Pipeline] echo
Build Info JSON: {"_class":"org.jenkinsci.plugins.workflow.job.WorkflowRun","actions":[{"_class":"hudson.model.ParametersAction","parameters":[{"_class":"hudson.model.StringParameterValue","name":"SERVICE","value":"unibank-branch-onboarding"},{"_class":"hudson.model.StringParameterValue","name":"ENV","value":"uat"},{"_class":"hudson.model.BooleanParameterValue","name":"REBOOT","value":true}]},{"_class":"hudson.model.CauseAction"},{"_class":"jenkins.metrics.impl.TimeInQueueAction"},{},{"_class":"org.jenkinsci.plugins.workflow.libs.LibrariesAction"},{},{"_class":"hudson.plugins.git.util.BuildData"},{},{"_class":"org.jenkinsci.plugins.workflow.cps.EnvActionImpl"},{},{"_class":"hudson.plugins.git.util.BuildData"},{"_class":"org.jenkinsci.plugins.workflow.cps.view.InterpolatedSecretsAction"},{},{},{},{},{"_class":"org.jenkinsci.plugins.displayurlapi.actions.RunDisplayAction"},{"_class":"org.jenkinsci.plugins.pipeline.modeldefinition.actions.RestartDeclarativePipelineAction"},{},{"_class":"org.jenkinsci.plugins.workflow.job.views.FlowGraphAction"},{},{},{}]}
[Pipeline] readJSON
[Pipeline] echo
Parameters for New Build: SERVICE=unibank-branch-onboarding&ENV=uat&REBOOT=true
[Pipeline] sh
/var/jenkins_home/jobs/DJD/jobs/Build-Archived/jobs/JenkinsCron/workspace@tmp/durable-0b8f8032/script.sh.copy: line 2: syntax error near unexpected token `('
[Pipeline] }
[Pipeline] // script
[Pipeline] }
[Pipeline] // withCredentials
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] // node
[Pipeline] End of Pipeline
ERROR: script returned exit code 2
                                                                                                                                                            
here's the pipeline : 
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
                            curl -s --user \${JENKINS_USER}:\${JENKINS_TOKEN} \
                            '${jenkinsUrl}/${jobPath}/lastSuccessfulBuild/buildNumber'""",
                            returnStdout: true
                        ).trim()

                        echo "Latest Successful Build Number: ${buildNumber}"

                        // Fetch build details (parameters and environment variables)
                        def buildInfoJson = sh(
                            script: """
                            curl -s --user \${JENKINS_USER}:\${JENKINS_TOKEN} \
                            '${jenkinsUrl}/${jobPath}/${buildNumber}/api/json?tree=actions%5Bparameters%5B*%5D%5D'""",
                            returnStdout: true
                        ).trim()

                        // Check if curl returned an error code
                        def buildInfoStatus = sh(
                            script: """
                            curl -s -o /dev/null -w "%{http_code}" --user \${JENKINS_USER}:\${JENKINS_TOKEN} \
                            '${jenkinsUrl}/${jobPath}/${buildNumber}/api/json?tree=actions%5Bparameters%5B*%5D%5D'
                            """,
                            returnStdout: true
                        ).trim()

                        echo "Curl Status: ${buildInfoStatus}"

                        if (buildInfoStatus != "200") {
                            echo "Error: Received invalid response or error code ${buildInfoStatus}"
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

                        // Clean REGION value
                        def paramString = parameters.collect { 
                            if (it.name == 'REGION') {
                                // Fix REGION parameter to remove extra quotes
                                return "${it.name}=${URLEncoder.encode(it.value.replace("'", ""), 'UTF-8')}"
                            } else {
                                return "${it.name}=${it.value}"
                            }
                        }.join('&')

                        echo "Parameters for New Build: ${paramString}"

                        // Fetch Jenkins crumb for CSRF protection
                        def crumbResponse = sh(
                            script: """
                            curl -s --user \${JENKINS_USER}:\${JENKINS_TOKEN} \
                            \${jenkinsUrl}/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)""",
                            returnStdout: true
                        ).trim()

                        def crumbHeader = crumbResponse.split(":")[0]
                        def crumbValue = crumbResponse.split(":")[1]

                        // Trigger a new build with the same parameters
                        def triggerUrl = "${jenkinsUrl}/${jobPath}/buildWithParameters?${paramString}"
                        def triggerResponse = sh(
                            script: """
                            curl -s -X POST --user \${JENKINS_USER}:\${JENKINS_TOKEN} \
                            -H "${crumbHeader}:${crumbValue}" \
                            ${triggerUrl}""",
                            returnStdout: true
                        ).trim()

                        echo "Build Trigger Response: ${triggerResponse}"
                    }
                }
            }
        }
    }
}                                                                                                                                                            
