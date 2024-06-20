def call(Map params) {
    def config = readYaml text: libraryResource('config.groovy')
    
    // Override config with provided params if present
    config << params
    
    pipeline {
        agent any
        
        environment {
            SLACK_CHANNEL_NAME  = config.SLACK_CHANNEL_NAME
            ENVIRONMENT         = config.ENVIRONMENT
            CODE_BASE_PATH      = config.CODE_BASE_PATH
            ACTION_MESSAGE      = config.ACTION_MESSAGE
            KEEP_APPROVAL_STAGE = config.KEEP_APPROVAL_STAGE.toString()
        }
        
        stages {
            stage('Clone Repository') {
                steps {
                    script {
                        git url: 'https://your-repo-url.git', branch: 'main'
                    }
                }
            }
            
            stage('User Approval') {
                when {
                    expression { env.KEEP_APPROVAL_STAGE == 'true' }
                }
                steps {
                    input message: 'Do you want to proceed with the deployment?', ok: 'Yes'
                }
            }
            
            stage('Playbook Execution') {
                steps {
                    script {
                        sh """
                        cd ${env.CODE_BASE_PATH}
                        ansible-playbook -i inventory/hosts playbook.yml -e "env=${env.ENVIRONMENT}"
                        """
                    }
                }
            }
            
            stage('Notification') {
                steps {
                    script {
                        slackSend(channel: env.SLACK_CHANNEL_NAME, message: env.ACTION_MESSAGE)
                    }
                }
            }
        }
        
        post {
            always {
                cleanWs()
            }
        }
    }
}
