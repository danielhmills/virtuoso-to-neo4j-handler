package com.openlink;

import org.neo4j.procedure.*;
import org.neo4j.logging.Log;
import virtuoso.jdbc4.VirtuosoExtendedString;
import virtuoso.jdbc4.VirtuosoRdfBox;

import java.sql.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.regex.Pattern;

public class VirtuosoJDBCHandler {

    @Context
    public Log log;

    // Extended pattern to include more write operations
    private static final Pattern WRITE_PATTERN = Pattern.compile("^(?i)\\s*(INSERT|UPDATE|DELETE|CREATE|DROP|ALTER|TRUNCATE|LOAD|(SPARQL\\s+(LOAD|CLEAR|COPY|ADD|MOVE|DELETE|DROP|INSERT|CREATE|INSERT\\s+DATA|DELETE\\s+DATA|DELETE\\s+WHERE|DEFINE\\s+get.*)))");

    @Procedure(name = "openlink.virtuoso_jdbc_connect", mode = Mode.WRITE)
    @Description("Executes a read or write query on Virtuoso and returns the results or update count")
    public Stream<MapResult> virtuosoJdbcConnect(@Name("url") String url, @Name("query") String query, @Name(value = "mode", defaultValue = "r") String mode) {
        List<Map<String, Object>> resultList = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // Default to read mode if mode is blank or null
            if (mode == null || mode.trim().isEmpty()) {
                mode = "r";
            }

            if (mode.equalsIgnoreCase("r")) {
                // Check if the query is a write operation
                if (WRITE_PATTERN.matcher(query).find()) {
                    throw new IllegalArgumentException("Write operations are not allowed in read mode.");
                }
                // Execute read query
                ResultSet rs = stmt.executeQuery(query);
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        if (value instanceof VirtuosoExtendedString) {
                            VirtuosoExtendedString ves = (VirtuosoExtendedString) value;
                            row.put(metaData.getColumnName(i), ves.str);
                        } else if (value instanceof VirtuosoRdfBox) {
                            VirtuosoRdfBox vrb = (VirtuosoRdfBox) value;
                            row.put(metaData.getColumnName(i), vrb.toString());
                        } else {
                            row.put(metaData.getColumnName(i), value != null ? value.toString() : null);
                        }
                    }
                    resultList.add(row);
                }
            } else if (mode.equalsIgnoreCase("rw")) {
                // Execute write query
                int updateCount = stmt.executeUpdate(query);
                Map<String, Object> result = new HashMap<>();
                result.put("updateCount", updateCount);
                resultList.add(result);
            } else {
                throw new IllegalArgumentException("Invalid mode. Use 'r' for read, 'rw' for read/write, or leave blank for read.");
            }
        } catch (SQLException e) {
            log.error("Error executing Virtuoso JDBC query", e);
            throw new RuntimeException(e);
        }

        return resultList.stream().map(MapResult::new);
    }

    public static class MapResult {
        public final Map<String, Object> value;

        public MapResult(Map<String, Object> value) {
            this.value = value;
        }
    }
}