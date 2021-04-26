#!/usr/bin/env groovy

dockerfile {
    nodeLabel = 'docker-oraclejdk8-compose-swarm'
    dockerPush = true
    usePackages = true
    dockerRepos = ['confluentinc/cp-base-new', 'confluentinc/cp-jmxterm']
    slackChannel = '#tools-notifications'
    mvnSkipDeploy = false
    cron = ''
    cpImages = true
    osTypes = ['ubi8']
    nanoVersion = true
}
