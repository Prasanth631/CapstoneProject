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
        
        // Kubernetes credentials ID (from Jenkins credentials)
        KUBERNETES_CREDENTIALS_ID = 'kubernetes-config'
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                echo '📥 Checking out code from GitHub...'
                git url: 'https://github.com/Prasanth631/CapstoneProject.git', branch: 'main'
                echo '✅ Code checkout complete'
            }
        }

        stage('Build & Test') {
            steps {
                echo '🔨 Building application with Maven...'
                bat 'mvn clean install'
                echo '✅ Build and tests complete'
            }
        }

        stage('Publish Test Results') {
            steps {
                echo '📊 Publishing test results...'
                junit 'target/surefire-reports/*.xml'
                echo '✅ Test results published'
            }
        }

        stage('Docker Build & Push') {
            steps {
                echo '🐳 Building Docker image...'
                script {
                    bat """
                        docker build -t %DOCKER_IMAGE%:%BUILD_NUMBER% .
                        docker tag %DOCKER_IMAGE%:%BUILD_NUMBER% %DOCKER_IMAGE%:latest
                    """
                    
                    echo "✅ Docker image built: ${DOCKER_IMAGE}:${BUILD_NUMBER}"
                    
                    echo '📤 Pushing image to Docker Hub...'
                    docker.withRegistry('https://index.docker.io/v1/', "${DOCKERHUB_CREDENTIALS}") {
                        def app = docker.image("${DOCKER_IMAGE}:${BUILD_NUMBER}")
                        app.push()
                        app.push("latest")
                    }
                    
                    echo '✅ Docker image pushed to Docker Hub'
                }
            }
        }

        stage('Verify Kubernetes Cluster') {
            steps {
                echo '🔍 Verifying Kubernetes cluster connectivity...'
                script {
                    // Using Kubernetes credentials
                    withKubeConfig([credentialsId: "${KUBERNETES_CREDENTIALS_ID}"]) {
                        bat """
                            echo Checking Kubernetes context...
                            kubectl config current-context
                            
                            echo.
                            echo Kubernetes Cluster Info:
                            kubectl cluster-info
                            
                            echo.
                            echo Checking Kubernetes nodes:
                            kubectl get nodes
                            
                            echo.
                            echo Checking namespace:
                            kubectl get namespace ${K8S_NAMESPACE}
                        """
                    }
                }
                echo '✅ Kubernetes cluster verification complete'
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo "🚀 Starting Kubernetes deployment to namespace: ${K8S_NAMESPACE}"
                script {
                    // Using Kubernetes credentials for deployment
                    withKubeConfig([credentialsId: "${KUBERNETES_CREDENTIALS_ID}"]) {
                        try {
                            bat """
                                echo.
                                echo [Step 1/4] Applying ConfigMap...
                                kubectl apply -f k8s/configmap.yaml --namespace=${K8S_NAMESPACE}
                                
                                echo.
                                echo [Step 2/4] Applying Service...
                                kubectl apply -f k8s/service.yaml --namespace=${K8S_NAMESPACE}
                                
                                echo.
                                echo [Step 3/4] Applying Deployment...
                                kubectl apply -f k8s/deployment.yaml --namespace=${K8S_NAMESPACE}
                                
                                echo.
                                echo [Step 4/4] Updating deployment image to build ${BUILD_NUMBER}...
                                kubectl set image deployment/${K8S_DEPLOYMENT} ${K8S_CONTAINER}=${DOCKER_IMAGE}:${BUILD_NUMBER} --namespace=${K8S_NAMESPACE} --record
                                
                                echo.
                                echo Waiting for rollout to complete...
                                kubectl rollout status deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE} --timeout=300s
                            """
                            
                            echo '✅ Deployment successful!'
                            
                        } catch (err) {
                            echo "❌ Deployment failed! Error: ${err.message}"
                            echo '🔄 Attempting automatic rollback...'
                            
                            bat """
                                kubectl rollout undo deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE}
                                kubectl rollout status deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE} --timeout=120s
                            """
                            
                            error("Deployment failed and has been rolled back to previous version")
                        }
                    }
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                echo '🔍 Verifying deployment status...'
                script {
                    withKubeConfig([credentialsId: "${KUBERNETES_CREDENTIALS_ID}"]) {
                        bat """
                            echo.
                            echo ========================================
                            echo Deployment Verification
                            echo ========================================
                            
                            echo.
                            echo Pods in namespace ${K8S_NAMESPACE}:
                            kubectl get pods --namespace=${K8S_NAMESPACE} -l app=capstone -o wide
                            
                            echo.
                            echo Service details:
                            kubectl get svc ${K8S_SERVICE} --namespace=${K8S_NAMESPACE}
                            
                            echo.
                            echo Endpoints (Pod IPs):
                            kubectl get endpoints ${K8S_SERVICE} --namespace=${K8S_NAMESPACE}
                            
                            echo.
                            echo Deployment status:
                            kubectl get deployment ${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE}
                            
                            echo.
                            echo ========================================
                            echo Application Access Information
                            echo ========================================
                            echo Application URL: http://localhost:30080
                            echo ========================================
                        """
                    }
                }
                echo '✅ Deployment verification complete'
            }
        }

        stage('Health Check') {
            steps {
                echo '🏥 Running application health check...'
                script {
                    echo 'Waiting 30 seconds for application to stabilize...'
                    sleep(time: 30, unit: 'SECONDS')
                    
                    try {
                        echo 'Testing application endpoint...'
                        def response = bat(
                            script: 'curl -s -o NUL -w "%%{http_code}" http://localhost:30080',
                            returnStdout: true
                        ).trim()
                        
                        echo "HTTP Response Code: ${response}"
                        
                        if (response == '200') {
                            echo "✅ Health check PASSED! Application is responding correctly."
                        } else {
                            echo "⚠️ Warning: Unexpected response code: ${response}"
                        }
                    } catch (err) {
                        echo "⚠️ Health check could not be completed automatically."
                        echo "Please verify manually at: http://localhost:30080"
                    }
                }
                echo '✅ Health check stage complete'
            }
        }

        stage('Save Build Summary') {
            steps {
                echo '💾 Saving build summary...'
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
   • Duration: ${currentBuild.durationString}

🔧 Jenkins Details:
   • Build URL: ${env.BUILD_URL}
   • Console Output: ${env.BUILD_URL}console
   • Triggered By: ${currentBuild.getBuildCauses()[0].shortDescription}

🐳 Docker Information:
   • Image: ${DOCKER_IMAGE}:${env.BUILD_NUMBER}
   • Latest Tag: ${DOCKER_IMAGE}:latest

☸️  Kubernetes Deployment:
   • Namespace: ${K8S_NAMESPACE}
   • Deployment: ${K8S_DEPLOYMENT}
   • Service: ${K8S_SERVICE}
   • Container: ${K8S_CONTAINER}

📊 Application Endpoints:
   • Main Application: http://localhost:30080
   • Index Page: http://localhost:30080/index.html

═══════════════════════════════════════════════════════════════
"""
                    writeFile file: 'build-summary.txt', text: summary
                    archiveArtifacts artifacts: 'build-summary.txt', fingerprint: true
                }
                echo '✅ Build summary saved'
            }
        }
    }

    post {
        always {
            script {
                echo '📧 Sending email notification...'
                
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

═══════════════════════════════════════════════════════════════
🔗 LINKS
═══════════════════════════════════════════════════════════════
• Console Output: ${env.BUILD_URL}console

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
                
                echo '✅ Email notification sent'
            }
        }
        
        success {
            echo '✅✅✅ Pipeline completed successfully! ✅✅✅'
            echo "Application is now running at: http://localhost:30080"
        }
        
        failure {
            echo '❌❌❌ Pipeline failed! ❌❌❌'
            echo 'Please check the console output for details.'
        }
    }
}