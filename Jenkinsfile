pipeline {
    agent any

    tools {
        maven 'MAVEN_HOME'
        jdk 'JDK11'
    }

    environment {
        EMAIL_RECIPIENTS     = '2200030631cseh@gmail.com'
        DOCKERHUB_CREDENTIALS = 'docker-hub-creds'
        DOCKER_IMAGE         = 'prasanth631/capstone_pro'
        K8S_DEPLOYMENT       = 'capstone-deployment'
        K8S_CONTAINER        = 'capstone-container'
        K8S_NAMESPACE        = 'capstone-app'
        K8S_SERVICE          = 'capstone-service'
        
        // ✅ Ensure Jenkins knows where kubeconfig is (Windows path with double slashes)
        KUBECONFIG = "${env.USERPROFILE}\\.kube\\config"
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
                script {
                    echo "Checking Kubernetes cluster connectivity..."
                    
                    bat """
                        set KUBECONFIG=%KUBECONFIG%
                        kubectl config current-context
                        kubectl cluster-info
                        kubectl get nodes -o wide
                    """
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    try {
                        echo "🚀 Deploying to namespace: ${K8S_NAMESPACE}"
                        
                        bat """
                            set KUBECONFIG=%KUBECONFIG%

                            kubectl apply -f k8s/configmap.yaml --namespace=${K8S_NAMESPACE}
                            kubectl apply -f k8s/service.yaml --namespace=${K8S_NAMESPACE}
                            kubectl apply -f k8s/deployment.yaml --namespace=${K8S_NAMESPACE}

                            kubectl set image deployment/${K8S_DEPLOYMENT} ${K8S_CONTAINER}=${DOCKER_IMAGE}:${BUILD_NUMBER} --namespace=${K8S_NAMESPACE} --record
                            kubectl rollout status deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE} --timeout=300s
                        """

                        echo "✅ Deployment successful!"
                    } catch (err) {
                        echo "❌ Deployment failed: ${err.message}"
                        echo "🔄 Rolling back..."

                        bat """
                            set KUBECONFIG=%KUBECONFIG%
                            kubectl rollout undo deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE}
                        """

                        error("Deployment failed and rolled back")
                    }
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                bat """
                    set KUBECONFIG=%KUBECONFIG%

                    echo ========================================
                    echo Pods in namespace ${K8S_NAMESPACE}:
                    kubectl get pods --namespace=${K8S_NAMESPACE} -l app=capstone

                    echo.
                    echo Service details:
                    kubectl get svc ${K8S_SERVICE} --namespace=${K8S_NAMESPACE}

                    echo.
                    echo Endpoints:
                    kubectl get endpoints ${K8S_SERVICE} --namespace=${K8S_NAMESPACE}

                    echo ========================================
                    echo Application URL: http://localhost:30080
                    echo ========================================
                """
            }
        }

        stage('Health Check') {
            steps {
                script {
                    echo "⏳ Waiting for application to stabilize..."
                    sleep(time: 30, unit: 'SECONDS')
                    
                    try {
                        def response = bat(
                            script: 'curl -s -o NUL -w "%%{http_code}" http://localhost:30080',
                            returnStdout: true
                        ).trim()
                        
                        if (response == '200') {
                            echo "✅ Health check passed! HTTP ${response}"
                        } else {
                            echo "⚠️ Health check returned: ${response}"
                        }
                    } catch (err) {
                        echo "⚠️ Health check endpoint not accessible yet. Manual verification needed."
                    }
                }
            }
        }

        stage('Save Build Summary') {
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

☸️  Kubernetes Deployment:
   • Namespace: ${K8S_NAMESPACE}
   • Deployment: ${K8S_DEPLOYMENT}
   • Service: ${K8S_SERVICE}
   • Container: ${K8S_CONTAINER}
   • Access URL: http://localhost:30080

📊 Application Endpoints:
   • Main App: http://localhost:30080
   • Index Page: http://localhost:30080/index.html
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

📋 Job: ${env.JOB_NAME}
🔢 Build: ${env.BUILD_NUMBER}
⏱ Duration: ${currentBuild.durationString}
👤 Triggered By: ${currentBuild.getBuildCauses()[0].shortDescription}

🔗 Console Output: ${env.BUILD_URL}console

🐳 Docker Image: ${DOCKER_IMAGE}:${env.BUILD_NUMBER}
☸️  Kubernetes: Namespace=${K8S_NAMESPACE}, Deployment=${K8S_DEPLOYMENT}, Service=${K8S_SERVICE}

🌍 Access App: http://localhost:30080

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
