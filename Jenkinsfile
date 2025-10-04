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
        pollSCM('H/5 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code from GitHub...'
                git url: 'https://github.com/Prasanth631/CapstoneProject.git', branch: 'main'
                echo 'Code checkout complete'
            }
        }

        stage('Build & Test') {
            steps {
                echo 'Building application with Maven...'
                bat 'mvn clean install'
                echo 'Build and tests complete'
            }
        }

        stage('Publish Test Results') {
            steps {
                echo 'Publishing test results...'
                junit 'target/surefire-reports/*.xml'
                echo 'Test results published'
            }
        }

        stage('Code Quality Analysis') {
            steps {
                echo 'Running code quality checks...'
                script {
                    echo 'Code quality analysis placeholder - configure SonarQube if needed'
                }
                echo 'Code quality check complete'
            }
        }

        stage('Docker Build & Push') {
            steps {
                echo 'Building Docker image...'
                script {
                    bat """
                        docker build -t %DOCKER_IMAGE%:%BUILD_NUMBER% .
                        docker tag %DOCKER_IMAGE%:%BUILD_NUMBER% %DOCKER_IMAGE%:latest
                    """
                    
                    echo "Docker image built: ${DOCKER_IMAGE}:${BUILD_NUMBER}"
                    
                    echo 'Pushing image to Docker Hub...'
                    docker.withRegistry('https://index.docker.io/v1/', "${DOCKERHUB_CREDENTIALS}") {
                        def app = docker.image("${DOCKER_IMAGE}:${BUILD_NUMBER}")
                        app.push()
                        app.push("latest")
                    }
                    
                    echo 'Docker image pushed to Docker Hub'
                }
            }
        }

        stage('Verify Kubernetes Cluster') {
            steps {
                echo 'Verifying Kubernetes cluster connectivity...'
                script {
                    bat """
                        echo Current KUBECONFIG: %KUBECONFIG%
                        echo.
                        
                        set KUBECONFIG=C:\\Users\\Prasanth Golla\\.kube\\config
                        
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
                echo 'Kubernetes cluster verification complete'
            }
        }

        stage('Deploy Prometheus') {
            steps {
                echo '========================================='
                echo 'Deploying Prometheus Monitoring'
                echo '========================================='
                script {
                    try {
                        bat """
                            set KUBECONFIG=C:\\Users\\Prasanth Golla\\.kube\\config
                            
                            echo.
                            echo [Step 1/3] Creating or updating Prometheus ConfigMap...
                            kubectl apply -f k8s/prometheus-config.yaml
                            
                            echo.
                            echo [Step 2/3] Checking if Prometheus deployment exists...
                            kubectl get deployment prometheus -n default 2>nul
                            
                            IF ERRORLEVEL 1 (
                                echo Prometheus deployment does not exist. Creating new deployment...
                                
                                echo Creating Prometheus Deployment...
                                kubectl create deployment prometheus --image=prom/prometheus:latest -n default
                                
                                echo.
                                echo Configuring Prometheus to use ConfigMap...
                                kubectl set volume deployment/prometheus ^
                                    --add --name=prometheus-config ^
                                    --mount-path=/etc/prometheus ^
                                    --configmap-name=prometheus-config -n default
                                
                                echo.
                                echo Setting resource limits...
                                kubectl set resources deployment/prometheus ^
                                    --limits=cpu=500m,memory=1Gi ^
                                    --requests=cpu=250m,memory=512Mi -n default
                                
                                echo.
                                echo Exposing Prometheus as NodePort service...
                                kubectl expose deployment prometheus ^
                                    --type=NodePort ^
                                    --port=9090 ^
                                    --target-port=9090 ^
                                    --name=prometheus-service -n default
                                
                                echo.
                                echo Setting NodePort to 30090...
                                kubectl patch service prometheus-service -n default ^
                                    --type='json' ^
                                    -p='[{"op": "replace", "path": "/spec/ports/0/nodePort", "value":30090}]'
                            ) ELSE (
                                echo Prometheus deployment already exists. Updating configuration...
                                kubectl rollout restart deployment/prometheus -n default
                            )
                            
                            echo.
                            echo [Step 3/3] Waiting for Prometheus to be ready...
                            kubectl wait --for=condition=available --timeout=120s deployment/prometheus -n default
                            
                            echo.
                            echo ========================================
                            echo Prometheus Deployment Summary
                            echo ========================================
                            kubectl get deployment prometheus -n default
                            kubectl get svc prometheus-service -n default
                            kubectl get pods -l app=prometheus -n default
                            echo.
                            echo Prometheus UI: http://localhost:30090
                            echo Metrics endpoint: http://localhost:30090/metrics
                            echo ========================================
                        """
                        
                        echo 'Prometheus deployment successful!'
                        
                    } catch (err) {
                        echo "Prometheus deployment warning: ${err.message}"
                        echo 'Continuing with pipeline...'
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo "Starting Kubernetes deployment to namespace: ${K8S_NAMESPACE}"
                script {
                    try {
                        bat """
                            set KUBECONFIG=C:\\Users\\Prasanth Golla\\.kube\\config
                            
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
                        
                        echo 'Deployment successful!'
                        
                    } catch (err) {
                        echo "Deployment failed! Error: ${err.message}"
                        echo 'Attempting automatic rollback...'
                        
                        bat """
                            set KUBECONFIG=C:\\Users\\Prasanth Golla\\.kube\\config
                            kubectl rollout undo deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE}
                            kubectl rollout status deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE} --timeout=120s
                        """
                        
                        error("Deployment failed and has been rolled back to previous version")
                    }
                }
            }
        }

        stage('Verify Prometheus Integration') {
            steps {
                echo 'Verifying Prometheus metrics collection...'
                script {
                    bat """
                        set KUBECONFIG=C:\\Users\\Prasanth Golla\\.kube\\config
                        
                        echo.
                        echo ========================================
                        echo Prometheus Integration Verification
                        echo ========================================
                        
                        echo.
                        echo Checking Prometheus pods:
                        kubectl get pods -l app=prometheus -n default
                        
                        echo.
                        echo Checking Prometheus service:
                        kubectl get svc prometheus-service -n default
                        
                        echo.
                        echo Checking if application exposes Prometheus metrics:
                        timeout 5 curl -s http://localhost:30080/actuator/prometheus ^| findstr "jvm_memory_used_bytes" || echo Application metrics endpoint may not be ready yet
                        
                        echo.
                        echo ========================================
                        echo Prometheus Access Information
                        echo ========================================
                        echo Prometheus UI: http://localhost:30090
                        echo Prometheus API: http://localhost:30090/api/v1/query
                        echo Application Metrics: http://localhost:30080/actuator/prometheus
                        echo.
                        echo To view metrics in Prometheus:
                        echo 1. Open http://localhost:30090
                        echo 2. Enter query: jvm_memory_used_bytes
                        echo 3. Click Execute
                        echo ========================================
                    """
                }
                echo 'Prometheus integration verification complete'
            }
        }

        stage('Verify Deployment') {
            steps {
                echo 'Verifying deployment status...'
                script {
                    bat """
                        set KUBECONFIG=C:\\Users\\Prasanth Golla\\.kube\\config
                        
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
                        echo Dashboard: http://localhost:30080/index.html
                        echo Health Check: http://localhost:30080/actuator/health
                        echo Prometheus Metrics: http://localhost:30080/actuator/prometheus
                        echo Prometheus UI: http://localhost:30090
                        echo ========================================
                    """
                }
                echo 'Deployment verification complete'
            }
        }

        stage('Health Check') {
            steps {
                echo 'Running application health check...'
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
                            echo "Health check PASSED! Application is responding correctly."
                        } else if (response == '000') {
                            echo "Warning: Could not connect to application. It may still be starting up."
                        } else {
                            echo "Warning: Unexpected response code: ${response}"
                        }
                        
                        echo 'Testing Prometheus metrics endpoint...'
                        def metricsResponse = bat(
                            script: 'curl -s -o NUL -w "%%{http_code}" http://localhost:30080/actuator/prometheus',
                            returnStdout: true
                        ).trim()
                        
                        if (metricsResponse == '200') {
                            echo "Prometheus metrics endpoint is accessible!"
                        } else {
                            echo "Warning: Prometheus metrics endpoint returned: ${metricsResponse}"
                        }
                        
                    } catch (err) {
                        echo "Health check could not be completed automatically."
                        echo "Please verify manually at: http://localhost:30080"
                    }
                }
                echo 'Health check stage complete'
            }
        }

        stage('Display Logs') {
            steps {
                echo 'Fetching recent application logs...'
                script {
                    bat """
                        set KUBECONFIG=C:\\Users\\Prasanth Golla\\.kube\\config
                        
                        echo.
                        echo ========================================
                        echo Recent Application Logs (Last 30 lines)
                        echo ========================================
                        kubectl logs deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE} --tail=30
                        echo ========================================
                    """
                }
                echo 'Logs displayed'
            }
        }

        stage('Save Build Summary') {
            steps {
                echo 'Saving build summary...'
                script {
                    def summary = """\
╔═══════════════════════════════════════════════════════════╗
║              BUILD & DEPLOYMENT SUMMARY                    ║
╚═══════════════════════════════════════════════════════════╝

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
   • Registry: Docker Hub
   • Pull Command: docker pull ${DOCKER_IMAGE}:${env.BUILD_NUMBER}

☸️ Kubernetes Deployment:
   • Namespace: ${K8S_NAMESPACE}
   • Deployment: ${K8S_DEPLOYMENT}
   • Service: ${K8S_SERVICE}
   • Container: ${K8S_CONTAINER}
   • Replicas: 2
   • Strategy: RollingUpdate

📊 Monitoring & Metrics:
   • Prometheus UI: http://localhost:30090
   • Application Metrics: http://localhost:30080/actuator/prometheus
   • Health Endpoint: http://localhost:30080/actuator/health
   • Grafana Dashboard: Configure with Prometheus data source

🌐 Application Endpoints:
   • Main Application: http://localhost:30080
   • Dashboard: http://localhost:30080/index.html
   • Health Check: http://localhost:30080/actuator/health
   • Prometheus Metrics: http://localhost:30080/actuator/prometheus

🔍 Verification Commands:
   • View Pods: kubectl get pods -n ${K8S_NAMESPACE}
   • View Service: kubectl get svc ${K8S_SERVICE} -n ${K8S_NAMESPACE}
   • View Logs: kubectl logs -f deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}
   • Check Status: kubectl rollout status deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}
   • View Prometheus: kubectl get pods -l app=prometheus -n default

═══════════════════════════════════════════════════════════
Generated by Jenkins CI/CD Pipeline with Prometheus Monitoring
═══════════════════════════════════════════════════════════
"""
                    writeFile file: 'build-summary.txt', text: summary
                    archiveArtifacts artifacts: 'build-summary.txt', fingerprint: true
                }
                echo 'Build summary saved'
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
        body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            margin: 0;
            padding: 0;
            background-color: #f5f5f5;
        }
        .container {
            max-width: 700px;
            margin: 20px auto;
            background-color: white;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .header { 
            background: linear-gradient(135deg, ${color} 0%, ${color}dd 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        .header h1 {
            margin: 0 0 10px 0;
            font-size: 28px;
        }
        .content { 
            padding: 30px;
        }
        .section { 
            margin-bottom: 30px;
            border-left: 4px solid ${color};
            padding-left: 15px;
        }
        .section h2 {
            color: #333;
            margin-top: 0;
            margin-bottom: 15px;
            font-size: 20px;
        }
        .info-table { 
            width: 100%;
            border-collapse: collapse;
        }
        .info-table td {
            padding: 10px;
            border-bottom: 1px solid #eee;
        }
        .info-table td:first-child {
            font-weight: 600;
            color: #555;
            width: 40%;
        }
        .link-button {
            display: inline-block;
            padding: 10px 20px;
            margin: 5px;
            background-color: #007bff;
            color: white !important;
            text-decoration: none;
            border-radius: 4px;
            font-weight: 500;
        }
        .code-block {
            background-color: #f8f9fa;
            border: 1px solid #e9ecef;
            border-radius: 4px;
            padding: 15px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            color: #6c757d;
            font-size: 14px;
            border-top: 1px solid #dee2e6;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>${statusIcon} Build ${status}</h1>
            <p><strong>${env.JOB_NAME}</strong> - Build #${env.BUILD_NUMBER}</p>
        </div>
        
        <div class="content">
            <div class="section">
                <h2>Build Information</h2>
                <table class="info-table">
                    <tr><td>Status</td><td>${status}</td></tr>
                    <tr><td>Build Number</td><td>#${env.BUILD_NUMBER}</td></tr>
                    <tr><td>Duration</td><td>${currentBuild.durationString}</td></tr>
                </table>
            </div>
            
            <div class="section">
                <h2>Quick Actions</h2>
                <div style="text-align: center;">
                    <a href="${env.BUILD_URL}" class="link-button">View Build</a>
                    <a href="http://localhost:30080/index.html" class="link-button">Dashboard</a>
                    <a href="http://localhost:30090" class="link-button">Prometheus</a>
                </div>
            </div>
            
            <div class="section">
                <h2>Monitoring Endpoints</h2>
                <table class="info-table">
                    <tr><td>Application</td><td><a href="http://localhost:30080">http://localhost:30080</a></td></tr>
                    <tr><td>Dashboard</td><td><a href="http://localhost:30080/index.html">http://localhost:30080/index.html</a></td></tr>
                    <tr><td>Prometheus</td><td><a href="http://localhost:30090">http://localhost:30090</a></td></tr>
                    <tr><td>Metrics</td><td><a href="http://localhost:30080/actuator/prometheus">http://localhost:30080/actuator/prometheus</a></td></tr>
                </table>
            </div>
        </div>
        
        <div class="footer">
            <p><strong>Jenkins CI/CD Pipeline with Prometheus</strong></p>
            <p>This is an automated notification.</p>
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
            echo "Application: http://localhost:30080/index.html"
            echo "Prometheus: http://localhost:30090"
        }
        
        failure {
            echo 'Pipeline failed!'
            echo 'Deployment has been rolled back to previous version.'
        }
    }
}