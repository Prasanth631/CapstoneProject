pipeline {
    agent any

    tools {
        maven 'MAVEN_HOME'
        jdk 'JDK11'
    }

    environment {
        EMAIL_RECIPIENTS = '2200030631cseh@gmail.com'
        DOCKERHUB_CREDENTIALS = 'docker-hub-creds'
        DOCKER_IMAGE = 'prasanth631/capstone_pro'
        CONTAINER_NAME = 'capstone_app'
        HOST_PORT = '9090'
        CONTAINER_PORT = '8080'
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/Prasanth631/CapstoneProject.git', branch: 'main'
            }
        }

        stage('Build & Test') {
            steps {
                bat 'mvn clean install'
            }
        }

        stage('Publish Test Results') {
            steps {
                junit 'target/surefire-reports/*.xml'
            }
        }

        stage('Docker Cleanup & Build') {
            steps {
                script {
                    bat """
                        @echo off
                        echo Stopping old container if exists...
                        docker stop %CONTAINER_NAME% 2>nul || echo No container to stop
                        docker rm %CONTAINER_NAME% 2>nul || echo No container to remove
                        
                        echo Removing old images...
                        docker rmi -f %DOCKER_IMAGE%:latest 2>nul || echo No old image to remove
                        echo Cleanup completed
                    """

                    docker.withRegistry('https://index.docker.io/v1/', "${DOCKERHUB_CREDENTIALS}") {
                        def app = docker.build("${DOCKER_IMAGE}:${env.BUILD_NUMBER}")
                        app.push()
                        app.push("latest")
                    }
                }
            }
        }

        stage('Deploy New Container') {
            steps {
                script {
                    bat """
                        @echo off
                        echo Running new container on port %HOST_PORT%...
                        docker run -d --name %CONTAINER_NAME% -p %HOST_PORT%:%CONTAINER_PORT% %DOCKER_IMAGE%:latest
                    """
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
- Container: ${CONTAINER_NAME} (running on port ${HOST_PORT})
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
- Running Container: ${CONTAINER_NAME} (http://localhost:${HOST_PORT})

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
