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
                branch 'master-blue_green'
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
                branch 'master-blue_green'
            }
            steps {
                script {
                    sh 'gradle clean build -Pprofile=real'
                    sh "docker build -t ${env.PROD_DOCKER_IMAGE_NAME}:latest ."
                }
            }
        }

        stage('[Master] Docker Hub deploy') {
            when {
                branch 'master-blue_green'
            }
            steps {
                script {
                    sh "docker login -u ${dockerUsername} -p ${dockerPassword}"
                    sh "docker push ${PROD_DOCKER_IMAGE_NAME}:latest"
                    sh "docker logout"
                }
            }
        }

        stage('[Master] k8s blue deploy') {
            when {
                branch 'master-blue_green'
            }
            steps {
                script {
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/blue/namespace.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/blue/configmap.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/blue/service.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/blue/hpa.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/blue/deployment.yaml'
                }
            }
        }

        stage('[Master] k8s green deploy check') {
            when {
                branch 'master-blue_green'
            }
            steps {
                input message: 'green deploy start', ok: "Yes"
            }
        }

        stage('[Master] k8s green deploy') {
            when {
                branch 'master-blue_green'
            }
            steps {
                script {
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/green/deployment.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/green/service.yaml'
                }
            }
        }

        stage('[Master] k8s traffic change (blue -> green)') {
            when {
                branch 'master-blue_green'
            }
            steps {
                script {
                    returnValue = input message: 'traffic change (blue -> green)', ok: "Yes", parameters: [booleanParam(defaultValue: true, name: 'IS_SWITCHED')]
                    if (returnValue) {
                        // od-test-prod 네임스페이스에서 od-test-prod-1 이름의 서비스를 찾습니다.
                        // 서비스의 spec.selector를 blue-green-no=2로 변경하여, 이제 해당 서비스는 blue-green-no 라벨이 2로 설정된 파드를 선택합니다.
                        sh "kubectl patch -n od-test-prod svc od-test-prod-1 -p '{\"spec\": {\"selector\": {\"blue-green-no\": \"2\"}}}'"
                    }
                }
            }
        }

        stage('[Master] k8s rollback check') {
            when {
                branch 'master-blue_green'
            }
            steps {
                script {
                    returnValue = input message: 'needs rollback?', parameters: [choice(choices: ['done', 'rollback'], name: 'IS_ROLLBACk')]
                    if (returnValue == "done") {
                        sh "kubectl delete -f ./src/main/deployment/real/k8s/blue/deployment.yaml"
                        sh "kubectl delete -f ./src/main/deployment/real/k8s/green/service.yaml"
                    }
                    if (returnValue == "rollback") {
                        sh "kubectl patch -n od-test-prod svc od-test-prod-1 -p '{\"spec\": {\"selector\": {\"blue-green-no\": \"1\"}}}'"
                        sh "kubectl delete -f ./src/main/deployment/real/k8s/green/deployment.yaml"
                        sh "kubectl delete -f ./src/main/deployment/real/k8s/green/service.yaml"
                    }
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
