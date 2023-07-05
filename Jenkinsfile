#!/usr/bin/env groovy

dockerfile {
    nodeLabel = 'docker-debian-jdk8-compose'
    dockerPush = true
    usePackages = true
    dockerRepos = ['confluentinc/cp-base-new', 'confluentinc/cp-base-lite', 'confluentinc/cp-jmxterm']
    slackChannel = '#release-eng'
    mvnSkipDeploy = true
    cron = ''
    cpImages = true
    osTypes = ['ubi8']
    nanoVersion = true
    buildArm = true
}
