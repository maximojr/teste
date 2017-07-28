node {
   def mvnHome
   stage('Baixar source') { // for display purposes
      checkout scm
	  
	  // save our docker build context before we switch branches
	  sh "cp -r ./.docker/build tmp-docker-build-context"
	  
      // ** NOTE: This 'M3' Maven tool must be configured
      // **       in the global configuration.           
      mvnHome = tool 'M3'
   }
   stage('Build') {
      
      if (isUnix()) {
         sh "'${mvnHome}/bin/mvn' -DskipTests clean package"
      } else {
         bat(/"${mvnHome}\bin\mvn" -DskipTests clean package/)
      }
   }
   stage('Testes unitários') {
       
       if (isUnix()) {
         sh "'${mvnHome}/bin/mvn' test"
      } else {
         bat(/"${mvnHome}\bin\mvn" test/)
      }
   }
   stage('Results') {
      //junit 'target/surefire-reports/TEST.xml'
      //archive 'target/*.jar'
      //step([$class: 'JUnitResultArchiver', testResults: 'target/TEST-*.xml'])
      echo '--- buil realizado ---'
   }
   stage('Testes não automatizados concluídos?'){
       script {
           env.CONFIRMA_TESTES_MANUAIS = input message: 'Escolha:',
           parameters: [choice(name: 'Os testes manuais foram concluídos com sucesso?', choices: 'não\nsim', description: 'Escolha')]
        }
   }
   stage('Testes não automatizados'){
       if (env.CONFIRMA_TESTES_MANUAIS!='sim'){
           echo '-- Testes manuais sem sucesso --'
           error 'Build não passou no teste manual!'
       } else {
           echo '-- Testes manuais OK --'
       }
   }
   stage('Confirmar deploy?'){
       script {
           env.CONFIRMA_DEPLOY = input message: 'Escolha:',
           parameters: [choice(name: 'Efetuar deploy', choices: 'não\nsim', description: 'Escolha "sim" se quer efetuar o deploy deste build')]
        }
   }
   stage('Deploy'){
       if (env.CONFIRMA_DEPLOY=='sim'){
		   
		  sh "cp /var/jenkins_home/workspace/pipeline_1/target/teste.war ./tmp-docker-build-context"
          
		  withDockerContainer([image: "tomcat:latest"]){
			  withDockerServer([uri: "tcp://192.168.33.11:4243"]) {
				 withDockerRegistry([url: "registry.discover.com.br"]) {
				   
				   // we give the image the same version as the .war package
				   def image = docker.build("registry.discover.com.br/tomcat_discover:1.0", "--build-arg PACKAGE_VERSION=1.0 ./tmp-docker-build-context")
				   image.push()
				 }
			   }
		  }
           
       } else {
           echo '-- não fazer deploy --'
       }
   }
}
