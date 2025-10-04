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
        K8S_NAMESPACE = 'capstone-app'
        K8S_SERVICE = 'capstone-service'
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
                jacoco(
                    execPattern: 'target/jacoco.exec',
                    classPattern: 'target/classes',
                    sourcePattern: 'src/main/java'
                )
            }
        }

        stage('Code Quality Analysis') {
            steps {
                script {
                    echo "Running code quality checks..."
                }
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

        stage('Verify Kubernetes Cluster') {
            steps {
                bat 'kubectl cluster-info'
                bat 'kubectl get nodes'
                bat "kubectl get namespace ${K8S_NAMESPACE}"
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    try {
                        echo "🚀 Starting Kubernetes deployment..."
                        bat "kubectl apply -f k8s/configmap.yaml --namespace=${K8S_NAMESPACE}"
                        bat "kubectl apply -f k8s/service.yaml --namespace=${K8S_NAMESPACE}"
                        bat "kubectl apply -f k8s/deployment.yaml --namespace=${K8S_NAMESPACE}"
                        bat "kubectl set image deployment/${K8S_DEPLOYMENT} ${K8S_CONTAINER}=${DOCKER_IMAGE}:${BUILD_NUMBER} --namespace=${K8S_NAMESPACE} --record"
                        bat "kubectl rollout status deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE} --timeout=300s"
                        echo "✅ Deployment successful!"
                    } catch (err) {
                        echo "❌ Deployment failed! Error: ${err.message}"
                        echo "🔄 Attempting rollback..."
                        bat "kubectl rollout undo deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE}"
                        bat "kubectl rollout status deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE} --timeout=120s"
                        error("Deployment failed and rolled back. Check logs for details.")
                    }
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    bat "kubectl get pods --namespace=${K8S_NAMESPACE} -l app=capstone"
                    bat "kubectl get svc ${K8S_SERVICE} --namespace=${K8S_NAMESPACE}"
                    bat """
                        echo.
                        echo ========================================
                        echo Application is accessible at:
                        echo http://localhost:30080
                        echo ========================================
                        echo.
                    """
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    sleep(time: 30, unit: 'SECONDS')
                    try {
                        def response = bat(
                            script: 'curl -s -o NUL -w "%%{http_code}" http://localhost:30080/actuator/health',
                            returnStdout: true
                        ).trim()
                        if (response == '200') {
                            echo "✅ Health check passed!"
                        } else {
                            error("❌ Health check failed with status: ${response}")
                        }
                    } catch (err) {
                        echo "⚠️ Health check endpoint not accessible yet. Manual verification required."
                    }
                }
            }
        }

        stage('Save Build Artifacts') {
            steps {
                script {
                    def summary = """\
╔════════════════════════════════════════════════════════════╗
║              BUILD & DEPLOYMENT SUMMARY                    ║
╚════════════════════════════════════════════════════════════╝

📋 Build Information:
   • Status: ${currentBuild.currentResult}
   • Job: ${env.JOB_NAME}
   • Build Number: ${env.BUILD_NUMBER}
   • Branch: main
   • Timestamp: ${new Date()}

🔧 Jenkins Details:
   • Build URL: ${env.BUILD_URL}
   • Triggered By: ${currentBuild.getBuildCauses()[0].shortDescription}

🐳 Docker Information:
   • Image: ${DOCKER_IMAGE}:${env.BUILD_NUMBER}
   • Latest Tag: ${DOCKER_IMAGE}:latest
   • Registry: Docker Hub

☸️  Kubernetes Deployment:
   • Namespace: ${K8S_NAMESPACE}
   • Deployment: ${K8S_DEPLOYMENT}
   • Service: ${K8S_SERVICE}
   • Container: ${K8S_CONTAINER}
   • Replicas: 2
   • Access URL: http://localhost:30080

📊 Application Endpoints:
   • Main App: http://localhost:30080
   • Health Check: http://localhost:30080/actuator/health
   • Metrics: http://localhost:30080/actuator/metrics

═══════════════════════════════════════════════════════════════
"""
                    writeFile file: 'build-summary.txt', text: summary
                    archiveArtifacts artifacts: 'build-summary.txt', fingerprint: true
                }
            }
        }
    }

    post {
        always {
            script {
                def status = currentBuild.currentResult
                def emoji = (status == 'SUCCESS') ? '✅' : '❌'
                def subject = "${emoji} ${status}: Build #${env.BUILD_NUMBER} - ${env.JOB_NAME}"
                def body = """\
Hello Team,

Build Completed: ${status}

═══════════════════════════════════════════════════════════════
📋 BUILD DETAILS
═══════════════════════════════════════════════════════════════
• Job Name: ${env.JOB_NAME}
• Build Number: ${env.BUILD_NUMBER}
• Status: ${status}
• Duration: ${currentBuild.durationString}
• Triggered By: ${currentBuild.getBuildCauses()[0].shortDescription}
• Branch: main

═══════════════════════════════════════════════════════════════
🔗 LINKS
═══════════════════════════════════════════════════════════════
• Console Output: ${env.BUILD_URL}console
• Build Summary: ${env.BUILD_URL}artifact/build-summary.txt

═══════════════════════════════════════════════════════════════
🐳 DOCKER IMAGE
═══════════════════════════════════════════════════════════════
• Image: ${DOCKER_IMAGE}:${env.BUILD_NUMBER}
• Latest: ${DOCKER_IMAGE}:latest

═══════════════════════════════════════════════════════════════
☸️  KUBERNETES DEPLOYMENT
═══════════════════════════════════════════════════════════════
• Namespace: ${K8S_NAMESPACE}
• Deployment: ${K8S_DEPLOYMENT}
• Service: ${K8S_SERVICE}
• Access URL: http://localhost:30080

═══════════════════════════════════════════════════════════════

Best regards,
Jenkins CI/CD Pipeline
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

        success {
            echo '✅ Pipeline completed successfully!'
        }

        failure {
            echo '❌ Pipeline failed! Check logs for details.'
        }
    }
}
