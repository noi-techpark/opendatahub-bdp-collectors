<!--
SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>

SPDX-License-Identifier: CC0-1.0
-->

# Traffic Province BZ Data Collector


[![CI](https://github.com/noi-techpark/bdp-commons/actions/workflows/ci-traffic-provbz.yml/badge.svg)](https://github.com/noi-techpark/bdp-commons/actions/workflows/ci-trafficbz.yml)

Application which takes data from the API provided by the company Famas, parses it
and stores it in the opendatahub.

**Table of Contents**
- [Traffic Province BZ Data Collector](#traffic-provBZ-data-collector)
	- [Getting started](#getting-started)
		- [Prerequisites](#prerequisites)
		- [Configuration](#configuration)
		- [Additional information](#additional-information)

## Getting started

General instructions can be found inside the [Open Data Hub Mobility - Data
Collectors README](../../README.md). Please read that first. The following
chapters will only contain specific configuration and setup steps.

These instructions will get you a copy of the project up and running on your
local machine for development and testing purposes.

### Prerequisites

To build the project, the following prerequisites must be met:
- Everything inside [Open Data Hub Mobility - Data Collectors README](../../README.md#prerequisites)

### Configuration

The data collector consists of three schedulers:
- For syncing the traffic stations
- For syncing the traffic data
- For syncing the bluetooth data

After cloning the project setup the project as a maven project. Afterwards start the project as a spring boot application.

Important note: The bdp-core repository has to be up and running (on docker) as well.
