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
	return_dir=$(pwd)
	mkdir .bats-install
	cd ./.bats-install
	git clone https://github.com/bats-core/bats-core.git
	cd bats-core
	sudo ./install.sh /usr/local
	cd -
	sudo git clone https://github.com/bats-core/bats-support.git \
	     --depth=1 \
	     --branch master \
	     --single-branch \
	     ${BATS_LIBS_INSTALL_LOCATION}/bats-support
	sudo git clone https://github.com/ztombol/bats-assert.git \
	     --depth=1 \
	     --branch master \
	     --single-branch \
	     ${BATS_LIBS_INSTALL_LOCATION}/bats-assert
	sudo git clone https://github.com/bats-core/bats-file.git \
	    --depth=1 \
	    --branch master \
	    --single-branch \
	    ${BATS_LIBS_INSTALL_LOCATION}/bats-file

	cd $return_dir
	rm -rf ./.bats-install
	echo "${BATS_LIBS_INSTALL_LOCATION}"
	ls -al ${BATS_LIBS_INSTALL_LOCATION}
	log_success "bats installation complete."
    else
	log_success "bats already installed."
    fi
}

install_bats

exit 0
