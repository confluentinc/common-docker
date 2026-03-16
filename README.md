# Common Docker Utilities

See [base image](./base/README.md), [utility belt](./utility-belt/README.md), [base-lite image](./base-lite/README.md)

## Building Images Locally

This repository and other Confluent Platform Docker image repositories use Maven to build Docker images. The sections below describe how to correctly build images from source.

### Use Post Branches

When building images locally, always use **post branches** (e.g., `7.7.2-post`, `8.0.1-post`, `8.2.0-post`). These correspond to released versions of Confluent Platform for which public artifacts (JARs and RPM/DEB packages) are available.

Do **not** use `master`, `8.x`, or `7.x` branches for local builds. These are active development branches and their dependencies are not published to any public repository.

### Maven `settings.xml` Configuration

The build pulls parent POM dependencies from Confluent's public Maven repository. Ensure your `~/.m2/settings.xml` includes the Confluent repository:

```xml
<settings>
  <profiles>
    <profile>
      <id>confluent</id>
      <repositories>
        <repository>
          <id>confluent</id>
          <url>https://packages.confluent.io/maven/</url>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>confluent</id>
          <url>https://packages.confluent.io/maven/</url>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>confluent</activeProfile>
  </activeProfiles>
</settings>
```

### Build Command

The standard build command shown in many of the Docker image repos:

```
mvn clean package -Pdocker -DskipTests
```

will fail unless you also supply the required package repository URL and Confluent version. The packages installed inside the Docker images are pulled from `packages.confluent.io`, so these build arguments must be provided.

Use the following command instead, replacing the version to match the post branch you checked out:

```bash
mvn clean package -Pdocker -DskipTests \
    -DCONFLUENT_PACKAGES_REPO='https://packages.confluent.io/rpm/8.2' \
    -DCONFLUENT_VERSION='8.2.0'
```

For example, if you are on the `7.7.2-post` branch:

```bash
mvn clean package -Pdocker -DskipTests \
    -DCONFLUENT_PACKAGES_REPO='https://packages.confluent.io/rpm/7.7' \
    -DCONFLUENT_VERSION='7.7.2'
```

The `CONFLUENT_PACKAGES_REPO` URL format is `https://packages.confluent.io/rpm/<major>.<minor>` and should match the version of the branch you are building.

### Common Build Failure

Running the build without `-DCONFLUENT_PACKAGES_REPO` and `-DCONFLUENT_VERSION` will result in an error like:

```
[ERROR] Failed to execute goal com.spotify:dockerfile-maven-plugin:...:build (package) on project ...:
Could not build image: The command '/bin/sh -c echo "===> Installing ${COMPONENT}..." ...' returned a non-zero code: 1
```

This happens because the Dockerfile tries to install RPM packages from a repository URL that is not set or defaults to an inaccessible location.

## Related Confluent Platform Docker Image Repositories

- [common-docker](https://github.com/confluentinc/common-docker) (this repository)
- [kafka-images](https://github.com/confluentinc/kafka-images)
- [schema-registry-images](https://github.com/confluentinc/schema-registry-images)
- [kafka-rest-images](https://github.com/confluentinc/kafka-rest-images)
- [control-center-images](https://github.com/confluentinc/control-center-images)
- [ksql-images](https://github.com/confluentinc/ksql-images)
- [kafka-replicator-images](https://github.com/confluentinc/kafka-replicator-images)
- [kafka-mqtt-images](https://github.com/confluentinc/kafka-mqtt-images)
- [kafkacat-images](https://github.com/confluentinc/kafkacat-images)
- [kafka-streams-examples](https://github.com/confluentinc/kafka-streams-examples)

The build instructions above apply to all of these repositories.
