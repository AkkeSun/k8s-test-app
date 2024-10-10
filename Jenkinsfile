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

        CURRENT_VERSION = sh(script: """
            kubectl get svc od-test-prod -n od-test-prod -o=jsonpath='{.spec.selector.blue-green}' || echo 'blue'
        """, returnStdout: true).trim()
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
                    echo '[current version] ' + CURRENT_VERSION
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
                    sh "docker build -t ${env.PROD_DOCKER_IMAGE_NAME}:${NOW_TIME} ."
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
                    sh "docker push ${env.PROD_DOCKER_IMAGE_NAME}:${NOW_TIME}"
                    sh "docker logout"
                }
            }
        }

        stage('[Master] k8s main deploy (최초 배포에만 호출하세요)') {
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

        stage('[Master] k8s next deploy') {
            when {
                branch 'master-blue_green'
            }
            steps {
                script {
                  NEXT_VERSION = CURRENT_VERSION == "blue" ? "green" : "blue"

                  sh """
                      sed -i 's|image: ${PROD_DOCKER_IMAGE_NAME}:.*|image: ${PROD_DOCKER_IMAGE_NAME}:${NOW_TIME}|' ./src/main/deployment/real/k8s/${NEXT_VERSION}/deployment.yaml
                  """
                  sh "kubectl apply -f ./src/main/deployment/real/k8s/${NEXT_VERSION}/deployment.yaml"
                  sh "kubectl apply -f ./src/main/deployment/real/k8s/${NEXT_VERSION}/service.yaml"
                }
            }
        }

        stage('[Master] k8s traffic change (current -> next)') {
            when {
                branch 'master-blue_green'
            }
            steps {
                script {
                    NEXT_VERSION = CURRENT_VERSION == "blue" ? "green" : "blue"

                    isTrafficChange = input message: "Switch traffic to version ${NEXT_VERSION}?", ok: "Yes"
                    if (isTrafficChange) {
                        // od-test-prod 네임스페이스에서 od-test-prod 이름의 서비스를 찾습니다.
                        // 서비스의 spec.selector를 blue-green 값을 sub 값으로 변경하여 이제 sub 에 배포한 deployment 를 바라보게 합니다
                    sh "kubectl patch -n od-test-prod svc od-test-prod -p '{\"spec\": {\"selector\": {\"blue-green\": \"${NEXT_VERSION}\"}}}'"
                    }
                }
            }
        }

        stage('[Master] k8s deploy check') {
            when {
                branch 'master-blue_green'
            }
            steps {
                script {
                    returnValue = input message: 'Needs rollback?', parameters: [choice(choices: ['done', 'rollback'], name: 'IS_ROLLBACK')]
                    NEXT_VERSION = CURRENT_VERSION == "blue" ? "green" : "blue"

                    if (returnValue == "done") {
                        sh "kubectl delete -f ./src/main/deployment/real/k8s/${CURRENT_VERSION}/deployment.yaml"
                        sh "kubectl delete -f ./src/main/deployment/real/k8s/${NEXT_VERSION}/service.yaml"
                    }

                    if (returnValue == "rollback") {
                        sh "kubectl patch -n od-test-prod svc od-test-prod -p '{\"spec\": {\"selector\": {\"blue-green\": \"${CURRENT_VERSION}\"}}}'"
                        sh "kubectl delete -f ./src/main/deployment/real/k8s/${NEXT_VERSION}/deployment.yaml"
                        sh "kubectl delete -f ./src/main/deployment/real/k8s/${NEXT_VERSION}/service.yaml"
                    }
                }
            }
        }
    }
}
