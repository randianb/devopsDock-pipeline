
def call(Map config) {

  // Lifecycle
  def kind = config['kind']
  def infra = (kind == 'infra_stateless' || kind == 'infra' || kind == 'ansible')
  def deplopyWithAnsible = config['ansible']
  def version = config['version']
  def app_group_id = config['app_group_id']
  def app_artifact_id = config['app_artifact_id']
  def app_artifact_version = config['app_artifact_version']
  def download_certificates = config['download_certificates'] ?: false
  def name = config['name']
  //def DESTROY_INFRA = config['DESTROY_INFRA']
  def database_migration = config['database_migration'] ?: false 
  def profile = database_migration != true ? "vault,production,json-logs" : "vault,production,json-logs,database-migration"
  def use_latest_os_image = config['use_latest_os_image'] ?: false

  // Component config
  def trigram = config["trigram"]
  def irt = config["irt"]
  def component = config["component"]
  def env = config['env']
  def regions = config['regions']
  def entity = config['entity'] ?: 'main'
  def token_secret = ''
  def javaVersion = config['javaVersion'] ?: 'openjdk_117_0_2_8'
  
  // Misc fongig
  //def buildCertificate = config['certificate']
  
  
  def cloudEnv = env

  if (cloudEnv == "prod" || cloudEnv == "prd") {
    cloudEnv = "prd"
  }else if (cloudEnv == "staging" || cloudEnv == "oat"  || cloudEnv == "ht") {
	  cloudEnv = "oat"	
  }else {
    cloudEnv = "dev"
  }
	
  // if (app_artifact_version.contains("SNAPSHOT") && env.toString() in ['oat','prd','prod']){
  //   error """
  //   **************************************************************************
  //   *   This version [$app_artifact_version] is not allowed to be deployed in: $env
  //   **************************************************************************
  //   """
  // }
  def steps = []
  if (use_latest_os_image) steps.add("latest os image")
  if (infra) steps.add("infra")
  //if (buildCertificate) steps.add("cert")
  if (deplopyWithAnsible) steps.add("deploy")
  if (download_certificates) steps.add("download certificates")
  //if (DESTROY_INFRA) steps.add("Destroy Infrastructure")

  if (!entity) throw new Error("config.entity is required")
  if (!regions) throw new Error("config.regions is required")
  if (!trigram) throw new Error("config.trigram is required")
  if (!irt) throw new Error("config.irt is required")
  if (!component) throw new Error("config.component is required")
  if (!javaVersion) throw new Error("config.java is required")


  // Pipeline config
  def sharedLibVersion = config['v_sharedlib'] ?: 'main'
  def pipelineVersion = config['v_pipeline'] ?: 'v0'
  def pipelineAppLocation = "pipeline/app/${pipelineVersion}"
  def pipelineLibLocation = "pipeline/lib"
  
  // Ansible Config
  def ansibleRoot = "$pipelineAppLocation/ansible"
  def ansible = config['ansible'] ?: false
  // 

  // Terraform config
  def terraformRoot = "$pipelineAppLocation/terraform/default"
  def terraformConfig = config['terraform'] ?: []
  def stateless = terraformConfig['stateless'] ?: false
  if (stateless) {
    terraformRoot = "$pipelineAppLocation/terraform/stateless"
  }
  def terraform_backend = ""

  // k8s config
  def k8sRoot = "$pipelineAppLocation/k8s"

  // Misc config
  def s3Endpoint = 'objs3parstd01.fr.world.socgen'
  
  currentBuild.displayName = "#${BUILD_NUMBER} ${env}"

  if (!steps.isEmpty()) currentBuild.displayName += " [${ steps.join(', ') }]"
  currentBuild.description = "${BUILD_USER} ($BUILD_USER_ID)"
  
  podTemplate(
    inheritFrom: 'jenkins-inbound-agent',
    containers: [
      containerTemplate(name: 'python', image: 'kube10-dtr-dev.fr.world.socgen/xsf-af567-dev-staging/python-builder:3.10.1', alwaysPullImage: false, ttyEnabled: true, command: 'cat'),
      containerTemplate(name: 'terraform', image: 'kube9-dtr-dev.fr.world.socgen/byo-ad016-dev-acid/terraform:1.4.2', alwaysPullImage: false, ttyEnabled: true, command: 'cat'),
      containerTemplate(name: 'ansible', image:'kube9-dtr-dev.fr.world.socgen/byo-ad016-dev-acid/ansible:2.11.6', alwaysPullImage: false, ttyEnabled: true, command: 'cat')
    ]
  ) {
    node(POD_LABEL) {
      withEnv([
        "LIB_DIR=${WORKSPACE}/$pipelineLibLocation",
        "PYTHONPATH=${WORKSPACE}/$pipelineLibLocation",
        "COMPONENT=${component}",
        "PROJECT_ENV=${WORKSPACE}/projects/$trigram/$component/$env",
        "BRANCH=main",
        "INSTANCE_TAG=blue",
        "ENV=${env}",
        "CLOUD_ENV=${cloudEnv}",
        "ANSIBLE_ROLES_PATH=${WORKSPACE}/$pipelineAppLocation/ansible/roles",
        "TRIGRAM=${trigram}",
        "IRT=${irt}",
        "GROUP_ID=${app_group_id}",
        "ARTIFACT_ID=${app_artifact_id}",
        "ARTIFACT_VERSION=${app_artifact_version}",
        "AWSENDPOINT=${s3Endpoint}",
        "NAME_APP=${name}",
        "JAVA_VERSION=${javaVersion}",
        "PROFILE=${profile}",
        "server_name=${component}_${env}",
        "LATEST_OS_IMAGE=__latest_image__"
        ]) {

        def secrets = [
          [ path: 'kv/global', secretValues: [
            [vaultKey: 'XSF_S3_ACCESS_KEY', envVar:'AWS_ACCESS_KEY_ID'], 
            [vaultKey: 'XSF_S3_SECRET_KEY', envVar:'AWS_SECRET_ACCESS_KEY'],
            [vaultKey: 'ANSIBLE_SSH_KEY_BASE64'],
            [vaultKey: 'LOKI_OAT'],
            [vaultKey: 'LOKI_PROD'],
            [vaultKey: 'MIMIR_OAT'],   
            [vaultKey: 'MIMIR_PROD'],
            [vaultKey: 'TEMPO_OAT'],
            [vaultKey: 'TEMPO_PROD']
           // [vaultKey: 'shared_bucket_name']
            ]
          ],
          [ path: "kv/$trigram/$cloudEnv", secretValues: [
            [vaultKey: 'CLOUD_ACCOUNT_ID', envVar: 'ACCOUNT_ID'], 
            [vaultKey: 'CLOUD_CLIENT_ID', envVar: 'CLIENT_ID'], 
            [vaultKey: 'CLOUD_CLIENT_SECRET', envVar: 'CLIENT_SECRET'],
            [vaultKey: 'VAULT_ROLE_ID', envVar: 'VAULT_ROLE'], 
            [vaultKey: 'VAULT_SECRET_ID', envVar: 'VAULT_SECRET'],
            [vaultKey: 'VAULT_NAMESPACE', envVar: 'VAULT_NAMESPACE'],
            [vaultKey: 'VAULT_URL', envVar: 'VAULT_URL']
          ]],                   
        ]

        withVault([vaultSecrets: secrets]) {          
          stage_checkout_scripts(sharedLibVersion);
          if (use_latest_os_image) stage_latest_image(regions, server_name);
          //if (infra) stage_infra(regions, stateless, s3Endpoint, trigram, component, env, entity, terraformRoot);
	        //if (download_certificates) stage_certificate(regions, entity);
          //if (deplopyWithAnsible) stage_ansible(regions,trigram, component, env, name);
        }
      } 
    }
  }
}

