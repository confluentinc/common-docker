#!/usr/bin/env bash

#
# Copyright 2018 Confluent Inc.
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


set +o nounset

if [ -z $SKIP_MESOS_AUTO_SETUP ]; then
    if [ -n $MESOS_SANDBOX ] && [ -e $MESOS_SANDBOX/.ssl/scheduler.crt ] && [ -e $MESOS_SANDBOX/.ssl/scheduler.key ]; then
        echo "Entering Mesos auto setup for Java SSL truststore. You should not see this if you are not on mesos ..."

        openssl pkcs12 -export -in $MESOS_SANDBOX/.ssl/scheduler.crt -inkey $MESOS_SANDBOX/.ssl/scheduler.key \
                       -out /tmp/keypair.p12 -name keypair \
                       -CAfile $MESOS_SANDBOX/.ssl/ca-bundle.crt -caname root -passout pass:export

        keytool -importkeystore \
                -deststorepass changeit -destkeypass changeit -destkeystore /tmp/kafka-keystore.jks \
                -srckeystore /tmp/keypair.p12 -srcstoretype PKCS12 -srcstorepass export \
                -alias keypair

        keytool -import \
                -trustcacerts \
                -alias root \
                -file $MESOS_SANDBOX/.ssl/ca-bundle.crt \
                -storepass changeit \
                -keystore /tmp/kafka-truststore.jks -noprompt
    fi
fi

set -o nounset
