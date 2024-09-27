pipeline {
    agent any

    // environment variable setting
    environment {
      PROD_DOCKER_IMAGE_NAME = 'akkessun/od-test-prod'
      LAST_COMMIT = ""
      TODAY = new Date().format('yyyy-MM-dd')
    }

    stages {
      stage('[Master] Jenkins variable setting'){
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

      stage('[Master] Jar & image Build'){
        when {
          branch 'master'
        }
        steps {
            script {
                sh 'gradle clean build -Pprofile=real'
                sh "docker build -t ${env.PROD_DOCKER_IMAGE_NAME}:${TODAY} ."
            }
        }
      }

      stage('[Master] Docker Hub deploy'){
        when {
          branch 'master'
        }
        steps {
            script {
                sh "docker login -u ${dockerUsername} -p ${dockerPassword}"
                sh "docker push ${PROD_DOCKER_IMAGE_NAME}:${TODAY}"
                sh "docker logout"
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