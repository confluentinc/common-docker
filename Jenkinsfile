#!/usr/bin/env groovy

dockerfile {
    nodeLabel = 'docker-oraclejdk8-compose-swarm'
    dockerPush = true
    usePackages = true
    dockerRepos = ['confluentinc/cp-base-new']
    slackChannel = '#tools-notifications'
    mvnSkipDeploy = true
    cron = ''
    cpImages = true
    osTypes = ['deb8', 'deb9', 'rhel8']
}
