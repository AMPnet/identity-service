# identity-service

## Requirements

Service must have running and initialized database. Default database url is `locahost:5432`.
To change database url set configuration: `spring.datasource.url` in file `application.properties`.
To initialize database run script in the project root folder:

```sh
./initialize-local-database.sh
```

## Start

Application is running on port: `8136`. To change default port set configuration: `server.port`.

### Build

```sh
./gradlew build
```

### Run

```sh
./gradlew bootRun
```
