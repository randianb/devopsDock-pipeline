+ curl -s --user '****:****' https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/openr-pipeline-int/lastSuccessfulBuild/buildNumber
[Pipeline] echo
Latest Successful Build Number: 810
[Pipeline] sh
+ curl -s --user '****:****' 'https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/openr-pipeline-int/810/api/json?tree=actions[parameters[*]]'
[Pipeline] echo
Build Info JSON: 3
[Pipeline] readJSON
[Pipeline] echo
Error parsing JSON: Could not instantiate {text=3} for org.jenkinsci.plugins.pipeline.utility.steps.json.ReadJSONStep: java.lang.ClassCastException: class org.jenkinsci.plugins.pipeline.utility.steps.AbstractFileOrTextStep.setText() expects class java.lang.String but received class java.lang.Integer
[Pipeline] error
[Pipeline] }
[Pipeline] // script
[Pipeline] }
[Pipeline] // withCredentials
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] // node
[Pipeline] End of Pipeline
ERROR: Failed to parse JSON response
