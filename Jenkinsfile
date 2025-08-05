pipeline {
    agent any
    
    // Define tools
    tools {
        // Use the names of your configured tools in Jenkins
        maven 'Maven' // Make sure this matches the name in Jenkins Global Tool Configuration
        jdk 'JDK' // Make sure this matches the name in Jenkins Global Tool Configuration
    }
    
    stages {
        stage('Checkout') {
            steps {
                // Checkout code from your repository
                checkout scm
                echo 'Code checkout complete'
            }
        }
        
        stage('Build') {
            steps {
                // Run Maven clean compile
                sh 'mvn clean compile'
                echo 'Build complete'
            }
        }
        
        stage('Test') {
            steps {
                // Run the tests
                sh 'mvn test'
                echo 'Tests complete'
            }
            post {
                always {
                    // Archive JUnit test results
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Code Coverage') {
            steps {
                // Generate code coverage report
                sh 'mvn jacoco:report'
                echo 'Code coverage report generated'
            }
            post {
                success {
                    // Archive JaCoCo coverage report
                    publishHTML(target: [
                        allowMissing: false,
                        alwaysLinkToLastBuild: false,
                        keepAll: true,
                        reportDir: 'target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'JaCoCo Code Coverage'
                    ])
                }
            }
        }
        
        stage('Package') {
            steps {
                // Package the application
                sh 'mvn package -DskipTests'
                echo 'Packaging complete'
            }
            post {
                success {
                    // Archive the built artifact
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }
    }
    
    post {
        always {
            // Clean the workspace
            cleanWs()
        }
        
        success {
            // Send email on successful build
            emailext (
                subject: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: """<p>BUILD SUCCESS</p>
                         <p>Job: ${env.JOB_NAME} [${env.BUILD_NUMBER}]</p>
                         <p>Check console output at: ${env.BUILD_URL}</p>""",
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], 
                                    [$class: 'RequesterRecipientProvider']]
            )
        }
        
        failure {
            // Send email on failed build
            emailext (
                subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: """<p>BUILD FAILED</p>
                         <p>Job: ${env.JOB_NAME} [${env.BUILD_NUMBER}]</p>
                         <p>Check console output at: ${env.BUILD_URL}</p>""",
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], 
                                    [$class: 'RequesterRecipientProvider']]
            )
        }
    }
}