pipeline {
    agent any

    tools {
        maven 'MAVEN_HOME'
        jdk 'JDK11'
    }

    environment {
        EMAIL_RECIPIENTS = '2200030631cseh@gmail.com'
        DOCKERHUB_CREDENTIALS = 'docker-hub-creds'  // Jenkins credential ID
        DOCKER_IMAGE = 'prasanth631/capstone-app'
    }

    triggers {
        pollSCM('* * * * *') 
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/Prasanth631/CapstoneProject.git', branch: 'main'
            }
        }

        stage('Build & Test') {
            steps {
                bat 'mvn -version'
                bat 'mvn clean install'
            }
        }

        stage('Publish Test Results') {
            steps {
                junit 'target/surefire-reports/*.xml'
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    // Clean up old containers and images for this app
                    bat """
                        docker ps -a -q --filter "ancestor=%DOCKER_IMAGE%:latest" | findstr . && docker stop $(docker ps -a -q --filter "ancestor=%DOCKER_IMAGE%:latest") || echo No running containers
                        docker ps -a -q --filter "ancestor=%DOCKER_IMAGE%:latest" | findstr . && docker rm $(docker ps -a -q --filter "ancestor=%DOCKER_IMAGE%:latest") || echo No old containers
                        docker images %DOCKER_IMAGE% --format "{{.ID}}" | findstr . && docker rmi -f $(docker images %DOCKER_IMAGE% --format "{{.ID}}") || echo No old images
                    """

                    docker.withRegistry('https://index.docker.io/v1/', "${DOCKERHUB_CREDENTIALS}") {
                        def app = docker.build("${DOCKER_IMAGE}:${env.BUILD_NUMBER}")
                        app.push()
                        app.push("latest")
                    }
                }
            }
        }

        stage('Save Build Summary') {
            steps {
                script {
                    def summary = """\
BUILD SUMMARY
--------------
- Status: ${currentBuild.currentResult}
- Job: ${env.JOB_NAME}
- Build Number: ${env.BUILD_NUMBER}
- Branch: main
- Jenkins URL: ${env.BUILD_URL}
- Triggered By: ${currentBuild.getBuildCauses()[0].shortDescription}
- Docker Image: ${DOCKER_IMAGE}:${env.BUILD_NUMBER}
"""
                    writeFile file: 'build-summary.txt', text: summary
                }
            }
        }
    }

    post {
        always {
            script {
                def status = currentBuild.currentResult
                def subject = (status == 'SUCCESS') ? "✅ SUCCESS: Build #${env.BUILD_NUMBER}" : "❌ FAILURE: Build #${env.BUILD_NUMBER}"
                def body = """\
Hello Team,

The build has completed with the following status:

- Status: ${status}
- Job: ${env.JOB_NAME}
- Build Number: ${env.BUILD_NUMBER}
- Triggered By: ${currentBuild.getBuildCauses()[0].shortDescription}
- Branch: main
- View Console Output: ${env.BUILD_URL}console
- Docker Image: ${DOCKER_IMAGE}:${env.BUILD_NUMBER}

The detailed summary is attached.

Regards,
Jenkins
"""
                emailext(
                    to: "${EMAIL_RECIPIENTS}",
                    subject: subject,
                    body: body,
                    attachmentsPattern: 'build-summary.txt',
                    mimeType: 'text/plain'
                )
            }
        }
    }
}
