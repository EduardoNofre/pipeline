def iniciarDeploy(gitUrl){ 

      echo " -------------------------------------- "
      echo " ----------- INICIAR DEPLOY BUILD ----- "
      echo " -------------------------------------- "
	  
 try {
		 stage("Build") {
			deployApp(gitUrl)
		 }
		 
		 stage("Code Quality - Sonar") {
			codeQuality()
		 }
		 
		 stage("Quality Gate") {
			qualityGate()
		 }

	 	stage("Build Docker") { 
	 		buildDocker(gitUrl) 
		}
	 
		 stage("Publish Container") {  
			publishContainer()
		 }

 	} finally {
    println currentBuild.result // this prints null
    step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: 'nofrereis@gmail.com', sendToIndividuals: true])
  }
}


def deployApp(gitUrl) {

      echo " -------------------------------------- "
      echo " ----------- STEP INICIO BUILD -------- "
      echo " -------------------------------------- "

      	// URL REPOSITORIO GIT
      	git url: "${gitUrl}",

        // NOME DA BRANCH PROD,DEV,QA...etctra
        branch: "develop",

        // ---------------------------------------------------------------------------------------------- 
        // TOKEN CRIADO NO GIT E ASSOCIADO AO JNEKINS
        // DEVE SER CRIAR UMA CREDENCIAL USANDO USUARIO E SENHA  DO GITHUB :NAME 'github-jenkin'
        // ---------------------------------------------------------------------------------------------- 
        credentialsId: "github-jenkins"

      	// NOME PROPERTIES DA APLICAÇAO.
      	def nomeProperties = "application.properties"

      	// ---------------------------------------------------------------------------------------------- 
      	// PARA USAR O COMANDO READMAVENPOM DEVE INSTALAR O PLUGINS O 'PIPELINE UTILITY STEPS' NO JENKINS 
      	// ----------------------------------------------------------------------------------------------  

      	def pom = readMavenPom file: 'pom.xml'

      	if (pom) {
        	echo "Building Version ${pom.version}"
      	} else {
        	echo "Impossible to get the project properties. Ensure your pom exists."
        	sh "exit 1"
      }

      if (nomeProperties == null) {
        echo "Eh obrigatorio definir o nome do arquivo de configuracao (yml/properties)."
        sh "exit 1"
      }

      // ---------------------------------------------------------------------------------------------- 
      // CONFGIRUAR O JAVA HOME NO MAQUINA ONDE ESTA INSTALADO O JENKINS NO MEU CASO 'JAVA_HOME_11' JRE FOI CONFIGURADO VARIAVEL DE AMBIENTE JAVA 
      // CONFGIRUAR O MAVEM HOME NO MAQUINA ONDE ESTA INSTALADO O JENKINS NO MEU CASO 'M3' FOI CONFIGURADO INSTALAR O MAVEM NA MAQUINA VARIAVEL DE AMBIENTE MAVEN
      // ---------------------------------------------------------------------------------------------- 
      withEnv(["JAVA_HOME=${ tool 'JAVA_HOME_11' }", "PATH+MAVEN=${tool 'M3'}/bin:${env.JAVA_HOME}/bin"]) {
        sh "rm -f ${pwd()}/src/main/resources/${nomeProperties}"
        sh "mvn --batch-mode -V -U -e clean org.jacoco:jacoco-maven-plugin:prepare-agent verify -Dsurefire.useFile=false -Dskip.failsafe.tests=true"
      }

      if (pom.packaging == 'jar') {
        archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true, onlyIfSuccessful: true
      }

      if (pom.packaging == 'war') {
        archiveArtifacts artifacts: '**/target/*.war', fingerprint: true, onlyIfSuccessful: true
      }
      junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'

      echo " -------------------------------------- "
      echo " ----------- STEP FIM BUILD ----------- "
      echo " -------------------------------------- "
    }