def stage_checkout_scripts(sharedLibVersion) {
  stage('Checkout') {
    checkout([
      $class: 'GitSCM', 
      branches: [[name: "*/$sharedLibVersion"]], 
      extensions: [
        [$class: 'CloneOption', noTags: true, shallow: true, depth: 1, timeout: 30],
        [$class: 'RelativeTargetDirectory',  relativeTargetDir: '.'], 
      ],
      userRemoteConfigs: [[credentialsId: 'sgithub', url: "$DF_PIPELINE_REPO"]]
    ])
  }
}

def stage_latest_image(regions, server_name) {
  stage('Latest OCS Image') {
    container("python"){
      echo """
        ***********************************************************
        *             Get the OS image to be used ...             *             
        ***********************************************************
      """
      result = sh(script: """
      python ${WORKSPACE}/pipeline/lib/scripts/latest_os_factory.py $server_name '${groovy.json.JsonOutput.toJson(regions)}'
      """, returnStdout: true).trim()
      def lines = result.split("\n")
      LATEST_OS_IMAGE = lines[-1] 
    }
    echo"""
    $result
    """
    echo """
      *********************************************************************************
      *             The OCS Image that will be deployed: $LATEST_OS_IMAGE                          
      *********************************************************************************
    """
  }
}

def stage_infra(regions, stateless, s3Endpoint, trigram, component, env, entity, terraformRoot) {
  stage('Extract IPs') {
      for (def region in regions) {

        if (!stateless) {
          terraform_backend = "-backend-config=\"endpoint=${s3Endpoint}\"  -backend-config=\"bucket=xsf-af567-prd-cicd\" -backend-config=\"key=$trigram/$component/$env-$region/$entity/terraform/main.tfstate\""
        }

        container('terraform') {
          sh """
          echo off
          ls -alr
          echo +++++++++++++++++++++++ [${region}] +++++++++++++++++++++++

          cd ${WORKSPACE}/${terraformRoot}

          echo TRIGRAM=$TRIGRAM

          echo -----------------------TERRAFORM-INIT---------------------------------

          terraform init ${terraform_backend} 
          
          echo -----------------------TERRAFORM-APPLY---------------------------------
          
          export TF_VAR_infra_env=$env

          cp $PROJECT_ENV/main.tfvars "${PROJECT_ENV}/main_${region}.tfvars"


          sed -i 's|%region%|${region}|g' $PROJECT_ENV/main_${region}.tfvars
          sed -i 's|%entity%|${entity}|g' $PROJECT_ENV/main_${region}.tfvars
          sed -i 's|%env%|${env}|g' $PROJECT_ENV/main_${region}.tfvars

          if [ $region = 'paris' ]; then
            sed -i 's|__dns_aliases__|dns_aliases|g' $PROJECT_ENV/main_${region}.tfvars
          else
            sed -i 's|__dns_aliases__|dns_aliases_desactivated|g' $PROJECT_ENV/main_${region}.tfvars
          fi

          cat $PROJECT_ENV/main_${region}.tfvars
          echo -----------------------TERRAFORM-IP---------------------------------
          terraform plan -out=tfplan -no-color -var-file="$PROJECT_ENV/main_${region}.tfvars"
          terraform show -json tfplan > tfplan_${region}.json
          echo -----------------------TERRAFORM-CAT---------------------------------
          cat tfplan_${region}.json

          rm -rf .terraform*
          pwd
          ls -alr
          """
        }
      }    
    }
  stage('Infra') {
      for (def region in regions) {

        if (!stateless) {
          terraform_backend = "-backend-config=\"endpoint=${s3Endpoint}\"  -backend-config=\"bucket=xsf-af567-prd-cicd\" -backend-config=\"key=$trigram/$component/$env-$region/$entity/terraform/main.tfstate\""
        }
        echo LATEST_OS_IMAGE
        container('terraform') {
          sh """
          echo off
          ls -alr
          echo +++++++++++++++++++++++ [${region}] +++++++++++++++++++++++

          cd ${WORKSPACE}/${terraformRoot}

          echo TRIGRAM=$TRIGRAM

          export TF_VAR_infra_env=$env

          cp $PROJECT_ENV/main.tfvars "${PROJECT_ENV}/main_${region}.tfvars"


          sed -i 's|%region%|${region}|g' $PROJECT_ENV/main_${region}.tfvars
          sed -i 's|%entity%|${entity}|g' $PROJECT_ENV/main_${region}.tfvars
          sed -i 's|%env%|${env}|g' $PROJECT_ENV/main_${region}.tfvars

          if [ $ENV != 'prd' ]; then
            sed -i 's|%latest-os-image%|$LATEST_OS_IMAGE|g' $PROJECT_ENV/main_${region}.tfvars
          fi

          if [ $region = 'paris' ]; then
            sed -i 's|__dns_aliases__|dns_aliases|g' $PROJECT_ENV/main_${region}.tfvars
          else
            sed -i 's|__dns_aliases__|dns_aliases_desactivated|g' $PROJECT_ENV/main_${region}.tfvars
          fi
          cat $PROJECT_ENV/main_${region}.tfvars
          """
        }
      }    
    }
}



