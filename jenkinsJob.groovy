 Connection #0 to host cdp-jenkins-paas-xsf.fr.world.socgen left intact
[Pipeline] sh
+ curl -s -o /dev/null -w '%{http_code}' --user '****:****' 'https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/openr-pipeline-int/810/api/json?tree=actions%5Bparameters%5B*%5D%5D'
[Pipeline] echo
Curl Status: 200
[Pipeline] echo
Build Info JSON: {"_class":"org.jenkinsci.plugins.workflow.job.WorkflowRun","actions":[{"_class":"hudson.model.ParametersAction","parameters":[{"_class":"hudson.model.BooleanParameterValue","name":"LATEST_IMAGE","value":false},{"_class":"hudson.model.BooleanParameterValue","name":"INFRA","value":false},{"_class":"hudson.model.BooleanParameterValue","name":"DEPLOY_FRONTEND","value":false},{"_class":"hudson.model.BooleanParameterValue","name":"DEPLOY_BACKEND","value":false},{"_class":"hudson.model.StringParameterValue","name":"FRONTEND_VERSION","value":""},{"_class":"hudson.model.StringParameterValue","name":"BACKEND_VERSION","value":""},{"_class":"hudson.model.StringParameterValue","name":"VERSION_TYPE","value":"snapshots"},{"_class":"hudson.model.StringParameterValue","name":"REGION","value":"'[\"paris\"]'"}]},{"_class":"hudson.model.CauseAction"},{"_class":"jenkins.metrics.impl.TimeInQueueAction"},{},{"_class":"org.jenkinsci.plugins.workflow.libs.LibrariesAction"},{},{"_class":"hudson.plugins.git.util.BuildData"},{},{"_class":"org.jenkinsci.plugins.workflow.cps.EnvActionImpl"},{},{"_class":"hudson.plugins.git.util.BuildData"},{},{},{},{},{"_class":"org.jenkinsci.plugins.displayurlapi.actions.RunDisplayAction"},{"_class":"org.jenkinsci.plugins.pipeline.modeldefinition.actions.RestartDeclarativePipelineAction"},{},{"_class":"org.jenkinsci.plugins.workflow.job.views.FlowGraphAction"},{},{},{}]}
[Pipeline] readJSON
[Pipeline] echo
Parameters for New Build: LATEST_IMAGE=false&INFRA=false&DEPLOY_FRONTEND=false&DEPLOY_BACKEND=false&FRONTEND_VERSION=&BACKEND_VERSION=&VERSION_TYPE=snapshots&REGION='["paris"]'
[Pipeline] sh
+ curl -s --user '****:****' 'https://cdp-jenkins-paas-xsf.fr.world.socgen/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)'
[Pipeline] sh
/var/jenkins_home/jobs/DJD/jobs/Build-Archived/jobs/JenkinsCron/workspace@tmp/durable-9963f6e7/script.sh.copy: line 17: unexpected EOF while looking for matching `"'
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
