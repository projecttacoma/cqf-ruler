#!/bin/sh

# TODO: we probably want to test the existence of the various variables before writing them to a file
# datasource.url= $DATASOURCE_URL
(
cat<<EOF
server_address=$SERVER_ADDRESS
themis.postgres.host=$THEMIS_POSTGRES_HOST
themis.postgres.database=$THEMIS_POSTGRES_DB
themis.postgres.user=$THEMIS_POSTGRES_USER
themis.postgres.password=$THEMIS_POSTGRES_PASSWORD
server_address=$SERVER_ADDRESS
EOF
) >> /var/lib/jetty/webapps/hapi.properties

/docker-entrypoint.sh "$@"