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
    }

    triggers {
        pollSCM('H/5 * * * *')  // check every 5 mins; better: use GitHub webhook
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
                    // Safer cleanup commands that don't fail if no containers/images exist
                    bat """
                        @echo off
                        echo "Stopping containers..."
                        docker ps -a -q --filter "ancestor=%DOCKER_IMAGE%:latest" > temp_containers.txt 2>nul
                        if exist temp_containers.txt (
                            for /f %%i in (temp_containers.txt) do (
                                echo Stopping container %%i
                                docker stop %%i 2>nul || echo Container %%i already stopped
                            )
                        )
                        del temp_containers.txt 2>nul

                        echo "Removing containers..."
                        docker ps -a -q --filter "ancestor=%DOCKER_IMAGE%:latest" > temp_containers.txt 2>nul
                        if exist temp_containers.txt (
                            for /f %%i in (temp_containers.txt) do (
                                echo Removing container %%i
                                docker rm %%i 2>nul || echo Container %%i already removed
                            )
                        )
                        del temp_containers.txt 2>nul

                        echo "Removing old images..."
                        docker images %DOCKER_IMAGE% -q > temp_images.txt 2>nul
                        if exist temp_images.txt (
                            for /f %%i in (temp_images.txt) do (
                                echo Removing image %%i
                                docker rmi -f %%i 2>nul || echo Image %%i already removed
                            )
                        )
                        del temp_images.txt 2>nul
                        echo "Cleanup completed"
                    """

                    // Build and push Docker image
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