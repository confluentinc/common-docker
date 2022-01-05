
[//]: # (Copyright 2021 Jack Viers)

[//]: # ( )

[//]: # (   Licensed under the Apache License, Version 2.0 \(the "License"\);)

[//]: # (   you may not use this file except in compliance with the License.)

[//]: # (   You may obtain a copy of the License at)

[//]: # ( )

[//]: # (       http://www.apache.org/licenses/LICENSE-2.0)

[//]: # ( )

[//]: # (   Unless required by applicable law or agreed to in writing, software)

[//]: # (   distributed under the License is distributed on an "AS IS" BASIS,)

[//]: # (   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.)

[//]: # (   See the License for the specific language governing permissions and)

[//]: # (   limitations under the License.)
   
# docker-common
The cross-platform build of cp-base-new

## Build

This project will attempt to do everything via `make` and `bash`, in a
compatible manner for `docker` without the experimental `buildx`
support. I have `podman` running in `qemu` on a mac M1. Docker Desktop
is non-free now, so builds have to be docker command compatible
without relying upon special features enabled by Docker Desktop in
order to be truly portable for maintainers and committers on multiple
platforms.

### Prerequisites

#### Mac

1. [brew](https://brew.sh/)
2.  [podman](https://podman.io/)

Install podman 3.4.2 -- newest version in podman cannot build images on macOS
as it errors out with an incorrect vm temp directory location (stat error).
You must give the vm at least 2 cpus, or it will
fail in building cp-base-new during pynacl compilation.

	```shell
	brew tap-new $USER/local-podman
	brew extract --version=3.4.2 podman $USER/local-podman
	HOMEBREW_NO_AUTO_UPDATE=1 brew install $USER/local-podman/podman@3.4.2
	ln -s /opt/homebrew/opt/podman@3.4.2/libexec /opt/homebrew/opt/podman/
    podman machine init --cpus 2 --disk-size 50 
	podman machine start
    podman machine ssh
    sudo -i
    rpm-ostree install qemu-user-static'
    systemctl reboot
    ```
3. bash
4. make
4. A docker repository running somewhere other than docker-hub, for
   local development only. See `Local Development` for additional
   instructions.
   
##### Troubleshooting Build on M1

If your podman fails in make make-devel, you may need to [install the patched
version of podman](https://edofic.com/posts/2021-09-12-podman-m1-amd64/).
   
#### Ubuntu

1. [brew](https://brew.sh/)
2.  [podman](https://podman.io/)

    ```shell
	. /etc/os-release
	echo "deb https://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/xUbuntu_${VERSION_ID}/ /" | sudo tee /etc/apt/sources.list.d/devel:kubic:libcontainers:stable.list
	curl -L "https://download.opensuse.org/repositories/devel:/kubic:/libcontainers:/stable/xUbuntu_${VERSION_ID}/Release.key" | sudo apt-key add -
	sudo apt update
	sudo apt -y upgrade
	sudo apt install -y podman	
    ```
3. qemu 5

	```shell
	sudo add-apt-repository ppa:jacob/virtualisation
	sudo apt update
	sudo apt install -y qemu qemu-user-static
	```

3. bash
4. make
4. A docker repository running somewhere other than docker-hub, for
   local development only. See `Local Development` for additional
   instructions.

### Local Development

### CUSTOMIZATION using `.local.make`

You can override the variables defined in the `Makefile` by creating
the git ignored file `.local.make` in the top level directory of this
repo, rather than using environment variables. This file is ignored by
git so allows you to set things only for your local builds.

#### Ubuntu

```Makefile
BATS_INSTALL_SCRIPT_LOCATION=./devel/src/main/bash/com/github/jackcviers/confluent/cp/images/installation/scripts/install_bats_ubuntu.sh
BATS_LIBS_INSTALL_LOCATION=/usr/local/lib
IMAGES_BUILD_TOOL=podman
```

### BUILDING LOCALLY

    $ make make-devel

### TESTS

Tests are run with [bats](https://bats-core.readthedocs.io/en/stable/), and
exec to get into a container to test for the existence of things. They
can be quite slow to execute. You can skip tests in make devel by
setting `DEVEL_SKIP_TESTS` to `true`.

#### ADDITIONAL CUSTOMIZATION

1. The shell scripts executed by `make` are tested with `bats` where
possible. `make devel` will run the installation for bats on osx using
homebrew. You can change how `bats` is installed by providing a
different bats installation script in the
`BATS_INSTALL_SCRIPT_LOCATION` 
variable. The default behaviour is to see if bats is available on the
system before installing and installing with `homebrew`.

## VERSIONING

If possible, all versions will follow the Confluent CP platform versions.
