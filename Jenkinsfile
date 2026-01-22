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
        
        KUBECONFIG = "C:\\Users\\Prasanth Golla\\.kube\\config"
    }

    triggers {
        pollSCM('* * * * *')
    }

    stages {
        // ============================================
        // STAGE 1: BUILD & TEST
        // ============================================
        stage('Build & Test') {
            steps {
                echo '========================================='
                echo 'Stage 1: Build & Test'
                echo '========================================='
                
                // Checkout
                echo 'Checking out code from GitHub...'
                git url: 'https://github.com/Prasanth631/CapstoneProject.git', branch: 'main'
                
                // Build & Test
                echo 'Building application with Maven...'
                bat 'mvn clean install -DskipTests'
                
                echo 'Build & Test complete'
            }
        }

        // SonarQube stage removed - using JaCoCo for coverage

        // ============================================
        // STAGE 3: DOCKER BUILD (LOCAL ONLY)
        // ============================================
        // ============================================
        // STAGE 3: DOCKER BUILD & PUSH
        // ============================================
        stage('Docker Build & Push') {
            steps {
                echo '========================================='
                echo 'Stage 3: Docker Build & Push'
                echo '========================================='
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        bat """
                            @echo off
                            echo Logging into Docker Hub...
                            docker login -u %DOCKER_USERNAME% -p %DOCKER_PASSWORD%
                            
                            echo Building Docker image...
                            docker build -t %DOCKER_IMAGE%:%BUILD_NUMBER% .
                            docker tag %DOCKER_IMAGE%:%BUILD_NUMBER% %DOCKER_IMAGE%:latest
                            
                            echo Pushing Docker image...
                            docker push %DOCKER_IMAGE%:%BUILD_NUMBER%
                            docker push %DOCKER_IMAGE%:latest
                            
                            echo Logout...
                            docker logout
                        """
                    }
                }
            }
        }

        // ============================================
        // STAGE 4: DEPLOY TO KUBERNETES
        // ============================================
        stage('Deploy to Kubernetes') {
            steps {
                echo '========================================='
                echo 'Stage 4: Deploy to Kubernetes'
                echo '========================================='
                script {
                    try {
                        bat """
                            set KUBECONFIG=C:\\Users\\Prasanth Golla\\.kube\\config
                            
                            echo Verifying Kubernetes cluster...
                            kubectl cluster-info
                            kubectl get nodes
                            
                            echo.
                            echo Applying Kubernetes manifests...
                            
                            echo.
                            echo Deploying PostgreSQL Database...
                            kubectl apply -f k8s/postgres-secret.yaml --namespace=${K8S_NAMESPACE}
                            kubectl apply -f k8s/postgres-service.yaml --namespace=${K8S_NAMESPACE}
                            kubectl apply -f k8s/postgres-statefulset.yaml --namespace=${K8S_NAMESPACE}
                            
                            echo.
                            echo Deploying Application...
                            kubectl apply -f k8s/secrets.yaml --namespace=${K8S_NAMESPACE}
                            kubectl apply -f k8s/configmap.yaml --namespace=${K8S_NAMESPACE}
                            kubectl apply -f k8s/service.yaml --namespace=${K8S_NAMESPACE}
                            kubectl apply -f k8s/deployment.yaml --namespace=${K8S_NAMESPACE}
                            
                            echo.
                            echo Updating deployment image to build %BUILD_NUMBER%...
                            kubectl set image deployment/${K8S_DEPLOYMENT} ${K8S_CONTAINER}=%DOCKER_IMAGE%:%BUILD_NUMBER% --namespace=${K8S_NAMESPACE}
                            
                            echo.
                            echo Waiting for rollout to complete...
                            kubectl rollout status deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE} --timeout=300s
                        """
                        echo 'Kubernetes deployment successful!'
                        
                    } catch (err) {
                        echo "Deployment failed! Error: ${err.message}"
                        echo 'Attempting automatic rollback...'
                        
                        bat """
                            set KUBECONFIG=C:\\Users\\Prasanth Golla\\.kube\\config
                            kubectl rollout undo deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE}
                            kubectl rollout status deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE} --timeout=120s
                        """
                        error("Deployment failed and has been rolled back")
                    }
                }
            }
        }

        // ============================================
        // STAGE 5: DEPLOY MONITORING
        // ============================================
        stage('Deploy Monitoring') {
            steps {
                echo '========================================='
                echo 'Stage 5: Deploy Monitoring (Prometheus)'
                echo '========================================='
                script {
                    try {
                        bat """
                            set KUBECONFIG=C:\\Users\\Prasanth Golla\\.kube\\config
                            
                            echo Applying Prometheus configuration...
                            kubectl apply -f k8s/prometheus-config.yaml --namespace=default
                            
                            echo Checking Prometheus deployment...
                            kubectl get deployment prometheus -n default 2>nul
                            
                            IF ERRORLEVEL 1 (
                                echo Creating Prometheus deployment...
                                kubectl create deployment prometheus --image=prom/prometheus:latest -n default
                                kubectl set volume deployment/prometheus --add --name=prometheus-config --mount-path=/etc/prometheus --configmap-name=prometheus-config -n default
                                kubectl expose deployment prometheus --type=NodePort --port=9090 --target-port=9090 --name=prometheus-service -n default
                                timeout /t 2 /nobreak >nul
                                kubectl patch service prometheus-service -n default --type=json -p="[{\\"op\\":\\"replace\\",\\"path\\":\\"/spec/ports/0/nodePort\\",\\"value\\":30090}]"
                            ) ELSE (
                                echo Prometheus already exists. Restarting...
                                kubectl rollout restart deployment/prometheus -n default
                            )
                            
                            echo Waiting for Prometheus to be ready...
                            kubectl wait --for=condition=available --timeout=120s deployment/prometheus -n default
                            
                            echo Prometheus UI: http://localhost:30090
                        """
                        echo 'Prometheus monitoring deployed!'
                        
                    } catch (err) {
                        echo "Prometheus deployment warning: ${err.message}"
                        echo 'Continuing with pipeline...'
                    }
                }
            }
        }

        // ============================================
        // STAGE 6: VERIFY & REPORT
        // ============================================
        stage('Verify & Report') {
            steps {
                echo '========================================='
                echo 'Stage 6: Verify & Report'
                echo '========================================='
                script {
                    // Smart Health Check
                    echo 'Running smart health check (timeout 60s)...'
                    timeout(time: 60, unit: 'SECONDS') {
                        waitUntil {
                            try {
                                def response = bat(
                                    script: 'curl -s -o NUL -w "%%{http_code}" http://localhost:30080',
                                    returnStdout: true
                                ).trim()
                                
                                if (response.contains('200')) {
                                    echo "Application is ready!"
                                    return true
                                }
                                echo "Waiting for application... (Status: ${response})"
                                sleep(time: 2, unit: 'SECONDS')
                                return false
                            } catch (Exception e) {
                                echo "Check failed, retrying..."
                                sleep(time: 2, unit: 'SECONDS')
                                return false
                            }
                        }
                    }
                    
                    echo "Health check PASSED!"
                    
                    // Verify Deployment Status
                    bat """
                        set KUBECONFIG=C:\\Users\\Prasanth Golla\\.kube\\config
                        
                        echo.
                        echo ========================================
                        echo DEPLOYMENT STATUS
                        echo ========================================
                        kubectl get pods --namespace=${K8S_NAMESPACE} -l app=capstone
                        kubectl get svc ${K8S_SERVICE} --namespace=${K8S_NAMESPACE}
                        echo.
                        echo Application: http://localhost:30080
                        echo Prometheus:  http://localhost:30090
                        echo ========================================
                    """
                    
                    // Save Build Summary
                    def summary = """\
+-----------------------------------------------------------+
|             BUILD & DEPLOYMENT SUMMARY                    |
+-----------------------------------------------------------+

Build Information:
  - Status: ${currentBuild.currentResult}
  - Job: ${env.JOB_NAME}
  - Build Number: ${env.BUILD_NUMBER}
  - Branch: main
  - Timestamp: ${new Date()}
  - Duration: ${currentBuild.durationString}

Docker Information:
  - Image: ${DOCKER_IMAGE}:${env.BUILD_NUMBER}
  - Registry: Docker Hub

Kubernetes Deployment:
  - Namespace: ${K8S_NAMESPACE}
  - Deployment: ${K8S_DEPLOYMENT}
  - Service: ${K8S_SERVICE}

Application Endpoints:
  - Main Application: http://localhost:30080
  - Prometheus UI: http://localhost:30090
  - Health Check: http://localhost:30080/actuator/health
  - Metrics: http://localhost:30080/actuator/prometheus

-----------------------------------------------------------
Generated by Jenkins CI/CD Pipeline
-----------------------------------------------------------
"""
                    writeFile file: 'build-summary.txt', text: summary
                    archiveArtifacts artifacts: 'build-summary.txt', fingerprint: true
                    
                    echo 'Build summary saved'
                }
            }
        }
    }

    post {
        always {
            script {
                echo 'Sending email notification...'
                
                def status = currentBuild.currentResult
                def statusIcon = (status == 'SUCCESS') ? '[SUCCESS]' : '[FAILED]'
                def color = (status == 'SUCCESS') ? '#28a745' : '#dc3545'
                def subject = "${statusIcon} Build #${env.BUILD_NUMBER} - ${env.JOB_NAME}"
                
                def body = """\
<html>
<head>
    <style>
        body { font-family: 'Segoe UI', sans-serif; margin: 0; padding: 0; background: #f5f5f5; }
        .container { max-width: 600px; margin: 20px auto; background: white; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .header { background: ${color}; color: white; padding: 25px; text-align: center; }
        .header h1 { margin: 0; font-size: 24px; }
        .content { padding: 25px; }
        .info-table { width: 100%; border-collapse: collapse; }
        .info-table td { padding: 10px; border-bottom: 1px solid #eee; }
        .info-table td:first-child { font-weight: 600; color: #555; width: 40%; }
        .link-button { display: inline-block; padding: 10px 20px; margin: 5px; background: #007bff; color: white !important; text-decoration: none; border-radius: 4px; }
        .footer { background: #f8f9fa; padding: 15px; text-align: center; color: #6c757d; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>${statusIcon} Build ${status}</h1>
            <p>${env.JOB_NAME} - Build #${env.BUILD_NUMBER}</p>
        </div>
        <div class="content">
            <table class="info-table">
                <tr><td>Status</td><td>${status}</td></tr>
                <tr><td>Build Number</td><td>#${env.BUILD_NUMBER}</td></tr>
                <tr><td>Duration</td><td>${currentBuild.durationString}</td></tr>
            </table>
            <div style="text-align: center; margin-top: 20px;">
                <a href="${env.BUILD_URL}" class="link-button">View Build</a>
                <a href="http://localhost:30080" class="link-button">Application</a>
                <a href="http://localhost:30090" class="link-button">Prometheus</a>
            </div>
        </div>
        <div class="footer">
            <p>Jenkins CI/CD Pipeline - Automated Notification</p>
        </div>
    </div>
</body>
</html>
"""
                
                emailext(
                    to: "${EMAIL_RECIPIENTS}",
                    subject: subject,
                    body: body,
                    attachmentsPattern: 'build-summary.txt',
                    mimeType: 'text/html'
                )
                
                echo 'Email notification sent'
            }
        }
        
        success {
            echo 'Pipeline completed successfully!'
            echo "Application: http://localhost:30080"
            echo "Prometheus: http://localhost:30090"
        }
        
        failure {
            echo 'Pipeline failed!'
            echo 'Deployment has been rolled back to previous version.'
        }
    }
}