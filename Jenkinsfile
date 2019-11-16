#!/usr/bin/env groovy

dockerfile {
    dockerPush = true
    dockerRepos = ['confluentinc/cp-base-new', 'confluentinc/cp-jmxterm',
      'confluentinc/cp-kerberos']
    slackChannel = '#tools-notifications'
    upstreamProjects = ['confluentinc/confluent-docker-utils', 'confluentinc/common']
    mvnSkipDeploy = true
}
