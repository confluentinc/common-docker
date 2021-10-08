#!/usr/bin/env groovy

dockerfile {
    nodeLabel = 'docker-debian-jdk8-compose'
    dockerPush = true
    usePackages = true
    dockerRepos = ['confluentinc/cp-base-new', 'confluentinc/cp-jmxterm']
    slackChannel = '#tools-notifications'
    mvnSkipDeploy = true
    cron = ''
    cpImages = true
    osTypes = ['ubi8']
    nanoVersion = true
}
