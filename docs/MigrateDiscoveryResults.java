import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MigrateDiscoveryResults {

    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    public static void main(String[] args) {
        try {
            if (args.length == 1 && args[0].equals("--self-test")) {
                selfTest();
                return;
            }
            if (args.length < 2) {
                throw new IllegalArgumentException("Usage: MigrateDiscoveryResults.java <absolute-db-base> --dry-run|--apply|--verify <backup-db-base>");
            }
            Path database = databaseBase(args[0]);
            if (args.length == 3 && args[1].equals("--verify")) {
                verifyBackup(databaseBase(args[2]), database);
                return;
            }
            if (args.length != 2 || (!args[1].equals("--apply") && !args[1].equals("--dry-run"))) {
                throw new IllegalArgumentException("Expected --dry-run, --apply, or --verify <backup-db-base>.");
            }
            boolean apply = args[1].equals("--apply");
            Outcome outcome;
            try (Connection connection = open(database, !apply)) {
                outcome = migrate(connection, apply);
            }
            if (apply) {
                try (Connection connection = open(database, true)) {
                    requireEqual(outcome.expected(), snapshot(connection, null), "Verification after reconnect failed.");
                    requireEmpty(plan(connection));
                }
            }
            report(args[1], outcome.plan());
            System.out.println(apply ? "Verified table contents, row counts, schema, preserved columns and idempotence."
                    : "Preflight only; no data changes written.");
        } catch (Exception exception) {
            if (exception instanceof SQLException sqlException) {
                System.err.println("FAILED: JDBC SQLState=" + sqlException.getSQLState() + ", code=" + sqlException.getErrorCode());
            } else {
                System.err.println("FAILED: " + exception.getMessage());
            }
            System.exit(1);
        }
    }

    private static void selfTest() throws Exception {
        try (Connection connection = fixture()) {
            Plan changes = plan(connection);
            check(changes.indexClears().equals(Set.of("a", "b", "d", "e")), "Incorrect latest-task/index selection.");
            check(changes.counts().get("JSON_NULL_TO_SQL_NULL") == 5 &&
                    changes.counts().get("NOT_FOUND_TO_SQL_NULL") == 1 &&
                    changes.counts().get("AMBIGUOUS_TO_SQL_NULL") == 2 &&
                    changes.counts().get("FOUND_STATUS_REMOVED") == 3, "Incorrect transformation counts.");
            check(!changes.taskUpdates().containsKey("t2") && !changes.taskUpdates().containsKey("t3") &&
                    !changes.taskUpdates().containsKey("t4"), "Non-null JSON results were changed.");
            Snapshot before = snapshot(connection, null);
            migrate(connection, false);
            requireEqual(before, snapshot(connection, null), "Dry run modified data.");
            migrate(connection, true);
            requireEmpty(plan(connection));
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT result FROM discovery_task WHERE id = 'c2'")) {
                check(rows.next(), "FOUND fixture missing.");
                JsonNode data = JSON.readTree(rows.getString(1));
                check(!data.has("status") && data.get("biography").isNull() &&
                        data.get("artist").get("status").asString().equals("nested"), "JSON values were not preserved.");
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT spotify_artist_id, music_brainz_artist_id, country_code FROM artist_discovery_index WHERE id = 'c'")) {
                check(rows.next() && rows.getString(1).equals("keep-c") &&
                        rows.getString(2).equals("mb-c") && rows.getString(3).equals("DE"), "Newer FOUND index was changed.");
            }
        }
        try (Connection connection = fixture(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE artist_discovery_index ADD CONSTRAINT prevent_clear CHECK (spotify_artist_id IS NOT NULL)");
            Snapshot before = snapshot(connection, null);
            boolean failed = false;
            try {
                migrate(connection, true);
            } catch (SQLException expected) {
                failed = true;
            }
            check(failed, "Expected an index update failure.");
            requireEqual(before, snapshot(connection, null), "Task updates were not rolled back after an index failure.");
        }
        for (String invalid : List.of("not JSON", "{\"status\":\"UNKNOWN\"}", "{\"status\":\"FOUND\"}")) {
            try (Connection connection = fixture(); PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO discovery_task VALUES ('invalid', 'a', 'SPOTIFY_ARTIST_DATA', 'COMPLETE', ?, TIMESTAMP '2026-10-29 12:00:00', NULL, 'request', 'response')")) {
                insert.setString(1, invalid);
                insert.executeUpdate();
                Snapshot before = snapshot(connection, null);
                boolean failed = false;
                try {
                    migrate(connection, true);
                } catch (IllegalStateException expected) {
                    failed = true;
                }
                check(failed, "Invalid data was accepted.");
                requireEqual(before, snapshot(connection, null), "Preflight failure modified data.");
            }
        }
        System.out.println("SELF-TEST PASSED: transformations, ordering, index preservation, idempotence, preflight and transaction rollback.");
    }

    private static Connection fixture() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:migration_" + UUID.randomUUID(), "sa", "");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE discovery_task (id VARCHAR PRIMARY KEY, artist_id VARCHAR, type VARCHAR, status VARCHAR, result VARCHAR, creation_date TIMESTAMP, update_date TIMESTAMP, raw_request VARCHAR, raw_result VARCHAR)");
            statement.execute("CREATE TABLE artist_discovery_index (id VARCHAR PRIMARY KEY, artist_id VARCHAR UNIQUE, spotify_artist_id VARCHAR, music_brainz_artist_id VARCHAR, country_code VARCHAR, creation_date TIMESTAMP, update_date TIMESTAMP)");
            statement.execute("CREATE TABLE discovery_cache (id INT PRIMARY KEY, payload BLOB)");
            statement.execute("INSERT INTO discovery_cache VALUES (1, X'0001FF')");
            for (String artist : List.of("a", "b", "c", "d", "e", "g")) {
                statement.execute("INSERT INTO artist_discovery_index VALUES ('" + artist + "', '" + artist + "', 'keep-" + artist + "', 'mb-" + artist + "', 'DE', TIMESTAMP '2026-10-01 00:00:00', NULL)");
            }
        }
        String found = "{\"status\":\"FOUND\",\"artist\":{\"id\":\"artist\",\"name\":\"Artist\",\"url\":\"url\",\"status\":\"nested\"},\"matchedAlbum\":{\"inputTitle\":\"Album\",\"title\":\"Album\",\"id\":\"album\",\"url\":\"url\"},\"biography\":null,\"links\":[],\"extra\":[null]}";
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO discovery_task VALUES (?, ?, ?, ?, ?, ?, TIMESTAMP '2026-10-28 00:00:00', 'request', 'response')")) {
            List<String[]> tasks = List.of(
                    new String[]{"a1", "a", "SPOTIFY_ARTIST_DATA", "COMPLETE", found, "2026-10-01 00:00:00"},
                    new String[]{"a2", "a", "SPOTIFY_ARTIST_DATA", "COMPLETE", "null", "2026-10-02 00:00:00"},
                    new String[]{"b1", "b", "SPOTIFY_ARTIST_DATA", "COMPLETE", "{\"status\":\"NOT_FOUND\"}", "2026-10-01 00:00:00"},
                    new String[]{"b2", "b", "SPOTIFY_ARTIST_DATA", "FAILED", "{\"error\":\"failure\"}", "2026-10-03 00:00:00"},
                    new String[]{"c1", "c", "SPOTIFY_ARTIST_DATA", "COMPLETE", "{\"status\":\"AMBIGUOUS\"}", "2026-10-01 00:00:00"},
                    new String[]{"c2", "c", "SPOTIFY_ARTIST_DATA", "COMPLETE", found, "2026-10-02 00:00:00"},
                    new String[]{"d1", "d", "SPOTIFY_ARTIST_DATA", "COMPLETE", found, "2026-10-01 00:00:00"},
                    new String[]{"d2", "d", "SPOTIFY_ARTIST_DATA", "COMPLETE", "{\"status\":\"AMBIGUOUS\"}", "2026-10-01 00:00:00"},
                    new String[]{"e1", "e", "SPOTIFY_ARTIST_DATA", "COMPLETE", null, "2026-10-01 00:00:00"},
                    new String[]{"f1", "f", "SPOTIFY_ARTIST_DATA", "COMPLETE", "null", "2026-10-01 00:00:00"},
                    new String[]{"g1", "g", "SPOTIFY_ARTIST_DATA", "STARTED", "null", "2026-10-01 00:00:00"},
                    new String[]{"t1", "a", "SPOTIFY_TOP_TRACKS", "COMPLETE", "null", "2026-10-01 00:00:00"},
                    new String[]{"m1", null, "MUSICBRAINZ_ALBUM_DATA", "COMPLETE", "null", "2026-10-01 00:00:00"},
                    new String[]{"t2", "c", "SPOTIFY_TOP_TRACKS", "COMPLETE", "[]", "2026-10-01 00:00:00"},
                    new String[]{"t3", "d", "SPOTIFY_TOP_TRACKS", "COMPLETE", "[\"track\",null]", "2026-10-01 00:00:00"},
                    new String[]{"t4", "e", "SPOTIFY_TOP_TRACKS", "COMPLETE", "\"null\"", "2026-10-01 00:00:00"});
            for (String[] task : tasks) {
                for (int index = 0; index < task.length; index++) {
                    insert.setString(index + 1, task[index]);
                }
                insert.executeUpdate();
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("UPDATE discovery_task SET update_date = TIMESTAMP '2026-10-29 12:00:00' WHERE id IN ('a1', 'c1')");
        }
        return connection;
    }

    private static Path databaseBase(String argument) {
        Path base = Path.of(argument);
        check(base.isAbsolute(), "Supply an absolute database base path.");
        base = base.normalize();
        check(!argument.endsWith(".mv.db") && !argument.contains(";"), "Supply the base path without .mv.db or JDBC settings.");
        check(Files.isRegularFile(Path.of(base + ".mv.db")), "The existing .mv.db file was not found.");
        return base;
    }

    private static Connection open(Path database, boolean readOnly) throws SQLException {
        String url = "jdbc:h2:file:" + database.toString().replace('\\', '/') + ";IFEXISTS=TRUE";
        if (readOnly) {
            url += ";ACCESS_MODE_DATA=r";
        }
        return DriverManager.getConnection(url, System.getenv().getOrDefault("PONY_MIGRATION_USER", "sa"),
                System.getenv().getOrDefault("PONY_MIGRATION_PASSWORD", ""));
    }

    private static void verifyBackup(Path backup, Path database) throws Exception {
        check(!backup.equals(database), "Backup and migrated database must be different files.");
        try (Connection before = open(backup, true); Connection after = open(database, true)) {
            Plan changes = plan(before);
            requireEqual(snapshot(before, changes), snapshot(after, null), "Migrated database differs from the expected backup transformation.");
            requireEmpty(plan(after));
            report("--verify", changes);
            System.out.println("Verified all public table data and schema against the backup.");
        }
    }

    private static Plan plan(Connection connection) throws Exception {
        Map<String, String> updates = new LinkedHashMap<>();
        Map<String, Task> latest = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String key : List.of("TASK_ROWS", "ALREADY_SQL_NULL", "JSON_NULL_TO_SQL_NULL", "NOT_FOUND_TO_SQL_NULL", "AMBIGUOUS_TO_SQL_NULL", "FOUND_STATUS_REMOVED")) {
            counts.put(key, 0L);
        }
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(
                "SELECT id, artist_id, type, status, result FROM discovery_task ORDER BY creation_date DESC, id DESC")) {
            while (rows.next()) {
                String id = rows.getString("id");
                String artistId = rows.getString("artist_id");
                String type = rows.getString("type");
                String status = rows.getString("status");
                String result = rows.getString("result");
                increment(counts, "TASK_ROWS");
                if ("SPOTIFY_ARTIST_DATA".equals(type) && "COMPLETE".equals(status) && artistId != null) {
                    latest.putIfAbsent(artistId, new Task(id, result));
                }
                if (result == null) {
                    increment(counts, "ALREADY_SQL_NULL");
                    continue;
                }
                if (result.equals("null")) {
                    updates.put(id, null);
                    increment(counts, "JSON_NULL_TO_SQL_NULL");
                    continue;
                }
                JsonNode tree;
                try {
                    tree = JSON.readTree(result);
                } catch (RuntimeException exception) {
                    throw new IllegalStateException("Invalid JSON in task " + id + ".");
                }
                if (!"SPOTIFY_ARTIST_DATA".equals(type)) {
                    continue;
                }
                check(tree instanceof ObjectNode, "Expected a Spotify JSON object in task " + id + ".");
                ObjectNode object = (ObjectNode) tree;
                JsonNode responseStatus = object.get("status");
                if (responseStatus == null) {
                    if ("COMPLETE".equals(status)) {
                        validateIdentification(object, id);
                    }
                    continue;
                }
                check(responseStatus.isString(), "Invalid Spotify status in task " + id + ".");
                switch (responseStatus.asString()) {
                    case "FOUND" -> {
                        validateIdentification(object, id);
                        object.remove("status");
                        updates.put(id, JSON.writeValueAsString(object));
                        increment(counts, "FOUND_STATUS_REMOVED");
                    }
                    case "NOT_FOUND", "AMBIGUOUS" -> {
                        updates.put(id, null);
                        increment(counts, responseStatus.asString() + "_TO_SQL_NULL");
                    }
                    default -> throw new IllegalStateException("Unknown Spotify status in task " + id + ".");
                }
            }
        }
        Set<String> clears = new LinkedHashSet<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(
                "SELECT id, artist_id, spotify_artist_id FROM artist_discovery_index WHERE spotify_artist_id IS NOT NULL")) {
            while (rows.next()) {
                Task task = latest.get(rows.getString("artist_id"));
                if (task != null && (updates.containsKey(task.id()) ? updates.get(task.id()) : task.result()) == null) {
                    clears.add(rows.getString("id"));
                }
            }
        }
        return new Plan(updates, clears, counts);
    }

    private static void increment(Map<String, Long> counts, String key) {
        counts.put(key, counts.get(key) + 1);
    }

    private static void validateIdentification(ObjectNode object, String id) {
        for (String field : List.of("artist", "matchedAlbum")) {
            JsonNode value = object.get(field);
            check(value instanceof ObjectNode, "Missing Spotify " + field + " in task " + id + ".");
            List<String> fields = field.equals("artist") ? List.of("id", "name", "url") : List.of("inputTitle", "title", "id", "url");
            for (String name : fields) {
                JsonNode item = value.get(name);
                check(item != null && item.isString() && !item.asString().isBlank(), "Invalid Spotify " + field + "." + name + " in task " + id + ".");
            }
        }
    }

    private static Outcome migrate(Connection connection, boolean apply) throws Exception {
        connection.setAutoCommit(false);
        try {
            Plan changes = plan(connection);
            Snapshot before = snapshot(connection, null);
            Snapshot expected = snapshot(connection, changes);
            if (!apply) {
                requireEqual(before, snapshot(connection, null), "Dry-run data changed during inspection.");
                connection.rollback();
                return new Outcome(changes, expected);
            }
            try (PreparedStatement update = connection.prepareStatement("UPDATE discovery_task SET result = ? WHERE id = ?")) {
                for (Map.Entry<String, String> entry : changes.taskUpdates().entrySet()) {
                    if (entry.getValue() == null) {
                        update.setNull(1, Types.VARCHAR);
                    } else {
                        update.setString(1, entry.getValue());
                    }
                    update.setString(2, entry.getKey());
                    check(update.executeUpdate() == 1, "Task disappeared during migration.");
                }
            }
            try (PreparedStatement update = connection.prepareStatement("UPDATE artist_discovery_index SET spotify_artist_id = NULL WHERE id = ? AND spotify_artist_id IS NOT NULL")) {
                for (String id : changes.indexClears()) {
                    update.setString(1, id);
                    check(update.executeUpdate() == 1, "Index changed during migration.");
                }
            }
            requireEqual(expected, snapshot(connection, null), "Unexpected data or schema changes; rolling back.");
            requireEmpty(plan(connection));
            connection.commit();
            return new Outcome(changes, expected);
        } catch (Exception exception) {
            connection.rollback();
            throw exception;
        }
    }

    private static Snapshot snapshot(Connection connection, Plan changes) throws Exception {
        Map<String, TableDigest> tables = new LinkedHashMap<>();
        List<String> names = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME")) {
            while (rows.next()) {
                names.add(rows.getString(1));
            }
        }
        for (String table : names) {
            List<String> hashes = new ArrayList<>();
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(
                    "SELECT * FROM PUBLIC.\"" + table.replace("\"", "\"\"") + "\"")) {
                ResultSetMetaData metadata = rows.getMetaData();
                while (rows.next()) {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    String id = table.equals("DISCOVERY_TASK") || table.equals("ARTIST_DISCOVERY_INDEX") ? rows.getString("ID") : null;
                    for (int column = 1; column <= metadata.getColumnCount(); column++) {
                        String name = metadata.getColumnName(column);
                        if (changes != null && table.equals("DISCOVERY_TASK") && name.equals("RESULT") && changes.taskUpdates().containsKey(id)) {
                            append(digest, changes.taskUpdates().get(id));
                        } else if (changes != null && table.equals("ARTIST_DISCOVERY_INDEX") && name.equals("SPOTIFY_ARTIST_ID") && changes.indexClears().contains(id)) {
                            append(digest, (String) null);
                        } else {
                            int type = metadata.getColumnType(column);
                            if (type == Types.BINARY || type == Types.VARBINARY || type == Types.LONGVARBINARY || type == Types.BLOB) {
                                append(digest, rows.getBytes(column));
                            } else {
                                append(digest, rows.getString(column));
                            }
                        }
                    }
                    hashes.add(HexFormat.of().formatHex(digest.digest()));
                }
            }
            hashes.sort(String::compareTo);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String hash : hashes) {
                append(digest, hash);
            }
            tables.put(table, new TableDigest(hashes.size(), HexFormat.of().formatHex(digest.digest())));
        }
        MessageDigest schema = MessageDigest.getInstance("SHA-256");
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SCRIPT NODATA NOPASSWORDS NOSETTINGS")) {
            while (rows.next()) {
                append(schema, rows.getString(1));
            }
        }
        return new Snapshot(tables, HexFormat.of().formatHex(schema.digest()));
    }

    private static void append(MessageDigest digest, String value) {
        append(digest, value != null ? value.getBytes(StandardCharsets.UTF_8) : null);
    }

    private static void append(MessageDigest digest, byte[] value) {
        int length = value != null ? value.length : -1;
        digest.update((byte) (length >>> 24));
        digest.update((byte) (length >>> 16));
        digest.update((byte) (length >>> 8));
        digest.update((byte) length);
        if (value != null) {
            digest.update(value);
        }
    }

    private static void requireEqual(Snapshot expected, Snapshot actual, String message) {
        check(expected.equals(actual), message);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void requireEmpty(Plan changes) {
        check(changes.taskUpdates().isEmpty() && changes.indexClears().isEmpty(), "Migration is not idempotent or empty-result Spotify IDs remain.");
    }

    private static void report(String mode, Plan changes) {
        System.out.println("Mode: " + mode);
        changes.counts().forEach((name, count) -> System.out.println(name + "=" + count));
        System.out.println("TASK_RESULTS_CHANGED=" + changes.taskUpdates().size());
        System.out.println("SPOTIFY_IDS_CLEARED=" + changes.indexClears().size());
    }

    private record Task(String id, String result) {}
    private record Plan(Map<String, String> taskUpdates, Set<String> indexClears, Map<String, Long> counts) {}
    private record TableDigest(long rows, String sha256) {}
    private record Snapshot(Map<String, TableDigest> tables, String schemaSha256) {}
    private record Outcome(Plan plan, Snapshot expected) {}
}
