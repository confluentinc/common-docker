#!/usr/bin/env groovy

docker_oraclejdk8 {
    dockerRegistry = '368821881613.dkr.ecr.us-west-2.amazonaws.com/'
    dockerRepos = ['confluentinc/cp-base']
    slackChannel = '#tools-eng'
    upstreamProjects = ['confluentinc/common', 'confluentinc/confluent-docker-utils'],
    withPush = true
}
