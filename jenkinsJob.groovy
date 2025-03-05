i found that the error is on this "                        def buildInfoJson = sh(
                            script: """
                            curl -v -s --user \${JENKINS_USER}:\${JENKINS_TOKEN} \
                            '${jenkinsUrl}/${jobPath}/${buildNumber}/api/json?tree=actions[parameters[*]]'""",
                            returnStdout: true
                        ).trim()" the result is this "https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/openr-pipeline-int/810/api/json?tree=actions[parameters[*" wich is not valid it should return this "https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/openr-pipeline-int/810/api/json?tree=actions[parameters[*]" instead 
