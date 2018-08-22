#!/usr/bin/env groovy

dockerfile {
    dockerPush = true
    dockerRepos = ['confluentinc/cp-base']
    nodeLabel = 'docker-oraclejdk7'
    slackChannel = '#tools-eng'
    upstreamProjects = ['confluentinc/confluent-docker-utils', 'confluentinc/common']
}
