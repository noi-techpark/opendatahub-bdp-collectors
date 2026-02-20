<!--
SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>

SPDX-License-Identifier: CC0-1.0
-->

# On-Demand Merano Data Collector

Application which takes data of the MENTOR project from the platform cube4t8, parses it and sends it to the opendatahub.

[![CI on-demand-merano](https://github.com/noi-techpark/bdp-commons/actions/workflows/ci-on-demand-merano.yml/badge.svg)](https://github.com/noi-techpark/bdp-commons/actions/workflows/ci-on-demand-merano.yml)
[![CI on-demand-badia](https://github.com/noi-techpark/bdp-commons/actions/workflows/ci-on-demand-badia.yml/badge.svg)](https://github.com/noi-techpark/bdp-commons/actions/workflows/ci-on-demand-badia.yml)

## Table of contents

- [Gettings started](#getting-started)
- [Running tests](#running-tests)
- [Deployment](#deployment)
- [Information](#information)

## Getting started

These instructions will get you a copy of the project up and running on your local machine for development and testing
purposes.

### Prerequisites

To build the project, the following prerequisites must be met:

- Java JDK 1.8 or higher (e.g. [OpenJDK](https://openjdk.java.net/))
- [Maven](https://maven.apache.org/) 3.x
- A postgres database with the schema intimev2 and elaboration (bdp) already installed

### Source code

Get a copy of the repository:

```bash
git clone git clone https://github.com/noi-techpark/bdp-commons
```

Change directory:

```bash
cd bdp-commons/data-collectors/on-demand-merano
```

### Build

Build the project:

```bash
mvn clean package
```

## Running tests

The unit tests can be executed with the following command:

```bash
mvn clean test
```

## Deployment

This is a maven project and will produce a war that can be deployed in any j2ee container like tomcat or jetty.

Steps:

* change the file src/main/resources/connector.properties. set the url, the username and the
  password to connect to the platform cube4t8 (or configure it within a CI tool)

```
url=
user=
password=
```

* optionally change the origin, the station types, periods (in seconds) in
  the file src/main/resources/ondemandmerano.properties. (or configure it within a CI tool)

```
origin=ON_DEMAND_MERANO
stop.stationtype=ON_DEMAND_STOP
vehicle.stationtype=ON_DEMAND_VEHICLE
vehicle.period=1
itinerary.stationtype=ON_DEMAND_ITINERARY
itinerary.period=1
polygon.category=ON_DEMAND_MERANO
polygon.uuid-prefix=9b519da7-ece1-4477-ac2c
```

* optionally change the data types in the file src/main/resources/datatypes.properties.
  (or configure it within a CI tool)

```
position={key: 'position', unit: 'Point', description: 'position', rtype: 'Instantaneous'}
itinerary_details={key: 'itinerary_details', unit: 'json', description: 'itinerary_details', rtype: 'Instantaneous'}
...
```

* configure the log4j.properties file as desidered

## Information

### Support

For support, please contact [info@opendatahub.com](mailto:info@opendatahub.com).

### Contributing

If you'd like to contribute, please follow the following instructions:

- Fork the repository.

- Checkout a topic branch from the `development` branch.

- Make sure the tests are passing.

- Create a pull request against the `development` branch.

### Documentation

More documentation can be found
at [https://opendatahub.readthedocs.io/en/latest/index.html](https://opendatahub.readthedocs.io/en/latest/index.html).

### License

The code in this project is licensed under the GNU AFFERO GENERAL PUBLIC LICENSE Version 3 license. See
the [LICENSE.md](LICENSE.md) file for more information.
