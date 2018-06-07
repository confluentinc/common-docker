#!/usr/bin/env groovy

dockerfile {
    upstreamProjects = ['confluentinc/confluent-docker-utils', 'confluentinc/license-file-generator']
    dockerRegistry = '368821881613.dkr.ecr.us-west-2.amazonaws.com/'
    dockerRepos = ['confluentinc/cp-base']
    slackChannel = '#kafka-core-eng'
    nodeLabel = 'docker-oraclejdk7'
    dockerPush = true
}
