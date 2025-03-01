import groovy.json.JsonOutput

// Define variable
def pipelineLibLocation = "pipeline/lib"
def REGIONS = "${params.REGION}"
def regionList = REGIONS.split(",")
def COMPONENT = "int-djd-af107"
def imageMap = [:] // Define imageMap at a higher scope
def secrets = [
    [path: 'kv/global', secretValues: [
      [vaultKey: 'XSF_S3_ACCESS_KEY'],
      [vaultKey: 'XSF_S3_SECRET_KEY']
  ]],
  [path: 'kv/djd/dev', secretValues: [
    [vaultKey: 'DJD_ACCOUNT_ID'], 
    [vaultKey: 'DJD_CLIENT_ID'], 
    [vaultKey: 'DJD_CLIENT_SECRET'],
    [vaultKey: 'DJD_FRONT_OAUTH2_CLIENT_ID', envVar:'OAUTH2_CLIENT_ID'],
    [vaultKey: 'DJD_FRONT_OAUTH2_CLIENT_SECRET', envVar:'OAUTH2_CLIENT_SECRET'],
  ]]

]
podTemplate(
  inheritFrom: 'jenkins-inbound-agent',
  containers: [
    containerTemplate(name: 'python', image: 'kube10-dtr-dev.fr.world.socgen/xsf-af567-dev-staging/python-builder:3.10.1', alwaysPullImage: false, ttyEnabled: true, command: 'cat'),
    containerTemplate(name: 'terraform', image: 'kube9-dtr-dev.fr.world.socgen/byo-ad016-dev-acid/terraform:1.4.2', alwaysPullImage: false, ttyEnabled: true, command: 'cat'),
    containerTemplate(name: 'ansible', image: 'kube9-dtr-dev.fr.world.socgen/byo-ad016-dev-acid/ansible:2.11.6', alwaysPullImage: false, ttyEnabled: true, command: 'cat')
  ]
){
  node(POD_LABEL) {
    withVault([vaultSecrets: secrets]) {
      withEnv([
        "LIB_DIR=${WORKSPACE}/$pipelineLibLocation",
        "PYTHONPATH=${WORKSPACE}/$pipelineLibLocation",
        "server_name=$COMPONENT"
        ])  {
    stage('CHECKOU_SCM') {
      checkout scm
    }
    if (env.LATEST_IMAGE == 'true') {
      stage('Latest OS Image'){
        container("python"){
          echo """
          ***********************************************************
          *             Get the OS image to be used ...             *             
          ***********************************************************
          """
          result = sh(script: """
            export ACCOUNT_ID=${DJD_ACCOUNT_ID}
            export CLIENT_ID=${DJD_CLIENT_ID}
            export CLIENT_SECRET=${DJD_CLIENT_SECRET}
            export TRIGRAM="djd"
            export IRT="af107"
            export CLOUD_ENV="dev"
            python ${WORKSPACE}/pipeline/lib/scripts/latest_os_factory.py $server_name $REGIONS
            """, returnStdout: true).trim()  
            def lines = result.split("\n") // Get all lines of output
            LATEST_OS_IMAGE = lines[-1]   // Last line should be the value of OS_FACTORY_IMAGE        
        }
          echo"""
          $result
          """
          echo """
          ********************************************************************************************************************************
          * The OCS Image that will be deployed: $LATEST_OS_IMAGE
          ********************************************************************************************************************************
          """
    }
    } 
    if (env.INFRA == 'true') {
      stage('INFRA'){
    
        container('terraform') {          

            sh """        
            cd projects/djd/int
            ls -alr

            sed -i 's|%latest-os-image%|$LATEST_OS_IMAGE|g' main.tf

            export AWS_ACCESS_KEY_ID=${XSF_S3_ACCESS_KEY}
            export AWS_SECRET_ACCESS_KEY=${XSF_S3_SECRET_KEY}
            export ACCOUNT_ID=${DJD_ACCOUNT_ID}
            export CLIENT_ID=${DJD_CLIENT_ID}
            export CLIENT_SECRET=${DJD_CLIENT_SECRET}

            echo -----------------------TERRAFORM-INIT---------------------------------
            terraform init
            echo -----------------------TERRAFORM-APPLY---------------------------------
            terraform apply -auto-approve -no-color
            """
          
        }
      }
    } 
    // 
    if (env.DEPLOY_FRONTEND == 'true'||env.DEPLOY_BACKEND == 'true' ) {
    stage("DEPLOY"){
      container('ansible') {        withCredentials([ 
            file(credentialsId: 'automation-ssh-key', variable: 'SSHKEY'),
            string(credentialsId: 'legacy-vault-token-uat', variable: 'VAULT_TOKEN'),
            string(credentialsId: 'aws-id', variable: 'AWS_ID'),
            string(credentialsId: 'aws-key', variable: 'AWS_KEY'),
            string(credentialsId: 'xsf-aws-id', variable: 'XSF_AWS_ID'),
            string(credentialsId: 'xsf-aws-key', variable: 'XSF_AWS_KEY'),
            string(credentialsId: 'smtp-host', variable: 'SMTP_HOST'),
            string(credentialsId: 'djd-role-id-dev', variable: 'VAULT_ROLE_DJD_DEV'),
            string(credentialsId: 'djd-role-secret-dev', variable: 'VAULT_SECRET_DJD_DEV'),
            string(credentialsId: 'djd-vault-dev-namespace', variable: 'VAULT_NAMESPACE_DJD_DEV')
          
        ]) {

            if (env.DEPLOY_FRONTEND == 'true') {
              sh """
              cd projects/djd/int/ansible
              ansible-playbook frontend.yml -i hosts.ini --private-key $SSHKEY -u cloud-user -v --extra-vars "front_version=${FRONTEND_VERSION} build_user=${BUILD_USER} build_user_id=${BUILD_USER_ID} email=${BUILD_USER_EMAIL} build_url=${BUILD_URL} build_number=${BUILD_NUMBER} version_type=${VERSION_TYPE} smtp_host=${SMTP_HOST}"
              ansible-playbook email.yml -v --extra-vars "service_name="openr-frontend" service_version=${FRONTEND_VERSION} build_user=${BUILD_USER} build_user_id=${BUILD_USER_ID} email=${BUILD_USER_EMAIL} build_url=${BUILD_URL} build_number=${BUILD_NUMBER} url_app=admin version_type=${VERSION_TYPE} region=${REGION}"
              """
            }
            if (env.DEPLOY_BACKEND == 'true') {
              sh """
              cd projects/djd/int/ansible
              ansible-playbook backend.yml -i hosts.ini --private-key $SSHKEY -u cloud-user -v --extra-vars "backend_version=${BACKEND_VERSION} build_user=${BUILD_USER} build_user_id=${BUILD_USER_ID} email=${BUILD_USER_EMAIL} build_url=${BUILD_URL} build_number=${BUILD_NUMBER} version_type=${VERSION_TYPE} smtp_host=${SMTP_HOST}"
              ansible-playbook email.yml -v --extra-vars "service_name="Unibank-branch-onboarding" service_version=${BACKEND_VERSION} build_user=${BUILD_USER} build_user_id=${BUILD_USER_ID} email=${BUILD_USER_EMAIL} build_url=${BUILD_URL} build_number=${BUILD_NUMBER} url_app=backend version_type=${VERSION_TYPE} region=${REGION}"
              """
            }
            
        }
      }
    }
    }
  
  }
  } 
  } 
}
