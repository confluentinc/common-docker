#
# Copyright 2017 Confluent Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM registry.access.redhat.com/ubi8/ubi-minimal:latest

ARG PROJECT_VERSION
ARG ARTIFACT_ID

# Remember where we came from
LABEL io.confluent.docker.git.repo="confluentinc/common"

ARG COMMIT_ID=unknown
LABEL io.confluent.docker.git.id=$COMMIT_ID

ARG BUILD_NUMBER=-1
LABEL io.confluent.docker.build.number=$BUILD_NUMBER

LABEL maintainer="tools@confluent.io"
LABEL io.confluent.docker=true

# This affects how strings in Java class files are interpreted.  We want UTF-8 and this is the only locale in the
# base image that supports it
ENV LANG="C.UTF-8"

# Set the classpath for JARs required by `cub`
ENV CUB_CLASSPATH='"/usr/share/java/cp-base-new/*"'

# Zulu openJDK
ENV ZULU_OPENJDK="zulu8.40.0.25-ca-jdk8.0.222-linux_x64"

COPY requirements.txt .

RUN microdnf install yum \
    && yum update -q -y \
    && yum install -y git wget nc python3 gcc libffi-devel redhat-rpm-config python3-devel openssl-devel make tar procps krb5-workstation \
    && pip3 install --install-option="--prefix=/usr/local" --upgrade -rrequirements.txt \
    && mkdir -p /usr/lib/jvm \
    && cd /usr/lib/jvm \
    && curl -O https://cdn.azul.com/zulu/bin/${ZULU_OPENJDK}.tar.gz \
    && tar -xzvf ${ZULU_OPENJDK}.tar.gz \
    && rm -rf ${ZULU_OPENJDK}.tar.gz \
    && cd / \
    && ln -s /usr/lib/jvm/${ZULU_OPENJDK}/bin/java /usr/bin/java \
    && export JAVA_HOME=/usr/lib/jvm/${ZULU_OPENJDK} \
    && export PATH=$JAVA_HOME/bin:$PATH \
    && yum remove -y git \
    && yum clean all \
    && rm -rf /tmp/* \
    && rm -f requirements.txt \
    && mkdir -p /etc/confluent/docker

ADD target/${ARTIFACT_ID}-${PROJECT_VERSION}-package/share/doc/* /usr/share/doc/${ARTIFACT_ID}/
ADD target/${ARTIFACT_ID}-${PROJECT_VERSION}-package/share/java/${ARTIFACT_ID}/* /usr/share/java/${ARTIFACT_ID}/

COPY include/etc/confluent/docker /etc/confluent/docker
