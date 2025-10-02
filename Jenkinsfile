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
        K8S_DEPLOYMENT = 'capstone-deployment'
        K8S_CONTAINER = 'capstone-container'
        K8S_NAMESPACE = 'default'
        KUBECONFIG = 'C:\\Users\\Prasanth Golla\\.kube\\config'
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
                    bat """
                        docker build -t %DOCKER_IMAGE%:%BUILD_NUMBER% .
                        docker tag %DOCKER_IMAGE%:%BUILD_NUMBER% %DOCKER_IMAGE%:latest
                    """

                    docker.withRegistry('https://index.docker.io/v1/', "${DOCKERHUB_CREDENTIALS}") {
                        def app = docker.image("${DOCKER_IMAGE}:${BUILD_NUMBER}")
                        app.push()
                        app.push("latest")
                    }
                }
            }
        }
        stage('Test Kubectl Access') {
            steps {
                bat 'kubectl get nodes'
                bat 'kubectl get svc'
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    try {
                        // Apply manifests if deployment does not exist
                        bat "kubectl apply -f k8s/"

                        // Update deployment with new image
                        bat "kubectl set image deployment/%K8S_DEPLOYMENT% %K8S_CONTAINER%=%DOCKER_IMAGE%:%BUILD_NUMBER% --record"

                        // Wait for rollout status (checks if deployment successful)
                        bat "kubectl rollout status deployment/%K8S_DEPLOYMENT% --namespace=%K8S_NAMESPACE% --timeout=60s"
                    } catch (err) {
                        echo "❌ Deployment failed! Rolling back..."
                        bat "kubectl rollout undo deployment/%K8S_DEPLOYMENT% --namespace=%K8S_NAMESPACE%"
                        error("Deployment failed and rolled back")
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
- Kubernetes Deployment: ${K8S_DEPLOYMENT}
- Container: ${K8S_CONTAINER}
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
- Kubernetes Deployment: ${K8S_DEPLOYMENT}
- Container: ${K8S_CONTAINER}

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
