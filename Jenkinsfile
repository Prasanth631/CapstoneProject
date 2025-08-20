pipeline {
    agent any

    tools {
        maven 'MAVEN_HOME'
        jdk 'JDK17' // Updated to match Dockerfile
    }

    environment {
        EMAIL_RECIPIENTS = '2200030631cseh@gmail.com'
        DOCKERHUB_CREDENTIALS = 'docker-hub-creds'
        DOCKER_IMAGE = 'prasanth631/capstone_pro'
        DOCKER_BUILDKIT = '1' // Enable BuildKit for better platform support
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

        stage('Docker Cleanup') {
            steps {
                script {
                    // Enhanced cleanup with better error handling
                    bat '''
                        @echo off
                        echo "=== Docker Cleanup Phase ==="
                        
                        echo "Stopping running containers..."
                        for /f "delims=" %%i in ('docker ps -q --filter "ancestor=%DOCKER_IMAGE%:latest" 2^>nul') do (
                            echo Stopping container %%i
                            docker stop %%i
                        )
                        
                        echo "Removing stopped containers..."
                        for /f "delims=" %%i in ('docker ps -a -q --filter "ancestor=%DOCKER_IMAGE%" 2^>nul') do (
                            echo Removing container %%i
                            docker rm %%i
                        )
                        
                        echo "Removing old images..."
                        for /f "delims=" %%i in ('docker images %DOCKER_IMAGE% -q 2^>nul') do (
                            echo Removing image %%i
                            docker rmi -f %%i
                        )
                        
                        echo "Cleanup completed successfully"
                        exit /b 0
                    '''
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                script {
                    try {
                        // Build with explicit platform
                        bat "docker build --platform linux/amd64 -t ${DOCKER_IMAGE}:${env.BUILD_NUMBER} -t ${DOCKER_IMAGE}:latest ."
                        
                        // Push to DockerHub
                        docker.withRegistry('https://index.docker.io/v1/', "${DOCKERHUB_CREDENTIALS}") {
                            bat "docker push ${DOCKER_IMAGE}:${env.BUILD_NUMBER}"
                            bat "docker push ${DOCKER_IMAGE}:latest"
                        }
                        
                        echo "Docker build and push completed successfully"
                        
                    } catch (Exception e) {
                        echo "Docker build failed: ${e.getMessage()}"
                        throw e
                    }
                }
            }
        }

        stage('Verify Docker Image') {
            steps {
                script {
                    // Verify the image was created successfully
                    bat "docker inspect ${DOCKER_IMAGE}:${env.BUILD_NUMBER}"
                    echo "Docker image verification completed"
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
- Platform: linux/amd64
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
- Platform: linux/amd64

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
        
        failure {
            script {
                // Additional debugging info on failure
                bat '''
                    echo "=== DEBUG INFO ==="
                    docker version
                    docker images
                    docker ps -a
                '''
            }
        }
    }
}