DROP DATABASE IF EXISTS identity_service;
CREATE DATABASE identity_service ENCODING 'UTF-8';

DROP DATABASE IF EXISTS identity_service_test;
CREATE DATABASE identity_service_test ENCODING 'UTF-8';

DROP USER IF EXISTS identity_service;
CREATE USER identity_service WITH PASSWORD 'password';

DROP USER IF EXISTS identity_service_test;
CREATE USER identity_service_test WITH PASSWORD 'password';
