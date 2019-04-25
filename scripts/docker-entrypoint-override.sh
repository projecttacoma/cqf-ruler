#!/bin/sh

(
cat<<EOF
themis.postgres.host=$THEMIS_POSTGRES_HOST
themis.postgres.database=$THEMIS_POSTGRES_DB
themis.postgres.user=$THEMIS_POSTGRES_USER
themis.postgres.password=$THEMIS_POSTGRES_PASSWORD
server_address=$SERVER_ADDRESS
EOF
) >> /var/lib/jetty/webapps/hapi.properties

/docker-entrypoint.sh "$@"