#!/usr/bin/env groovy

dockerfile {
    nodeLabel = 'docker-oraclejdk8-compose'
    dockerPush = true
    usePackages = true
    dockerRepos = ['confluentinc/cp-base-new', 'confluentinc/cp-base',
      'confluentinc/docker-utils', 'confluentinc/cp-jmxterm',
      'confluentinc/cp-kerberos']
    slackChannel = '#tools-notifications'
    upstreamProjects = ['confluentinc/confluent-docker-utils', 'confluentinc/common']
}
