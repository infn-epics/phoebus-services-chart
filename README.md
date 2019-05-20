CAVEAT: in the following the name MASAR is used for reasons of legacy. It is slightly misleading
as it suggest that the service supports restore operations (the R in MASAR). Please
not that the jmasar service does NOT provide a restore API.

The jmasar-service implements the MAchine Save And Restore service as a collection
of REST endpoints. These can be used by clients to create and manage configurations and
snapshots. 

NOTE: the R(estore) portion of the name JMasar is a bit misleading. The service does not provide
a way to restore/write PV values. However, it provides an API to retrieve PV values
that have been persisted in a snapshot at some point in time. It is then up to the client application 
to perform the restore operation.

The service depends on the jmasar-model artifact 
(https://gitlab.esss.lu.se/ics-software/jmasar-model). 
Java-based clients should also make use of jmasar-model to facilitate 
marshalling/unmarshalling of data.

Features:

* The service supports a tree structure of objects. Nodes in the tree are
folders, configurations (aka save sets) or snapshots.

* There is always a top level root node of type folder. This cannot be modified
in any manner.

* Child nodes of folder nodes are folder or configuration nodes. Child nodes
of configuration nodes are only snapshot nodes. Snapshot nodes do not contain
child nodes, but they are associated with snapshot items (PV values).

* Each node can be associated with an arbitrary number of string properties, e.g.
a "golden" property can be set on snapshot nodes.

* Each node has a created date and a last updated date, as well as a user name
attribute. This should identify the user creating or updating a node.

* Nodes in the tree can be renamed and deleted. When a folder or configuration
node is deleted, all its child nodes are deleted unconditionally.

* A folder or configuration node can be moved to another parent node. All
child nodes of the moved node remain child nodes of the object in question.

* The service is built upon Spring Boot and depends on a persistence 
implementation. In its current version, persistence is implemented against
a RDB engine, using Spring's JdbcTemplate to manage SQL queries.

* The service has been verified on Postgres 9.6 and Mysql 8.0, on Mac OS, as well as on
Postgres 9.6 on CentOS 7.4. Database 
connection parameters are found in src/main/resources. To select a database engine, add
-Ddbengine=postgresql or -Ddbengine=mysql to the command line when launching
the service.

* Flyway scripts for Postgres and Mysql are provided to set up the database. 
Flyway is run as part of the application startup, i.e. there is no need to 
run Flyway scripts manually. The Flyway scripts will create the top level folder.

* Unit tests rely on the H2 in-memory database and are independent of any
external database engine. Flyway scripts for the H2 database are found
in src/test/resources/db/migration. Running the unit tests will create the H2
database file (h2.db.mv.db) in a folder named db relative to the current directory.

* A Swagger UI is available by default.
When running the service on any other host than the local development box, also
add command line parameter -Dhostname=[IP address | FQDN].

Missing features:

* Security in terms of authentication and authorization.

* Search for configurations or snapshots.

* JPA as persistence framework, which would make transition to database engines
other than Postgresql or Mysql easier, and also hide some of the issues with
differences in SQL dialects.

Build and run:

The project was developed using Spring STS 3.9.5 (an Eclipse IDE clone).

Being a Spring Boot application, one may build from Maven command line and 
then launch the jar artifact assembled by the build system. The following 
parameters (environment variables) must be specified on the command line:

-Ddbengine=[postgresql | mysql]. 
-Dspring.datasource.username=<DB user name>
-Dspring.datasource.password=<DB password>
-Dspring.datasource.jdbcUrl=<JDBC URL e.g. jdbc:postgresql://localhost:5432/masar>

