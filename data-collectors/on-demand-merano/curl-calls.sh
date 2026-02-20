#!/bin/bash

# SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
#
# SPDX-License-Identifier: CC0-1.0

# load .env
if [ -f .env ]; then
	set -a
	source .env
	set +a
else
	printf "create .env first and fill needed vars\n"
	printf "cp .env.example .env\n"
	exit 1
fi

STOPS_PATH=/v2.0/stops
ACTIVE_ACTIVITIES_PATH=/v1.0/activities/active
POLYGONS_PATH=/v1.0/polygons

# stops
printf "\nstops\n"
curl -u $USERNAME:$PASSWORD -H 'Accept:application/json' $API_ENDPOINT$STOPS_PATH | jq
printf "\n###############\n"

# activities
printf "\nactivities\n"
curl -u $USERNAME:$PASSWORD -H 'Accept:application/json' $API_ENDPOINT$ACTIVE_ACTIVITIES_PATH | jq
printf "\n###############\n"

# polygon path
printf "\npolygons\n"
curl -u $USERNAME:$PASSWORD -H 'Accept:application/json' $API_ENDPOINT$POLYGONS_PATH | jq
printf "\n###############\n"
