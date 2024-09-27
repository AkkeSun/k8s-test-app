pipeline {
    agent any

    tools {
        gradle 'gradle-8.8'
        jdk 'jdk-17'
    }

    // environment variable setting
    environment {
        PROD_DOCKER_IMAGE_NAME = 'akkessun/od-test-prod'
        LAST_COMMIT = ""
        NOW_TIME = sh(script: 'date +%Y%m%d%H%M', returnStdout: true).trim()
    }

    stages {

        stage('[Master] Jenkins variable setting') {
            when {
                branch 'master'
            }
            steps {
                script {
                    // ------ use Folder Property plugin
                    // Jenkins variable setting
                    wrap([$class: 'ParentFolderBuildWrapper']) {
                        dockerUsername = "${env.DOCKER_USERNAME}"
                        dockerPassword = "${env.DOCKER_PASSWORD}"
                    }

                    // git last commit setting (for Slack Notification)
                    LAST_COMMIT = sh(returnStdout: true, script: "git log -1 --pretty=%B").trim()
                    echo '[dockerUsername] ' + dockerUsername
                    echo '[dockerPassword] ' + dockerPassword
                    echo '[last commit] ' + LAST_COMMIT
                }
            }
        }

        stage('[Master] Jar & Image Build') {
            when {
                branch 'master'
            }
            steps {
                script {
                    sh 'gradle clean build -Pprofile=real'
                    sh "docker build -t ${env.PROD_DOCKER_IMAGE_NAME}:${NOW_TIME} ."
                }
            }
        }

        stage('[Master] Docker Hub deploy') {
            when {
                branch 'master'
            }
            steps {
                script {
                    sh "docker login -u ${dockerUsername} -p ${dockerPassword}"
                    sh "docker push ${PROD_DOCKER_IMAGE_NAME}:${NOW_TIME}"
                    sh "docker logout"
                }
            }
        }

        stage('[Master] k8s deploy') {
            when {
                branch 'master'
            }
            steps {
                script {
                  sh """
                     sed -i 's|image: ${PROD_DOCKER_IMAGE_NAME}:.*|image: ${PROD_DOCKER_IMAGE_NAME}:${NOW_TIME}|' deployment.yaml
                  """
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/namespace.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/pv.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/pvc.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/configmap.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/service.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/hpa.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/deployment.yaml'

                }
            }
        }
    }

    /*
    // ------ use Slack Notification plugin
    post {
        success {
            slackSend color: "good", message: "✅Build Success!\\n\\n\\n PROJECT             : ${JOB_NAME}\\n BRANCH             : ${env.BRANCH_NAME}\\n JENKINS URL     : <${env.RUN_DISPLAY_URL}|Blue Ocean Link>\\n LAST_COMMIT  : ${LAST_COMMIT}"
        }
        failure {
            slackSend color: "danger", message: "❌Build Fail!\\n\\n\\n PROJECT             : ${JOB_NAME}\\n BRANCH             : ${env.BRANCH_NAME}\\n JENKINS URL     : <${env.RUN_DISPLAY_URL}|Blue Ocean Link>\\n LAST_COMMIT  : ${LAST_COMMIT}"
        }
    }
    */
}
