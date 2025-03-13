# Docker images for cp-base-java

This repo provides build files for the new base image for Confluent lightweight Docker images.
This will build the image `cp-base-java`, which is the lighter version of `cp-base-new`. 
`cub-dub` commands aren't supported in this image as Python is not installed.
Instead of `JDK`, the `JRE` is being installed.

## Properties

This project contains a Dockerfile for building *cp-base-java*, the common base image for new Confluent lightweight Docker images.

Properties are inherited from a top-level POM. Properties may be overridden on the command line (`-Ddocker.registry=testing.example.com:8080/`), or in a subproject's POM.

- *docker.skip-build*: (Optional) Set to `false` to include Docker images as part of build. Default is 'false'.
- *docker.skip-test*: (Optional) Set to `false` to include Docker image integration tests as part of the build. Requires Python 2.7, `tox`. Default is 'true'.
- *docker.registry*: (Optional) Specify a registry other than `placeholder/`. Used as `DOCKER_REGISTRY` during `docker build` and testing. Trailing `/` is required. Defaults to `placeholder/`.
- *docker.tag*: (Optional) Tag for built images. Used as `DOCKER_TAG` during `docker build` and testing. Defaults to the value of `project.version`.
- *docker.upstream-registry*: (Optional) Registry to pull base images from. Trailing `/` is required. Used as `DOCKER_UPSTREAM_REGISTRY` during `docker build`. Defaults to the value of `docker.registry`.
- *docker.upstream-tag*: (Optional) Use the given tag when pulling base images. Used as `DOCKER_UPSTREAM_TAG` during `docker build`. Defaults to the value of `docker.tag`.
- *docker.test-registry*: (Optional) Registry to pull test dependency images from. Trailing `/` is required. Used as `DOCKER_TEST_REGISTRY` during testing. Defaults to the value of `docker.upstream-registry`.
- *docker.test-tag*: (Optional) Use the given tag when pulling test dependency images. Used as `DOCKER_TEST_TAG` during testing. Defaults to the value of `docker.upstream-tag`.
- *docker.os_type*: (Optional) Specify which operating system to use as the base image by using the Dockerfile with this extension. Valid values are `ubi8`. Default value is `ubi8`.
- *docker.skip-security-update-check*: (Optional) The Dockerfile.ubi9 will run a security update check. If left to the default (false) and there is a pending security update that is not installed, then the build will fail, enforcing good security practices. If set to true, the check will pass no matter what. NOT ADVISABLE USE AT YOUR OWN RISK. Default value is `false`.

## Build arguments

- *CONFLUENT_VERSION*: (Required) Specify the full Confluent Platform release version. Example: 5.4.0
- See Dockferfile for more.


## Building

This project uses `maven-assembly-plugin` and `dockerfile-maven-plugin` to build Docker images via Maven.

To build SNAPSHOT images, configure `.m2/settings.xml` for SNAPSHOT dependencies. These must be available at build time.

```
mvn clean package -Pdocker -DskipTests # Build local images
```

## License

Usage of this image is subject to the license terms of the software contained within. Please refer to Confluent's Docker images documentation [reference](https://docs.confluent.io/platform/current/installation/docker/image-reference.html) for further information. The software to extend and build the custom Docker images is available under the Apache 2.0 License.
