# sqldb2es - SQL DB to ElasticSearch import tool
Automatically sync data from a source SQL database into a target ElasticSearch repository.

**sqldb2es** is a Java application that fetches data from a JDBC data source (tabular data) for indexing by [ElasticSearch](https://www.elastic.co/products/elasticsearch) in a structured (JSon) data format.

**sqldb2es** allows defining the structure of the *ElasticSearch* JSon objects (aka documents) by mapping them to the SQL database relational data.

## Installation

* [Download](https://bintray.com/artifact/download/takemoa/java-apps/sqldb2es-0.9-bin.zip) and unzip the sqldb2es distribution currently at version 0.9.

## Configuration

Configuration files are located in  *&lt;sqlbd2es_home&gt;/config* folder:

- Application level configuration (Elasticsearch, SQL database, data channels): _sql2es.yaml_
- Mapping between relational tables/columns and Elasticsearch documents/fields: *&lt;domain&gt;.yaml*

Please see the configuration reference [here](docs/config.md).

## Execution

Start sqldb2es by running *sql2es* (for \*nix OS) or *sql2e.bat* (on Windows) from *&lt;sqldb2es_home&gt;/bin/* directory.

<TODO image here>

By default the application will start in an infinite loop processing new/updated DB records every 30 minutes.

## Quick Start and Tutorial

This example shows how to populate and sync an Elasticsearch repository with data from *employees* MySQL reference  database.

### MySQL

- [Download](http://www.oracle.com/us/products/mysql/mysqlcommunityserver/overview/index.html) and install MySQL database for your OS.
- [Install](https://dev.mysql.com/doc/employee/en/employees-introduction.html) the *employees* MySQL sample database. It will be used as data source for this tutorial.
- The sample database needs to be altered for the purpose of this tutorial. Run *&lt;sqlbd2es_home&gt;\tutorial\sql\alter_employees_db.sql* sql script against the newly installed MySQL database. It will add an additional column *last_update_date* to the *employees* table to be used as record update timestamp.

### Elasticsearch

- [Download and install](https://www.elastic.co/downloads/elasticsearch) Elasticsearch. Current **sqldb2es** version has been tested against Elasticsearch version 1.7.3.
- Start Elasticsearch server according to the instructions.

### sqldb2es
- Modify tutorial configuration file *&lt;sqlbd2es_home&gt;/tutorial/config/sql2es.yaml* to match the MySQL server location and Elasticsearch cluster name (if different than default)

```yaml

# ES cluster name and settings. It is not required to be defined here if default settings apply.
esClusters:
  elasticsearch:
    discovery.zen.ping.multicast.enabled: false
    discovery.zen.ping.unicast.hosts.0: "localhost:9300"
    discovery.zen.ping.unicast.hosts.1: "localhost:9301"
...
# Datasources definition
datasources:
  # MySQL employee sample database
  employeesDS:
    driverClassName: com.mysql.jdbc.Driver
    url: "jdbc:mysql://127.0.0.1:3306/employees"
    # Username if not included in the URL
    username: root
    password:
```

- Start sqldb2es in tutorial mode by running *sql2es_tutorial* (for \*nix OS) or *sql2es_tutorial.bat* (on Windows) from *&lt;sqlbd2es_home&gt;/tutorial/bin/* directory. This way sqldb2es will use the tutorial configuration files located in *&lt;sqlbd2es_home&gt;/tutorial/config/* folder.

If using default tutorial settings, sqldb2es will process 1,000,000 database records every 10 minutes. Elasticsearch data is created in index *example* and type *employee*.

Mapping between MySQL DB tables/columms and Elasticsearch documents/fields is defined in *&lt;sqlbd2es_home&gt;/tutorial/config/employee.yaml* configuration file.

To view the resulting Elasticsearch JSon data structure/mappings, execute the following command in [Sense](http://localhost:9200/_plugin/marvel/sense/):

```HTTP
GET /example/_mapping/employee
```

It should result in:

```json

{
   "example": {
      "mappings": {
         "employee": {
            "properties": {
               "birthDate": {
                  "type": "date",
                  "format": "dateOptionalTime"
               },
               "channel_": {
                  "type": "string"
               },
               "departmentManagers": {
                  "properties": {
                     "departmentName": {
                        "type": "string"
                     },
                     "departmentNo": {
                        "type": "string"
                     },
                     "fromDate": {
                        "type": "date",
                        "format": "dateOptionalTime"
                     },
                     "idValue": {
                        "type": "string"
                     },
                     "toDate": {
                        "type": "date",
                        "format": "dateOptionalTime"
                     }
                  }
               },
               "departments": {
                  "properties": {
                     "departmentName": {
                        "type": "string"
                     },
                     "departmentNo": {
                        "type": "string"
                     },
                     "fromDate": {
                        "type": "date",
                        "format": "dateOptionalTime"
                     },
                     "idValue": {
                        "type": "string"
                     },
                     "toDate": {
                        "type": "date",
                        "format": "dateOptionalTime"
                     }
                  }
               },
               "employeeNo": {
                  "type": "long"
               },
               "firstName": {
                  "type": "string"
               },
               "gender": {
                  "type": "string",
                  "index": "not_analyzed",
                  "store": true
               },
               "hireDate": {
                  "type": "date",
                  "format": "dateOptionalTime"
               },
               "lastName": {
                  "type": "string"
               },
               "lastUpdateDate": {
                  "type": "date",
                  "format": "dateOptionalTime"
               },
               "salaries": {
                  "properties": {
                     "fromDate": {
                        "type": "date",
                        "format": "dateOptionalTime"
                     },
                     "idValue": {
                        "type": "string"
                     },
                     "salary": {
                        "type": "double"
                     },
                     "toDate": {
                        "type": "date",
                        "format": "dateOptionalTime"
                     }
                  }
               },
               "titles": {
                  "properties": {
                     "fromDate": {
                        "type": "date",
                        "format": "dateOptionalTime"
                     },
                     "idValue": {
                        "type": "string"
                     },
                     "title": {
                        "type": "string"
                     },
                     "toDate": {
                        "type": "date",
                        "format": "dateOptionalTime"
                     }
                  }
               }
            }
         }
      }
   }
}

```

To test the newly created employee documents in the Elasticsearch index, execute the following in [Sense](http://localhost:9200/_plugin/marvel/sense/):

```json
# Number of employee documents in the example Elasticsearch index
GET /example/employee/_search?search_type=count

# retrieve the first 50 documents sorted by employeeNo
GET /example/employee/_search?size=50
{
  "query": {
    "match_all": {}
  },
  "sort" : [
    "employeeNo"
  ]
}

```
To test the records update synchronization execute *update_employee_1.sql* and *update_employee_2.sql* sql scripts from *&lt;sqlbd2es_home&gt;/tutorial/sql/alter_employees_db.sql* folder while sqldb2es is running, then verify the updated record in [Sense](http://localhost:9200/_plugin/marvel/sense/):

```
GET /example/employee/11827
```
