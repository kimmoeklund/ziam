# ziam

Ziam is project for me to learn ZIO and FP in a selected domain. 

## Status

* work in progress

## Domain

Simple "multi-tentant" crud app, but with linked resources and relational db schema and fetch linked resources with SQL joins. Multi-tenancy here means ability run several "sites", each with their own database.

## Features

* Basic CRUD for users, roles and permissions
* Support for multiple data sources 
* CLI for provisioning new databases

## Stack 

* Scala 3 with macros
* ZIO 2, ZIO Http, Quill, ZIO Config, ZIO Logging, ZIO Prelude, ZIO JSON, ZIO Cli
* SQLite
* Twirl templates, HTMX & Tailwind CSS frontend
* Mini web framework for htmx apps, including HTML encoding data and forms with Scala 3 macros 

## Running

### Prerequisites

* Install [Scala 3.3+](https://www.scala-lang.org/download/) and cli tools with coursier

### Running

Application needs one or more databases to run. You can create a new database with the CLI:

```shell
./ziam.sh create --database newdb --username admin --password admin
```

Once you have one or more databases created, run the application:

```shell
sbt run
```

Application will start on `http://localhost:8080/<dbname-without-extension>`, i.e [http://localhost:8080/newdb](http://localhost:8080/newdb)

### Running tests

Running tests will create `unittest.db` database and run tests against it. 

```shell
sbt test
```

## [LICENSE](LICENSE)

