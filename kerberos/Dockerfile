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

FROM centos:6.9

ARG COMMIT_ID=unknown
LABEL io.confluent.docker.git.id=$COMMIT_ID
ARG BUILD_NUMBER=-1
LABEL io.confluent.docker.build.number=$BUILD_NUMBER
LABEL io.confluent.docker=true
LABEL description="Confluent Containerized KDC"

# EPEL
RUN yum install -y wget
RUN cd /tmp && wget https://dl.fedoraproject.org/pub/epel/epel-release-latest-6.noarch.rpm
RUN rpm -ivh /tmp/epel-release-latest-6.noarch.rpm

# kerberos
RUN yum install -y krb5-server krb5-libs krb5-auth-dialog krb5-workstation

EXPOSE 88 749

ADD ./config.sh /config.sh

ENTRYPOINT ["/config.sh"]
