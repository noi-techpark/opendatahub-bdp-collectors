#!/bin/bash

# SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
#
# SPDX-License-Identifier: CC0-1.0

# add calls that the data collector makes to an external API here
# this makes maintenance easier

# load .env, if needed
if [ -f .env ]; then
	set -a
	source .env
	set +a
else
	printf "create .env first and fill needed vars\n"
	printf "cp .env.example .env\n"
	exit 1
fi

# define vars
API_ENDPOINT=https://mobility.api.opendatahub.com/v2/flat,node/

# calls
printf "\nexample\n"
curl -H 'Accept:application/json' $API_ENDPOINT | jq
printf "\n###############\n"
