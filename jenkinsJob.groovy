[Pipeline] {
[Pipeline] script
[Pipeline] {
[Pipeline] sh
+ curl -s --user '****:****' https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/openr-pipeline-int/lastSuccessfulBuild/buildNumber
[Pipeline] echo
Latest Successful Build Number: 810
[Pipeline] sh
+ curl -s --user '****:****' 'https://cdp-jenkins-paas-xsf.fr.world.socgen/job/DJD/job/CD-Deploy/job/openr-pipeline-int/810/api/json?tree=actions[parameters[*]]'
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