def codeQuality() {

      echo " --------------------------------------------------- "
      echo " ----------- STEP INICIO SONAR CODE QUALITY -------- "
      echo " --------------------------------------------------- "

      withEnv(["JAVA_HOME=${tool 'JAVA_HOME_11'}", "PATH+MAVEN=${tool 'M3'}/bin:${env.JAVA_HOME}/bin"]) {

        // --------------------------------------------------------------------------------------------------------------------------------------------------------- 
        // TOKEN 'SECRET TEXT' CRIADO NO SONAR E ASSOCIADO AO JNEKINS
        // STEP-01: CAMINHO PIPELINE SYNTAX : PAINEL DE CONTROLE> OCR-BRASIL> DIGITAL-CONFIG-SERVICE> PIPELINE SYNTAX ACHAR A SEÇÃO  'Sample Step'
        //		 opção: withSonarQubeEnv : DEVE SER CRIAR UMA CREDENCIAL USANDO SECRET TEXT DO SONAR :NAME 'SONAR_SERVER'
        // --------------------------------------------------------------------------------------------------------------------------------------------------------- 
        withCredentials([usernamePassword(
          credentialsId: 'SONAR_PEPILINE',
          passwordVariable: 'SONAR_PASS',
          usernameVariable: 'SONAR_USER')]) {

          //----------------------------------------------------------------------------------------------------------------------------------------------------------
          // TOKEN 'SECRET TEXT' CRIADO NO SONAR E ASSOCIADO AO JNEKINS
          // STEP-02: CAMINHO SONAR SERVE DENTRO DO JENKINS : PAINEL DE CONTROLE > GERENCIAR JENKINS > SYSTEM, ACHAR A SEÇÃO  'SONARQUBE SERVERS'
          // 		  DEVE SER CRIAR UMA CREDENCIAL USANDO SECRET TEXT DO SONAR :NAME 'SONAR_SERVER'   
          // --------------------------------------------------------------------------------------------------------------------------------------------------------- 
          withSonarQubeEnv('SONAR_SERVER') {
            sh "mvn --batch-mode -V -U -e ${SONAR_MAVEN_GOAL} -Dsonar.host.url=${SONAR_HOST_URL} -Dsonar.login=${SONAR_USER} -Dsonar.password=${SONAR_PASS}"
            def props = readProperties file: 'target/sonar/report-task.txt'
            env.SONAR_CE_TASK_URL = props['ceTaskUrl']
            echo env.SONAR_CE_TASK_URL
          }
        }
      }

      echo " --------------------------------------------------- "
      echo " ----------- STEP FIM SONAR CODE QUALITY ----------- "
      echo " --------------------------------------------------- "
    }

 def qualityGate() {

      echo " --------------------------------------------------- "
      echo " -------------- STEP INICIO QUALITY GATE ----------- "
      echo " --------------------------------------------------- "

      withSonarQubeEnv('SONAR_SERVER') {
        def ceTask
        timeout(time: 1, unit: 'MINUTES') {
          waitUntil {
            sh 'curl -u $SONAR_AUTH_TOKEN $SONAR_CE_TASK_URL -o ceTask.json'
            ceTask = readJSON file: 'ceTask.json'
            echo ceTask.toString()
            return "SUCCESS".equals(ceTask["task"]["status"])
          }
        }
        def qualityGateUrl = env.SONAR_HOST_URL + "/api/qualitygates/project_status?analysisId=" + ceTask["task"]["analysisId"]
        echo qualityGateUrl
        sh "curl -u $SONAR_AUTH_TOKEN $qualityGateUrl -o qualityGate.json"
        def qualitygate = readJSON file: 'qualityGate.json'
        echo qualitygate.toString()
        if ("ERROR".equals(qualitygate["projectStatus"]["status"])) {
          error "Quality Gate failure - check Sonar !!! "
        }
        echo "Quality Gate success"
      }

      echo " --------------------------------------------------- "
      echo " -------------- STEP FIM QUALITY GATE -------------- "
      echo " --------------------------------------------------- "
    }

