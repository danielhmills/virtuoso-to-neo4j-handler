A custom Neo4j procedure that allows SPARQL-within-SQL queries to be returned in Neo4j without having to individually CAST each variable with an IRI or literal value.

## Installation Guide for Developers

1. Clone the git repo using `git clone {pending}`
2. Build the .JAR using `mvn clean package`
3. Copy the .JAR and included JDBC Driver to your Neo4j installation's `plugins` directory.
4. Open your `neo4j.conf` file and add `openlink.*` to your `dbms.security.procedures.unrestricted` parameter value.
5. Restart your Neo4j instance.
6. Test that the custom procedure has successfully installed by running:
```
CALL dbms.procedures() YIELD name
WHERE name STARTS WITH 'openlink'
RETURN name;
```

7. If successful, you can now use **openlink.virtuoso_jdbc_connect()**

## Querying

The Procedure uses the following parameters

* **jdbc_url**: Your JDBC URL string or variable
* **query**: Your SQL or SPARQL-within-SQL query
* **read/write (default = null)**
    * 'r' = read
    * 'rw' = read/write
    * It is recommended to apply correct privileges on your Virtuoso instance rather than fully depending on this parameter.

#### SPARQL

SPARQL queries are executed using SPARQL-within-SQL:

```
SPARQL
SELECT *
FROM <urn:analytics>
WHERE
{
 ?a foaf:knows ?b.
}
```
