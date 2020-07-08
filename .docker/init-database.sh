#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL

    create database worm_tr_db;
    create user worm_tr_user with encrypted password 'tr_pass';
    grant all privileges on database worm_tr_db to worm_tr_user;

    create database worm_de_db;
    create user worm_de_user with encrypted password 'de_pass';
    grant all privileges on database worm_de_db to worm_de_user;

EOSQL