def buildDocker(gitUrl) {
	def nomeImagem = gitUrl.tokenize('/').last().split("\\.")[0]
	echo "NOME DA IMAGEM QUE SERA PUBLICADA NO DOCKER"
	echo "${nomeImagem}"
}
	
 def publishContainer() {
    
      echo " ---------------------------------------------------------- "
      echo " ------ INICIO PUBLISH CONTAINER ${params.profile}  ------- "
      echo " ---------------------------------------------------------- "
    
     // ---------------------------------------------------------------------------------------------- 
     // PARA USAR O COMANDO SSHAGENT DEVE INSTALAR O PLUGINS O 'SSH-AGENT' NO JENKINS 
     // opção: SSHAGENT : DEVE SER CRIAR UMA CREDENCIAL USANDO SSH  DO SONAR :NAME 'ACESSO_REMOTO_SSH'
     // ---------------------------------------------------------------------------------------------- 
     sshagent(['ACESSO_REMOTO_SSH']) {   	
    	echo " ----------------------------------------------------------------------- "
    	echo " -------------- Publicar no ambiente de ${params.profile} -------------- "
    	echo " ----------------------------------------------------------------------- "
        	 metodoDeployServer() 
      		 currentBuild.result = 'SUCCESS'
       	}
    }


def metodoDeployServer() {
	
 echo " ----------- PROFILE ${params.profile}  ------------"
 echo " --------- SERVIDOR ${params.servidor}  ------------"

  def ambiente = "${params.profile}"
  def server = "${params.servidor}"
  def nomeJar = "digital-config-service.jar"
  def nomeProperties = "application.properties"
  def origemDir = "${pwd()}/target"
  def destinoDir = "/java/springboot/digital/digital-config-service"
  def msgObjetivo = "Objetivo";
  def msgObjetivo1 = "- Publicar o pacote: ${nomeJar} para ${destinoDir}"
  def userNameServer = "ubuntu"
	
  echo "Iniciando publicação em [${ambiente}] com o usuário: ${userNameServer} no servidor: ${server}"

  withEnv(["JAVA_HOME=${ tool 'JAVA_HOME_11' }", "PATH+MAVEN=${tool 'M3'}/bin:${env.JAVA_HOME}/bin"]) {

     stopService(userNameServer,server)
	 transferFile(nomeJar,origemDir,destinoDir,userNameServer,server) 
	 startService(userNameServer,server,nomeJar)
  }
  echo "Fim da publicação em [${ambiente}] "
  echo ""
}

def stopService(userNameServer,server) {

    echo " ----------------------------------------------------------------- "
    echo " -------------- STOP SERVICE DIGITAL-CONFIG-SERVICE -------------- "
    echo " ----------------------------------------------------------------- "
    sh "ssh -tt -o StrictHostKeyChecking=no ${userNameServer}@${server} sudo systemctl stop digital-config-service.service"
   // sh "sshpass ssh ${userNameServer}@${server} sudo systemctl stop digital-config-service.service"
    sh "sleep 5"
}

def transferFile(nomeJar,origemDir,destinoDir,userNameServer,server) {

    echo " --------------------------------------------------------------------------- "
    echo " ------------------------ MOVENDO O ARQUIVO -------------------------------- "
    echo " --------------------------------------------------------------------------- "
    echo " ----------- ARTERFATO ${nomeJar}  ------------"
    echo " ----------- ORIGEM ${origemDir}   ------------"
    echo " ----------- DESTINO ${destinoDir} ------------"

    sh "scp ${origemDir}/${nomeJar} ${userNameServer}@${server}:${destinoDir}/"

    echo " ----------------------------------------------------------------------------------- "
    echo " ---------------------------- TRANSFERIDO COM SUCESSO ------------------------------ "
    echo " ----------------------------------------------------------------------------------- "
}

def startService(userNameServer,server,nomeJar) {

    echo " ----------------------------------------------------------------------------------- "
    echo " -------------- INICIALIZANO O SERVIÇO  DIGITAL-CONFIG-SERVICE.SERICE -------------- "
    echo " ----------------------------------------------------------------------------------- "

    sh "ssh -tt -o StrictHostKeyChecking=no ${userNameServer}@${server} sudo systemctl start digital-config-service.service"
    echo "Pacote ${nomeJar} publicado com sucesso."
}

return this
