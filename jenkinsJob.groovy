Parameters for New Build: SERVICE=unibank-branch-onboarding&ENV=uat&REBOOT=true
[Pipeline] sh
+ curl -s --user '****:****' 'https://cdp-jenkins-paas-xsf.fr.world.socgen/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,'\'':'\'',//crumb)'
[Pipeline] echo
Triggering build with URL: https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/Reboot/buildWithParameters?SERVICE=unibank-branch-onboarding&ENV=uat&REBOOT=true
[Pipeline] sh
+ curl -s -X POST --user '****:****' ''
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
/var/jenkins_home/jobs/DJD/jobs/Build-Archived/workspace/JenkinsCron@tmp/jfrog/53/.jfrog deleted
Finished: FAILUR
