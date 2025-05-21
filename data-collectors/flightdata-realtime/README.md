<!--
SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>

SPDX-License-Identifier: CC0-1.0
-->

# Realtime Flightdata Data Collector

[![CI](https://github.com/noi-techpark/bdp-commons/actions/workflows/ci-flightdata-realtime.yml/badge.svg)](https://github.com/noi-techpark/bdp-commons/actions/workflows/ci-flightdata-realtime.yml)

This data collector collects real-time flight data to obtain any deviations from the scheduled takeoff and landing times in real-time.

**Table of Contents**
- [Realtime Flightdata Data Collector](#realtime-flightdata-data-collector)
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

This data collector requires 2 new environment variables:

ODH_CLIENT_ENDPOINT: REST endpoint for retrieving real-time data
ODH_CLIENT_TOKEN: Bearer token for authentication

### Additional information

This data collector collects real-time flight data to obtain any deviations from the scheduled takeoff and landing times in real-time.
The flightdata-skyalps data collector can be used to retrieve scheduled flight routes. This data collector supplements the flight data retrieved via flightdata-skyalps with real-time information, including actual takeoff and landing times on site.


