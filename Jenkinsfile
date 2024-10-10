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
                    currentVersion = sh(script: """
                        if kubectl get deploy od-test-prod-green -n od-test-prod >/dev/null 2>&1; then
                            echo 'green'
                        else
                            echo 'blue'
                        fi
                    """, returnStdout: true).trim()

                    nextVersion = currentVersion == "blue" ? "green" : "blue"

                    // git last commit setting (for Slack Notification)
                    LAST_COMMIT = sh(returnStdout: true, script: "git log -1 --pretty=%B").trim()
                    echo '[current version] ' + currentVersion
                    echo '[next version] ' + nextVersion
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
                    sh "docker rmi ${env.PROD_DOCKER_IMAGE_NAME}:${NOW_TIME}"
                }
            }
        }

        /*
        stage('[Master] k8s main deploy (최초 배포에만 호출하세요)') {
            when {
                branch 'master-blue_green'
            }
            steps {
                script {
                  sh """
                      sed -i 's|image: ${PROD_DOCKER_IMAGE_NAME}:.*|image: ${PROD_DOCKER_IMAGE_NAME}:${NOW_TIME}|' ./src/main/deployment/real/k8s/blue/deployment.yaml
                  """
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/blue/namespace.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/blue/configmap.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/blue/service.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/blue/hpa.yaml'
                  sh 'kubectl apply -f ./src/main/deployment/real/k8s/blue/deployment.yaml'
                }
            }
        }
        */

        stage('[Master] k8s next deploy') {
            when {
                branch 'master-blue_green'
            }
            steps {
                script {
                  sh """
                      sed -i 's|image: ${PROD_DOCKER_IMAGE_NAME}:.*|image: ${PROD_DOCKER_IMAGE_NAME}:${NOW_TIME}|' ./src/main/deployment/real/k8s/${nextVersion}/deployment.yaml
                      sed -i 's|blue-green:.*|blue-green: "${nextVersion}"|' ./src/main/deployment/real/k8s/test/service.yaml
                  """
                  sh "kubectl apply -f ./src/main/deployment/real/k8s/${nextVersion}/deployment.yaml"
                  sh "kubectl apply -f ./src/main/deployment/real/k8s/test/service.yaml"
                }
            }
        }

        stage('[Master] k8s traffic change (current -> next)') {
            when {
                branch 'master-blue_green'
            }
            steps {
                script {
                    returnValue = input message: 'traffic change', ok: "Yes", parameters: [booleanParam(defaultValue: true, name: 'IS_SWITCHED')]
                    if (returnValue) {
                        // od-test-prod 네임스페이스에서 od-test-prod 이름의 서비스를 찾습니다.
                        // 서비스의 spec.selector를 blue-green 값을 nextVersion 값으로 변경하여 새로 배포한 deployment 를 바라보게 합니다
                        sh "kubectl patch -n od-test-prod svc od-test-prod -p '{\"spec\": {\"selector\": {\"blue-green\": \"${nextVersion}\"}}}'"
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
                    sh "kubectl delete -f ./src/main/deployment/real/k8s/test/service.yaml"

                    if (returnValue == "done") {
                        sh "kubectl delete -f ./src/main/deployment/real/k8s/${currentVersion}/deployment.yaml"
                    }

                    if (returnValue == "rollback") {
                        sh "kubectl patch -n od-test-prod svc od-test-prod -p '{\"spec\": {\"selector\": {\"blue-green\": \"${currentVersion}\"}}}'"
                        sh "kubectl delete -f ./src/main/deployment/real/k8s/${nextVersion}/deployment.yaml"
                    }
                }
            }
        }
    }
}
