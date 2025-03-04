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



  <!DOCTYPE html><html><head resURL="/static/672d5e66" data-rooturl="" data-resurl="/static/672d5e66" data-extensions-available="true" data-unit-test="false" data-imagesurl="/static/672d5e66/images" data-crumb-header="" data-crumb-value="">



    <title>Jenkins [Jenkins]</title><link rel="stylesheet" href="/static/672d5e66/jsbundles/styles.css" type="text/css"><link rel="stylesheet" href="/static/672d5e66/css/responsive-grid.css" type="text/css"><link rel="icon" href="/static/672d5e66/favicon.svg" type="image/svg+xml"><link sizes="any" rel="alternate icon" href="/static/672d5e66/favicon.ico"><link sizes="180x180" rel="apple-touch-icon" href="/static/672d5e66/apple-touch-icon.png"><link color="#191717" rel="mask-icon" href="/static/672d5e66/mask-icon.svg"><meta name="theme-color" content="#ffffff"><script src="/static/672d5e66/scripts/behavior.js" type="text/javascript"></script><script src='/adjuncts/672d5e66/org/kohsuke/stapler/bind.js' type='text/javascript'></script><script src="/static/672d5e66/scripts/yui/yahoo/yahoo-min.js"></script><script src="/static/672d5e66/scripts/yui/dom/dom-min.js"></script><script src="/static/672d5e66/scripts/yui/event/event-min.js"></script><script src="/static/672d5e66/scripts/yui/animation/animation-min.js"></script><script src="/static/672d5e66/scripts/yui/dragdrop/dragdrop-min.js"></script><script src="/static/672d5e66/scripts/yui/container/container-min.js"></script><script src="/static/672d5e66/scripts/yui/connection/connection-min.js"></script><script src="/static/672d5e66/scripts/yui/datasource/datasource-min.js"></script><script src="/static/672d5e66/scripts/yui/autocomplete/autocomplete-min.js"></script><script src="/static/672d5e66/scripts/yui/menu/menu-min.js"></script><script src="/static/672d5e66/scripts/yui/element/element-min.js"></script><script src="/static/672d5e66/scripts/yui/button/button-min.js"></script><script src="/static/672d5e66/scripts/yui/storage/storage-min.js"></script><script src="/static/672d5e66/scripts/hudson-behavior.js" type="text/javascript"></script><script src="/static/672d5e66/scripts/sortable.js" type="text/javascript"></script><link rel="stylesheet" href="/static/672d5e66/scripts/yui/container/assets/container.css" type="text/css"><link rel="stylesheet" href="/static/672d5e66/scripts/yui/container/assets/skins/sam/container.css" type="text/css"><link rel="stylesheet" href="/static/672d5e66/scripts/yui/menu/assets/skins/sam/menu.css" type="text/css"><meta name="ROBOTS" content="INDEX,NOFOLLOW"><meta name="viewport" content="width=device-width, initial-scale=1"><script src="/adjuncts/672d5e66/org/kohsuke/stapler/jquery/jquery.full.js" type="text/javascript"></script><script>var Q=jQuery.noConflict()</script><script src='/adjuncts/672d5e66/org/jenkinsci/plugins/scriptsecurity/scripts/ScriptApproval/FormValidationPageDecorator/validate.js' type='text/javascript'></script><script>
    if(window.Prototype && JSON) {
    var _json_stringify = JSON.stringify;
    JSON.stringify = function(value) {
    var _array_tojson = Array.prototype.toJSON;
    delete Array.prototype.toJSON;
    var r=_json_stringify(value);
    Array.prototype.toJSON = _array_tojson;
    return r;
    };
    }
  </script><script src="/static/672d5e66/plugin/extended-choice-parameter/js/selectize.min.js" type="text/javascript"></script><script src="/static/672d5e66/plugin/extended-choice-parameter/js/jsoneditor.min.js" type="text/javascript"></script><script src="/static/672d5e66/plugin/extended-choice-parameter/js/jquery.jsonview.min.js" type="text/javascript"></script><link rel="stylesheet" href="/static/672d5e66/plugin/extended-choice-parameter/css/jquery.jsonview.css"><link rel="stylesheet" id="icon_stylesheet" href="/static/672d5e66/plugin/extended-choice-parameter/css/selectize.css"><link rel="stylesheet" id="icon_stylesheet" href="/static/672d5e66/plugin/extended-choice-parameter/css/selectize.bootstrap2.css"><link rel="stylesheet" id="theme_stylesheet"><link rel="stylesheet" id="icon_stylesheet"><script src="/static/672d5e66/jsbundles/vendors.js" type="text/javascript"></script><script src="/static/672d5e66/jsbundles/sortable-drag-drop.js" type="text/javascript"></script><script defer="true" src="/static/672d5e66/jsbundles/app.js" type="text/javascript"></script></head><body data-model-type="hudson.model.Hudson" id="jenkins" class="yui-skin-sam one-column jenkins-2.452.2" data-version="2.452.2"><a href="#skip2content" class="jenkins-skip-link">Skip to content</a><header id="page-header" class="page-header"><div class="page-header__brand"><div class="logo"><a id="jenkins-home-link" href="/"><img src="/static/672d5e66/images/svgs/logo.svg" alt="[Jenkins]" id="jenkins-head-icon"><img src="/static/672d5e66/images/title.svg" alt="Jenkins" width="139" id="jenkins-name-icon" height="34"></a></div><a href="/" class="page-header__brand-link"><img src="/static/672d5e66/images/svgs/logo.svg" alt="[Jenkins]" class="page-header__brand-image"><span class="page-header__brand-name">Jenkins</span></a></div><div class="searchbox hidden-xs"><form role="search" method="get" name="search" action="/search/" style="position:relative;" class="no-json"><div id="search-box-sizer"></div><div id="searchform"><input role="searchbox" name="q" placeholder="Search" id="search-box" class="main-search__input"><span class="main-search__icon-leading"><svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg"  viewBox="0 0 512 512"><title></title><path d="M221.09 64a157.09 157.09 0 10157.09 157.09A157.1 157.1 0 00221.09 64z" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="32"/><path fill="none" stroke="currentColor" stroke-linecap="round" stroke-miterlimit="10" stroke-width="32" d="M338.29 338.29L448 448"/></svg></span><a href="https://www.jenkins.io/redirect/search-box" class="main-search__icon-trailing"><svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><path d="M256 40a216 216 0 10216 216A216 216 0 00256 40z" fill="none" stroke="currentColor" stroke-miterlimit="10" stroke-width="38"/><path d="M200 202.29s.84-17.5 19.57-32.57C230.68 160.77 244 158.18 256 158c10.93-.14 20.69 1.67 26.53 4.45 10 4.76 29.47 16.38 29.47 41.09 0 26-17 37.81-36.37 50.8S251 281.43 251 296" fill="none" stroke="currentColor" stroke-linecap="round" stroke-miterlimit="10" stroke-width="38"/><circle cx="250" cy="360" r="25" fill="currentColor"/></svg></a><div id="search-box-completion" data-search-url="/search/"></div><script src='/adjuncts/672d5e66/jenkins/views/JenkinsHeader/search-box.js' type='text/javascript'></script></div></form></div><div class="login page-header__hyperlinks"><div id="visible-am-insertion" class="page-header__am-wrapper"></div><div id="visible-sec-am-insertion" class="page-header__am-wrapper"></div><a href="/login?from=%2Fjob%2FDJD%2Fjob%2FCD-Deploy%2Fjob%2Fopenr-pipeline-int%2FbuildWithParameters">log in</a></div></header><script src="/static/672d5e66/jsbundles/keyboard-shortcuts.js" type="text/javascript"></script><div id="breadcrumbBar" class="jenkins-breadcrumbs" aria-label="breadcrumb"><ol class="jenkins-breadcrumbs__list" id="breadcrumbs"><li class="jenkins-breadcrumbs__list-item"><a href="/" class="model-link">Dashboard</a></li><li class="children" data-href="/"></li></ol></div><div id="page-body" class="app-page-body app-page-body--one-column clear"><div id="main-panel"><a id="skip2content"></a><h1 style="text-align: center"><img src="/static/672d5e66/images/rage.svg" width="154" height="179"><span style="font-size:50px"> Oops!</span></h1><div id="error-description"><h2 style="text-align: center">A problem occurred while processing the request</h2><p style="text-align: center">Logging ID=7aa2ba08-a830-4fb5-b837-479058e15cb1</div></div></div><footer class="page-footer jenkins-mobile-hide"><div class="page-footer__flex-row"><div class="page-footer__footer-id-placeholder" id="footer"></div><div class="page-footer__links"><a class="jenkins-button jenkins-button--tertiary rest-api" href="api/">REST API</a><button type="button" class="jenkins-button jenkins-button--tertiary jenkins_ver" data-dropdown="true">

    Jenkins 2.452.2
  </button><template><div class="jenkins-dropdown"><template data-dropdown-icon="&lt;svg aria-hidden=&quot;true&quot; xmlns=&quot;http://www.w3.org/2000/svg&quot; viewBox=&quot;0 0 512 512&quot;&gt;&lt;path d=&quot;M352.92 80C288 80 256 144 256 144s-32-64-96.92-64c-52.76 0-94.54 44.14-95.08 96.81-1.1 109.33 86.73 187.08 183 252.42a16 16 0 0018 0c96.26-65.34 184.09-143.09 183-252.42-.54-52.67-42.32-96.81-95.08-96.81z&quot; fill=&quot;none&quot; stroke=&quot;currentColor&quot; stroke-linecap=&quot;round&quot; stroke-linejoin=&quot;round&quot; stroke-width=&quot;32&quot;/&gt;&lt;/svg&gt;
" data-dropdown-text="Get involved" data-dropdown-type="ITEM" data-dropdown-href="https://www.jenkins.io/participate/"></template><template data-dropdown-icon="&lt;svg aria-hidden=&quot;true&quot; xmlns=&quot;http://www.w3.org/2000/svg&quot; viewBox=&quot;0 0 512 512&quot;&gt;&lt;path d=&quot;M384 224v184a40 40 0 01-40 40H104a40 40 0 01-40-40V168a40 40 0 0140-40h167.48M336 64h112v112M224 288L440 72&quot; fill=&quot;none&quot; stroke=&quot;currentColor&quot; stroke-linecap=&quot;round&quot; stroke-linejoin=&quot;round&quot; stroke-width=&quot;32&quot;/&gt;&lt;/svg&gt;
" data-dropdown-text="Website" data-dropdown-type="ITEM" data-dropdown-href="https://www.jenkins.io/"></template></div></template></div></div></footer></body></html>[0]
