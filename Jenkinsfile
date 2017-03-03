pipeline {
    agent none
    stages {
        stage('Build') {
			agent { label 'yona' }
			steps {
				withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'docker-hub',
								usernameVariable: 'DOCKER_HUB_USERNAME', passwordVariable: 'DOCKER_HUB_PASSWORD']]) {
					sh './gradlew -PdockerHubUserName=$DOCKER_HUB_USERNAME -PdockerHubPassword="$DOCKER_HUB_PASSWORD" -PdockerUrl=unix:///var/run/docker.sock build pushDockerImage'
				}
            }
			post {
				always {
					junit '**/build/test-results/*/*.xml'
				}
			}
        }
        stage('Setup test server') {
			agent { label 'yona' }
			steps {
				withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'test-db',
								usernameVariable: 'YONA_DB_USERNAME', passwordVariable: 'YONA_DB_PASSWORD']]) {
					sh 'scripts/install-test-server.sh $YONA_DB_USERNAME "$YONA_DB_PASSWORD" jdbc:mariadb://yonadbserver:3306/yona'
				}
            }
        }
        stage('Run integration tests') {
			agent { label 'yona' }
			steps {
				sh './gradlew -Pyona_appservice_url=http://185.3.209.132 -Pyona_adminservice_url=http://185.3.209.132:8080 -Pyona_analysisservice_url=http://185.3.209.132:8081 intTest'
            }
			post {
				always {
					junit '**/build/test-results/*/*.xml'
				}
			}
        }
        stage('Tag revision on GitHub') {
			agent { label 'yona' }
			steps {
				withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: '65325e52-5ec0-46a7-a937-f81f545f3c1b', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
					sh('git tag -a build-$BUILD_NUMBER -m "Jenkins"')
					sh('git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/yonadev/yona-server.git --tags')
				}
            }
        }
		stage('Decide tag on Docker Hub') {
			agent none
            steps {
                script {
                    env.TAG_ON_DOCKER_HUB = input message: 'User input required',
                            parameters: [choice(name: 'Tag on Docker Hub', choices: 'no\nyes', description: 'Choose "yes" if you want to deploy this build')]
                }
            }
        }
		stage('Tag on Docker Hub') {
			agent { label 'yona' }
			when {
				environment name: 'TAG_ON_DOCKER_HUB', value: 'yes'
			}
            steps {
				sh('scripts/retag-images.sh ${BUILD_NUMBER}')
            }
        }
    }
}