#!/bin/bash

source etc/init/postgres/client-local.sh

psql -f etc/flyway/init/create_databases.sql