def stage_certificate(regions, entity) {
  stage('Generate Certificate') {
    for (def region in regions) {
      dir("${PROJECT_ENV}") {
        container('python') {
          sh """
          cat terraform_output_${region}.json
          python ${WORKSPACE}/pipeline/lib/scripts/ansible_inventory.py terraform_output_${region}.json inventory.ini
          cat inventory.ini
          """
        }
        container("python") {
          sh """
          cd ${PROJECT_ENV}
          sed -i 's|%entity%|${entity}|g' certificate.yml
          echo $PROJECT_ENV
          pwd
          python -u $LIB_DIR/certificate/main.py certificate.yml
          ls -alr
          """
        }
        container("python") {
          sh """
          cd ${PROJECT_ENV}
          python -u $LIB_DIR/certificate/download_certificates.py certificate.yml
          ls -alr
          """
        }
        container('ansible'){
          sh '''
          echo $ANSIBLE_SSH_KEY_BASE64 | base64 -d > id_rsa
          chmod 600 id_rsa
          pwd
          ls -alr
          ansible-playbook cert_playbook.yml --private-key id_rsa -u cloud-user -i inventory.ini
          rm -f id_rsa
          '''
        }
      }
    }
  }
}

def stage_ansible(regions,trigram, component, env, name) {
    stage('Deploy') {
        for (def region in regions) {
            dir("${PROJECT_ENV}") {
                container('python') {
                    // Write the token to a file in the Jenkins workspace
                    
                    sh """
                    python ${WORKSPACE}/pipeline/lib/scripts/ansible_inventory.py terraform_output_${region}.json inventory.ini
                    cat inventory.ini
                    python -u $LIB_DIR/jwks/download_jwks.py certificate.yml
                    ls -alr
                    python -u $LIB_DIR/scripts/s3_artifact_download.py certificate.yml
                    ls -alr
                    """
                }
                container('ansible') {
                    // Use the token in withEnv L
                    sh '''
                    echo this is $name
                    echo $ANSIBLE_SSH_KEY_BASE64 | base64 -d > id_rsa
                    chmod 600 id_rsa
                    if [[ "$CLOUD_ENV" == "prd" ]]; then 
                      ansible-playbook playbook.yml --private-key id_rsa -u cloud-user -i inventory.ini --extra-vars "NAME_APP=${NAME_APP} VAULT_URL=$VAULT_URL VAULT_ROLE=$VAULT_ROLE VAULT_SECRET=$VAULT_SECRET ENVI=$ENV LOKI=$LOKI_PROD TEMPO=$TEMPO_PROD MIMIR=$MIMIR_PROD VAULT_NAMESPACE=$VAULT_NAMESPACE COMPONEN=$COMPONENT GROUP_ID=${GROUP_ID} ARTIFACT_ID=${ARTIFACT_ID} ARTIFACT_VERSION=${ARTIFACT_VERSION} AWSS3ENDPOINT=${AWSENDPOINT} TRIGRAM=${TRIGRAM} COMPONENT=${COMPONENT} AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} PROFILE=${PROFILE} JAVA_VERSION=${JAVA_VERSION}"
                    else
                      ansible-playbook playbook.yml --private-key id_rsa -u cloud-user -i inventory.ini --extra-vars "shared_bucket_name=${shared_bucket_name} NAME_APP=${NAME_APP} VAULT_URL=$VAULT_URL VAULT_ROLE=$VAULT_ROLE VAULT_SECRET=$VAULT_SECRET ENVI=$ENV LOKI=$LOKI_OAT TEMPO=$TEMPO_OAT MIMIR=$MIMIR_OAT VAULT_NAMESPACE=$VAULT_NAMESPACE COMPONEN=$COMPONENT GROUP_ID=${GROUP_ID} ARTIFACT_ID=${ARTIFACT_ID} ARTIFACT_VERSION=${ARTIFACT_VERSION} AWSS3ENDPOINT=${AWSENDPOINT} TRIGRAM=${TRIGRAM} COMPONENT=${COMPONENT} AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} PROFILE=${PROFILE} JAVA_VERSION=${JAVA_VERSION}"
                    fi
                    rm -f id_rsa
                    '''
                }
                container('python') {
                  // Write the token to a file in the Jenkins workspace
                  def file_path = "$PROJECT_ENV/main.tfvars"

                  // Capture the output of the shell script
                  def region_value = sh(script: """
                      echo "${regions}" | tr -d '[]'
                  """, returnStdout: true).trim()

                  // Use the captured value in Groovy
                  echo "Service url: https://${trigram}-${component}-${env}-paris.afs.socgen"
              }
            }
        }
    } 
}




