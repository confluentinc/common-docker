#! /bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -o nounset \
    -o verbose \
    -o xtrace

echo "Testing Schema Registry Ready Command..."

# Test with help
java -cp target/docker-utils-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
     io.confluent.admin.utils.cli.SchemaRegistryReadyCommand \
     -h &> /tmp/test-sr-ready-help.log

HELP_TEST=$([ $? -eq 0 ] && echo "PASS" || echo "FAIL")

# Test with invalid host (should fail)
java -cp target/docker-utils-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
     io.confluent.admin.utils.cli.SchemaRegistryReadyCommand \
     invalid-host 8081 5 &> /tmp/test-sr-ready-invalid.log

INVALID_TEST=$([ $? -eq 1 ] && echo "PASS" || echo "FAIL")

# Test with missing arguments (should fail)
java -cp target/docker-utils-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
     io.confluent.admin.utils.cli.SchemaRegistryReadyCommand \
     localhost 8081 &> /tmp/test-sr-ready-missing-args.log

MISSING_ARGS_TEST=$([ $? -eq 1 ] && echo "PASS" || echo "FAIL")

echo "TEST RESULTS:"
echo "HELP_TEST=$HELP_TEST"
echo "INVALID_TEST=$INVALID_TEST"
echo "MISSING_ARGS_TEST=$MISSING_ARGS_TEST"

[ ${HELP_TEST} == "PASS" ] \
    && [ ${INVALID_TEST} == "PASS" ] \
    && [ ${MISSING_ARGS_TEST} == "PASS" ] \
    && exit 0 \
    || exit 1 