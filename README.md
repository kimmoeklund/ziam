# ziam

Ziam is project for me to learn ZIO and FP in a selected domain. This may become a real project, probably not.

## Status

* work in progress

## Domain

Ziam's domain is fine grained authorization which you might find usefull in following use cases for example:
*  multi-tenant SaaS platform, where tenant's can collaborate and share data
*  purpose based personal data handling

## Features

* Basic CRUD for users, roles and permissions
* Support for multiple data sources 
* CLI for provisioning new databases

## Stack 

* Scala 3
* ZIO 2, ZIO Http, Quill, ZIO Config, ZIO Logging, ZIO Prelude, ZIO JSON, ZIO Cli
* SQLite
* HTMX & Tailwind CSS frontend
* Mini web framework for htmx apps, including HTML encoding data and forms with Scala 3 macros 

## Running

### Prerequisites

* Install [Scala 3.3+](https://www.scala-lang.org/download/) and cli tools with coursier

### Running

Application needs one or more databases to run. You can create a new database with the CLI:

```shell
./ziam.sh create --database new.db --user admin --password admin
```

Once you have one or more databases created, run the application:

```shell
sbt run
```

Application will start on `http://localhost:8080/<dbname-without-extension>`, i.e [http://localhost:8080/new](http://localhost:8080/new)

### Running tests

Running tests will create `unittest.db` database and run tests against it. 

```shell
sbt test
```

## [LICENSE](LICENSE)

