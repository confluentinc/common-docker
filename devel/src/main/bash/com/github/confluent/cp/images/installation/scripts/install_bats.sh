#!/bin/bash

# Copyright 2021 Jack Viers

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#     http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


source "$(dirname $0)/../../colors.sh"

set -e

install_bats(){
    log_info "Checking for bats installation..."

    if ! command -v "bats" &> /dev/null;
    then
	log_error "bats not found. Installing bats..."
	brew tap kaos/shell
	brew install bats-assert
	brew install bats-file
	log_success "bats installation complete."
    else
	log_success "bats already installed."
    fi	
}

install_bats

exit 0
