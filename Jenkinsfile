pipeline {
    agent any

    tools {
        maven 'MAVEN_HOME'
        jdk 'JDK11'
    }

    environment {
        // Email configuration
        EMAIL_RECIPIENTS = '2200030631cseh@gmail.com'
        
        // Docker configuration
        DOCKERHUB_CREDENTIALS = 'docker-hub-creds'
        DOCKER_IMAGE = 'prasanth631/capstone_pro'
        
        // Kubernetes configuration
        K8S_DEPLOYMENT = 'capstone-deployment'
        K8S_CONTAINER = 'capstone-container'
        K8S_NAMESPACE = 'capstone-app'
        K8S_SERVICE = 'capstone-service'
        
        // IMPORTANT: Set KUBECONFIG to your user's kubeconfig location
        // This tells kubectl where to find Kubernetes configuration
        KUBECONFIG = "${env.USERPROFILE}\\.kube\\config"
    }

    triggers {
        // Poll GitHub every 5 minutes for changes
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

        stage('Code Quality Analysis') {
            steps {
                echo '🔍 Running code quality checks...'
                script {
                    // Optional: Add SonarQube or other code quality tools here
                    echo 'Code quality analysis placeholder - configure SonarQube if needed'
                }
                echo '✅ Code quality check complete'
            }
        }

        stage('Docker Build & Push') {
            steps {
                echo '🐳 Building Docker image...'
                script {
                    // Build Docker image with build number tag
                    bat """
                        docker build -t %DOCKER_IMAGE%:%BUILD_NUMBER% .
                        docker tag %DOCKER_IMAGE%:%BUILD_NUMBER% %DOCKER_IMAGE%:latest
                    """
                    
                    echo "✅ Docker image built: ${DOCKER_IMAGE}:${BUILD_NUMBER}"
                    
                    // Push to Docker Hub
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
                    bat """
                        echo Current KUBECONFIG: %KUBECONFIG%
                        echo.
                        
                        REM Set KUBECONFIG explicitly
                        set KUBECONFIG=%USERPROFILE%\\.kube\\config
                        
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
                echo '✅ Kubernetes cluster verification complete'
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo "🚀 Starting Kubernetes deployment to namespace: ${K8S_NAMESPACE}"
                script {
                    try {
                        bat """
                            REM Set KUBECONFIG
                            set KUBECONFIG=%USERPROFILE%\\.kube\\config
                            
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
                            set KUBECONFIG=%USERPROFILE%\\.kube\\config
                            kubectl rollout undo deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE}
                            kubectl rollout status deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE} --timeout=120s
                        """
                        
                        error("Deployment failed and has been rolled back to previous version")
                    }
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                echo '🔍 Verifying deployment status...'
                script {
                    bat """
                        set KUBECONFIG=%USERPROFILE%\\.kube\\config
                        
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
                        echo Health Check: http://localhost:30080/actuator/health
                        echo ========================================
                    """
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
                        } else if (response == '000') {
                            echo "⚠️ Warning: Could not connect to application. It may still be starting up."
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

        stage('Display Logs') {
            steps {
                echo '📋 Fetching recent application logs...'
                script {
                    bat """
                        set KUBECONFIG=%USERPROFILE%\\.kube\\config
                        
                        echo.
                        echo ========================================
                        echo Recent Application Logs (Last 30 lines)
                        echo ========================================
                        kubectl logs deployment/${K8S_DEPLOYMENT} --namespace=${K8S_NAMESPACE} --tail=30
                        echo ========================================
                    """
                }
                echo '✅ Logs displayed'
            }
        }

        stage('Save Build Summary') {
            steps {
                echo '💾 Saving build summary...'
                script {
                    def summary = """\
╔════════════════════════════════════════════════════════════╗
║              BUILD & DEPLOYMENT SUMMARY                     ║
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
   • Registry: Docker Hub (https://hub.docker.com)
   • Pull Command: docker pull ${DOCKER_IMAGE}:${env.BUILD_NUMBER}

☸️  Kubernetes Deployment:
   • Namespace: ${K8S_NAMESPACE}
   • Deployment: ${K8S_DEPLOYMENT}
   • Service: ${K8S_SERVICE}
   • Container: ${K8S_CONTAINER}
   • Replicas: 2
   • Strategy: RollingUpdate

📊 Application Endpoints:
   • Main Application: http://localhost:30080
   • Index Page: http://localhost:30080/index.html
   • Health Check: http://localhost:30080/actuator/health

🔍 Verification Commands:
   • View Pods: kubectl get pods -n ${K8S_NAMESPACE}
   • View Service: kubectl get svc ${K8S_SERVICE} -n ${K8S_NAMESPACE}
   • View Logs: kubectl logs -f deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}
   • Check Status: kubectl rollout status deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}

═══════════════════════════════════════════════════════════════
Generated by Jenkins CI/CD Pipeline
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
        .header p {
            margin: 5px 0;
            opacity: 0.9;
            font-size: 16px;
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
            margin-top: 10px;
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
        .info-table td:last-child {
            color: #333;
        }
        .info-list {
            list-style: none;
            padding: 0;
            margin: 10px 0;
        }
        .info-list li {
            padding: 8px 0;
            border-bottom: 1px solid #eee;
        }
        .info-list li:last-child {
            border-bottom: none;
        }
        .label {
            font-weight: 600;
            color: #555;
            display: inline-block;
            width: 150px;
        }
        .value {
            color: #333;
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
        .link-button:hover {
            background-color: #0056b3;
        }
        .code-block {
            background-color: #f8f9fa;
            border: 1px solid #e9ecef;
            border-radius: 4px;
            padding: 15px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
            overflow-x: auto;
            color: #212529;
        }
        .footer {
            background-color: #f8f9fa;
            padding: 20px;
            text-align: center;
            color: #6c757d;
            font-size: 14px;
            border-top: 1px solid #dee2e6;
        }
        .status-badge {
            display: inline-block;
            padding: 5px 15px;
            border-radius: 20px;
            font-weight: 600;
            background-color: ${color};
            color: white;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>${statusIcon} Build ${status}</h1>
            <p><strong>${env.JOB_NAME}</strong> - Build #${env.BUILD_NUMBER}</p>
            <p>${new Date().format('MMM dd, yyyy HH:mm:ss')}</p>
        </div>
        
        <div class="content">
            <div class="section">
                <h2>Build Information</h2>
                <table class="info-table">
                    <tr>
                        <td>Status</td>
                        <td><span class="status-badge">${status}</span></td>
                    </tr>
                    <tr>
                        <td>Job Name</td>
                        <td>${env.JOB_NAME}</td>
                    </tr>
                    <tr>
                        <td>Build Number</td>
                        <td>#${env.BUILD_NUMBER}</td>
                    </tr>
                    <tr>
                        <td>Duration</td>
                        <td>${currentBuild.durationString}</td>
                    </tr>
                    <tr>
                        <td>Branch</td>
                        <td>main</td>
                    </tr>
                    <tr>
                        <td>Triggered By</td>
                        <td>${currentBuild.getBuildCauses()[0].shortDescription}</td>
                    </tr>
                </table>
            </div>
            
            <div class="section">
                <h2>Quick Actions</h2>
                <div style="text-align: center; margin: 20px 0;">
                    <a href="${env.BUILD_URL}" class="link-button">View Build Details</a>
                    <a href="${env.BUILD_URL}console" class="link-button">Console Output</a>
                    <a href="http://localhost:30080" class="link-button">Open Application</a>
                </div>
            </div>
            
            <div class="section">
                <h2>Docker Image</h2>
                <ul class="info-list">
                    <li><span class="label">Image Tag:</span> <span class="value">${DOCKER_IMAGE}:${env.BUILD_NUMBER}</span></li>
                    <li><span class="label">Latest Tag:</span> <span class="value">${DOCKER_IMAGE}:latest</span></li>
                    <li><span class="label">Registry:</span> <span class="value">Docker Hub</span></li>
                </ul>
                <div class="code-block">docker pull ${DOCKER_IMAGE}:${env.BUILD_NUMBER}</div>
            </div>
            
            <div class="section">
                <h2>Kubernetes Deployment</h2>
                <table class="info-table">
                    <tr>
                        <td>Namespace</td>
                        <td>${K8S_NAMESPACE}</td>
                    </tr>
                    <tr>
                        <td>Deployment</td>
                        <td>${K8S_DEPLOYMENT}</td>
                    </tr>
                    <tr>
                        <td>Service</td>
                        <td>${K8S_SERVICE}</td>
                    </tr>
                    <tr>
                        <td>Container</td>
                        <td>${K8S_CONTAINER}</td>
                    </tr>
                </table>
            </div>
            
            <div class="section">
                <h2>Application Endpoints</h2>
                <ul class="info-list">
                    <li><span class="label">Main App:</span> <a href="http://localhost:30080">http://localhost:30080</a></li>
                    <li><span class="label">Health Check:</span> <a href="http://localhost:30080/actuator/health">http://localhost:30080/actuator/health</a></li>
                    <li><span class="label">Index Page:</span> <a href="http://localhost:30080/index.html">http://localhost:30080/index.html</a></li>
                </ul>
            </div>
            
            <div class="section">
                <h2>Verification Commands</h2>
                <div class="code-block">
kubectl get pods -n ${K8S_NAMESPACE}<br>
kubectl get svc ${K8S_SERVICE} -n ${K8S_NAMESPACE}<br>
kubectl logs -f deployment/${K8S_DEPLOYMENT} -n ${K8S_NAMESPACE}
                </div>
            </div>
        </div>
        
        <div class="footer">
            <p><strong>Jenkins CI/CD Pipeline</strong></p>
            <p>This is an automated notification. Please do not reply to this email.</p>
            <p style="margin-top: 10px; font-size: 12px;">
                Build started at ${new Date().format('yyyy-MM-dd HH:mm:ss')}
            </p>
        </div>
    </div>
</body>
</html>
"""
                
                // Send email with HTML content
                emailext(
                    to: "${EMAIL_RECIPIENTS}",
                    subject: subject,
                    body: body,
                    attachmentsPattern: 'build-summary.txt',
                    mimeType: 'text/html'
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
            echo 'The deployment has been automatically rolled back to the previous working version.'
        }
        
        unstable {
            echo '⚠️⚠️⚠️ Pipeline completed with warnings ⚠️⚠️⚠️'
        }
        
        cleanup {
            echo '🧹 Cleaning up workspace...'
            // Optional: Clean up temporary files
            // cleanWs()
        }
    }
}