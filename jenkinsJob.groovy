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
                            script: """curl -s --user \${JENKINS_USER}:\${JENKINS_TOKEN} \\
                            '${jenkinsUrl}/${jobPath}/lastSuccessfulBuild/buildNumber'""",
                            returnStdout: true
                        ).trim()
                        
                        echo "Latest Successful Build Number: ${buildNumber}"

                        // Fetch build details (parameters and environment variables)
                        def buildInfoJson = sh(
                            script: """curl -s --user \${JENKINS_USER}:\${JENKINS_TOKEN} \\
                            '${jenkinsUrl}/${jobPath}/${buildNumber}/api/json?tree=actions%5Bparameters%5B*%5D%5D'""",
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

================================================

curl -X POST https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/openr-pipeline-int/buildWithParameters \
     --user ********:************ \
     --data-urlencode "LATEST_IMAGE=false" --data-urlencode "INFRA=false" --data-urlencode "DEPLOY_FRONTEND=true" --data-urlencode "DEPLOY_BACKEND=false" --data-urlencode "FRONTEND_VERSION=2.1.1-SNAPSHOT" --data-urlencode "BACKEND_VERSION=0" --data-urlencode "VERSION_TYPE=snapshots" --data-urlencode "REGION=paris"


CRUMB=$(curl -s -u "your_user:your_api_token" "https://cdp-jenkins-paas-xsf.fr.world.socgen/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,\":\",//crumb)")

curl -X POST "https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/openr-pipeline-int/buildWithParameters" \
     --user "your_user:your_api_token" \
     -H "$CRUMB" \
     --data-urlencode "LATEST_IMAGE=false" \
     --data-urlencode "INFRA=false" \
     --data-urlencode "DEPLOY_FRONTEND=true" \
     --data-urlencode "DEPLOY_BACKEND=false" \
     --data-urlencode "FRONTEND_VERSION=2.1.1-SNAPSHOT" \
     --data-urlencode "BACKEND_VERSION=0" \
     --data-urlencode "VERSION_TYPE=snapshots" \
     --data-urlencode "REGION=paris"
