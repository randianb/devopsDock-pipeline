[Pipeline] sh
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
+ curl -s --user '****:****' '/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,'\'':'\'',//crumb)'
[Pipeline] }
[Pipeline] // script
[Pipeline] }
[Pipeline] // withCredentials
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] // node
[Pipeline] End of Pipeline
ERROR: script returned exit code 3
