+ curl -s --user '****:****' 'https://cdp-jenkins-paas-xsf.fr.world.socgen/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,'\'':'\'',//crumb)'
[Pipeline] echo
Triggering build with URL: https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/Reboot/buildWithParameters?SERVICE=unibank-branch-onboarding&ENV=uat&REBOOT=true
[Pipeline] sh
+ curl -s -X POST --user '****:****' ''
