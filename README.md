<!--
SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>

SPDX-License-Identifier: CC0-1.0
-->
# Deprecated
This repository is deprecated, as we are in the process of re-writing all data collectors [to an event-driven architecture](https://github.com/noi-techpark/infrastructure-v2)

New data collectors or major feature implementations should be done on the new architecture.

You can find the new data collector repository [here](https://github.com/noi-techpark/opendatahub-collectors)

# Open Data Hub Mobility - Data Collectors

[![REUSE Compliance](https://github.com/noi-techpark/bdp-commons/actions/workflows/reuse.yml/badge.svg)](https://github.com/noi-techpark/odh-docs/wiki/REUSE#badges)

The Open Data Hub Mobility Data Collectors, historically called also Big Data
Platform data collectors, and also data providers where contained in this repo,
therefore it is called common.

This repository contains the source code of all data collectors, that are Java
workers that connect to a remote data pool, such as an API, MQTT broker, FTP
server or their like, and download data, aggregate and enriches that, and
finally send it to the [Big Data Platform
writer](https://github.com/noi-techpark/bdp-core), which stores it inside a
Postgres DB.

We use [Keycloak](https://www.keycloak.org/) for authentication against the
[Open Data Hub](https://opendatahub.com/) writer API.

**Table of contents**
- [Open Data Hub Mobility - Data Collectors](#open-data-hub-mobility---data-collectors)
	- [Getting started](#getting-started)
		- [Prerequisites](#prerequisites)
		- [Source code](#source-code)
		- [Build](#build)
		- [Run tests](#run-tests)
		- [Development with Docker](#development-with-docker)
			- [VSCode: Start a debug session](#vscode-start-a-debug-session)
		- [Develop natively](#develop-natively)
		- [Credentials needed?](#credentials-needed)
	- [Instructions for Maintainers](#instructions-for-maintainers)
		- [Deploy a new data collector](#deploy-a-new-data-collector)
		- [Update pom dependencies](#update-pom-dependencies)
	- [Information](#information)
		- [Support](#support)
		- [Contributing](#contributing)
		- [Documentation](#documentation)
		- [License](#license)

## Getting started

These instructions will get you a copy of the project up and running on your
local machine for development and testing purposes. These are *just general
guidelines*, for specific details, refer to the `README.md` file in each folder.

### Prerequisites

To build the data collector project, the following prerequisites must be met:

- Java JDK 1.8 or higher (e.g. [OpenJDK](https://openjdk.java.net/))
- [Maven](https://maven.apache.org/) 3.x
- The [Open Data Hub Mobility Writer](https://github.com/noi-techpark/bdp-core)
  (aka Big Data Platform Core) installed

If you want to run the application using [Docker](https://www.docker.com/), the
environment is already set up with all dependencies for you. You only have to
install [Docker](https://www.docker.com/) and [Docker
Compose](https://docs.docker.com/compose/) and follow the instruction in the
[dedicated section](#execute-with-docker).

Hint: To be sure to have the correct Java version and build environment equal to our
infrastructure use the provided docker configuration.

### Source code

Get a copy of the repository:

```bash
git clone https://github.com/noi-techpark/bdp-commons.git
```

Change directory:

```bash
cd bdp-commons/data-collectors/[your-collector]
```

### Build

Build the project:

```bash
mvn clean package
```

### Run tests

The unit tests can be executed with the following command:

```bash
mvn clean test
```

### Development with Docker

- Inside the corresponding data collector folder, copy `.env.example` to `.env`
  and configure it
- Run `docker-compose up -d`
- You can follow the output with `docker-compose logs -f`

Please, refer to the `README.md` inside that folder for further details, and
report any incidence to `help@opendatahub.com`.

If both the Data Collector and the Open Datahub Mobility Writer are run as Docker instances during development, an additional manual Docker network must be set up to enable communication between the containers:

```
docker network create collector-dev-network
```

This network must be included in both docker-compose.yaml files and configured for use by the two services:

```yaml
networks:
  collector-dev-network:
    external: true
```

All endpoints and occurrences of localhost in the Data Collector must then be replaced by the bdp-service "bdp".

#### VSCode: Start a debug session

Copy this file to `.vsode/launch.json`:
```json
{
    "version": "0.2.0",
    "configurations": [
		{
			"type": "java",
			"name": "Attach",
			"request": "attach",
			"hostName": "0.0.0.0",
			"port": "9000",
			"justMyCode": false
		}
    ]
}
```

Run `docker-compose up -d` inside the data-collector folder of your choice, and
then launch `Attach` from VSCode. You are now ready to set breakpoints and debug.

### Develop natively

Change directory into the data collector you want.

You can set the parameters directly as environmental variables (see
`.env.example`) and start it, as follows:

1) Newer data collectors are Spring Boot applications

```bash
mvn spring-boot:run
```

...or, if you want to use your personalized Spring profile:

```bash
cd data-collectors/[your-collector]
cp src/main/resources/application.properties src/main/resources/application-local.properties
# Now open src/main/resources/application-local.properties and modify values as you like
mvn -D spring.profiles.active=local spring-boot:run
```

2) Older data collectors are Spring applications with an additional tomcat maven plugin:

```bash
mvn tomcat:run \
  -DPARAM1=... \
  -DPARAM2=... \
  -DPARAM3=...
```

...or, set them inside the relevant `.properties` files directly (see the
corresponding `README.md` for details), and run:

```bash
mvn tomcat:run
```

### Credentials needed?
You do not need special credentials for local development. Use the following
Keycloak OAuth parameters inside `application.properties` to get started
immediately (some data collectors have them already as defaults):

```ini
authorizationUri=https://auth.opendatahub.testingmachine.eu/auth
tokenUri=https://auth.opendatahub.testingmachine.eu/auth/realms/noi/protocol/openid-connect/token
BASE_URI=http://localhost:8999/json
clientId=odh-mobility-datacollector-development
clientName=odh-mobility-datacollector-development
clientSecret=7bd46f8f-c296-416d-a13d-dc81e68d0830
scope=openid
```

Or, find the corresponding variable names inside the specific `.env` files of
each data collector, if you develop with docker. Unfortunately, these were not
standardized in the past.

If you want to test it on our infrastructure directly, please read about
[Credentials in our Contributor
Guidelines](https://github.com/noi-techpark/odh-docs/wiki/Contributor-Guidelines:-Credentials).

## Instructions for Maintainers

### Deploy a new data collector

- Copy `data-collectors/helloworld/ci-helloworld.yml` to `.github/workflows/ci-your-new-datacollector.yml`
- Inside that file, replace all `helloworld` with `your-new-datacollector`
- Go to `data-collectors/your-new-datacollector`
- Check which docker servers have the least load recently, and choose them for
  testing and production inside the `infrastructure/ansible/hosts` file
- If you need to inject credentials:
  - go to [Github Actions Secrets](https://github.com/noi-techpark/bdp-commons/settings/secrets/actions)
  - create new credentials with keys in uppercase letters
    - either, prefixed with the data collector name, if they are used only there
    - or with a generic meaningful names, if you use them in more collectors
  - inject them in your Github Action Yaml like `${{ secrets.HELLOWORLD_SECRET_1 }}`

### Update pom dependencies

To update a dependency in all data-collectors the quickversionbump scripts can be used.
- quickversionbump.sh: update dc-interface
- quickversionbump-generic.sh: update any dependency
- quickversionbump-min.sh: update min version in properties, if dependency is not used but a
  minimal version is mandatory

Note: Read the comments in every script for further instructions


## Information

### Support

For support, please contact [help@opendatahub.com](mailto:help@opendatahub.com).

### Contributing

If you want to write a new Data Collector:
1) Read and follow our [Getting Started] guidelines
2) Copy/paste the [helloworld](data-collectors/helloworld/) example in a new folder under `data-collectors`, choose the name of your data collector for that folder
3) Find `TODO` comments and follow their instructions
4) See and alter code inside `SyncScheduler.java`
5) Start the writer API locally and test everything:
   - [Writer API with Docker](https://github.com/noi-techpark/bdp-core#getting-started-with-docker)
   - Start your data collector
   - Check log outputs of the writer and the data collector to find issues
   - Connect to the DB, and see what is their after some tests
6) Create a pull request as described in the guidelines above

[Getting Started]:
    https://github.com/noi-techpark/odh-docs/wiki/Contributor-Guidelines:-Getting-started

### Documentation

More documentation can be found at
[https://docs.opendatahub.com](https://docs.opendatahub.com).

### License

The code in this project is licensed under the GNU AFFERO GENERAL PUBLIC LICENSE
Version 3 license.

See the [LICENSE](LICENSE) file for more information.

### REUSE

This project is [REUSE](https://reuse.software) compliant, more information about the usage of REUSE in NOI Techpark repositories can be found [here](https://github.com/noi-techpark/odh-docs/wiki/Guidelines-for-developers-and-licenses#guidelines-for-contributors-and-new-developers).

Since the CI for this project checks for REUSE compliance you might find it useful to use a pre-commit hook checking for REUSE compliance locally. The [pre-commit-config](.pre-commit-config.yaml) file in the repository root is already configured to check for REUSE compliance with help of the [pre-commit](https://pre-commit.com) tool.

Install the tool by running:
```bash
pip install pre-commit
```
Then install the pre-commit hook via the config file by running:
```bash
pre-commit install
```
