package fuzzyOntologyMiner.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbManager implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(DbManager.class.getName());

    // SQL Queries
    private static final String SQL_INIT_METADATA = "CREATE TABLE IF NOT EXISTS metadata (id INTEGER PRIMARY KEY, total_obs BIGINT)";
    private static final String SQL_INIT_METADATA_VAL = "INSERT OR IGNORE INTO metadata VALUES (1, 0)";
    private static final String SQL_INIT_TERMS = "CREATE TABLE IF NOT EXISTS terms (term VARCHAR PRIMARY KEY, occurrences BIGINT, isConcept BOOLEAN DEFAULT TRUE)";
    private static final String SQL_INIT_CO_OCC = "CREATE TABLE IF NOT EXISTS co_occurrences (term_a VARCHAR, term_b VARCHAR, occurrences BIGINT, PRIMARY KEY (term_a, term_b))";
    private static final String SQL_INIT_MEMBERSHIPS = "CREATE TABLE IF NOT EXISTS memberships (term VARCHAR, concept VARCHAR, membership DOUBLE, PRIMARY KEY (term, concept))";

    private static final String SQL_UPDATE_OBS = "UPDATE metadata SET total_obs = total_obs + 1 WHERE id = 1";
    private static final String SQL_UPSERT_TERM = "INSERT INTO terms (term, occurrences) VALUES (?, 1) ON CONFLICT (term) DO UPDATE SET occurrences = occurrences + 1";
    private static final String SQL_UPSERT_CO_OCC = "INSERT INTO co_occurrences (term_a, term_b, occurrences) VALUES (?, ?, 1) ON CONFLICT (term_a, term_b) DO UPDATE SET occurrences = occurrences + 1";
    private static final String SQL_INSERT_MEMBERSHIP = "INSERT OR IGNORE INTO memberships (term, concept, membership) VALUES (?, ?, ?)";
    private static final String SQL_DELETE_CONCEPT = "UPDATE INTO terms SET isConcept = FALSE WHERE term = ?";

    private static final String SQL_SELECT_TOTAL = "SELECT total_obs FROM metadata WHERE id = 1";
    private static final String SQL_SELECT_TERM = "SELECT occurrences FROM terms WHERE term = ?";
    private static final String SQL_SELECT_CO_OCC = "SELECT occurrences FROM co_occurrences WHERE term_a = ? AND term_b = ?";
    private static final String SQL_SELECT_MEMBERSHIP = "SELECT membership FROM memberships WHERE term = ? AND concept = ?";
    private static final String SQL_SELECT_ALL_MEMBERSHIPS = "SELECT term, concept, membership FROM memberships";
    private static final String SQL_SELECT_MIN_MAX_MEMBERSHIP = "SELECT MIN(membership), MAX(membership) FROM memberships";
    private static final String SQL_NORMALIZE_MEMBERSHIP = "UPDATE memberships SET membership = (membership - ?) / (? - ?)";
    private static final String SQL_SELECT_ALL_TERMS = "SELECT term FROM terms";
    private static final String SQL_SELECT_ALL_CONCEPTS = "SELECT term FROM terms WHERE isConcept = TRUE";

    private static final String SQL_DELETE_OUT_OF_RANGE = "DELETE FROM terms WHERE (CAST(count AS DOUBLE) / (SELECT total_obs FROM metadata WHERE id = 1)) < ? "
            +
            "OR (CAST(count AS DOUBLE) / (SELECT total_obs FROM metadata WHERE id = 1)) > ?";

    private static final String SQL_CLEAN_CO_OCC = "DELETE FROM co_occurrences WHERE term_a NOT IN (SELECT term FROM terms) "
            +
            "OR term_b NOT IN (SELECT term FROM terms)";

    private final Connection conn;
    private final PreparedStatement psUpsertTerm;
    private final PreparedStatement psUpsertCoOcc;
    private final PreparedStatement psInsertMembership;

    private int batchCounter = 0;
    private static final int BATCH_SIZE_THRESHOLD = 1000;

    public DbManager(String dbPath) {
        try {
            Class.forName("org.duckdb.DuckDBDriver");
            Path path = Paths.get(dbPath);
            if (Files.exists(path)) {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to delete existing database file", e);
                }
            }
            this.conn = DriverManager.getConnection("jdbc:duckdb:" + dbPath);
            this.conn.setAutoCommit(false);

            configureStorage();
            initDatabase();

            this.psUpsertTerm = conn.prepareStatement(SQL_UPSERT_TERM);
            this.psUpsertCoOcc = conn.prepareStatement(SQL_UPSERT_CO_OCC);
            this.psInsertMembership = conn.prepareStatement(SQL_INSERT_MEMBERSHIP);
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void configureStorage() {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA memory_limit='6GB'");
            st.execute("PRAGMA threads=4");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to configure storage", e);
        }
    }

    private void initDatabase() {
        try (Statement st = conn.createStatement()) {
            st.execute(SQL_INIT_METADATA);
            st.execute(SQL_INIT_METADATA_VAL);
            st.execute(SQL_INIT_TERMS);
            st.execute(SQL_INIT_CO_OCC);
            st.execute(SQL_INIT_MEMBERSHIPS);
            conn.commit();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database schema", e);
        }
    }

    public void newObservation() {
        try (Statement st = conn.createStatement()) {
            st.execute(SQL_UPDATE_OBS);
            checkBatch();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update observation count", e);
        }
    }

    public void addOccurrence(String term) {
        try {
            psUpsertTerm.setString(1, term);
            psUpsertTerm.addBatch();
            batchCounter++;
            checkBatch();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to add occurrence: " + term, e);
        }
    }

    public void addOccurrence(String first, String second) {
        if (first.equals(second))
            return;
        try {
            String t1 = first.compareTo(second) < 0 ? first : second;
            String t2 = first.compareTo(second) < 0 ? second : first;

            psUpsertCoOcc.setString(1, t1);
            psUpsertCoOcc.setString(2, t2);
            psUpsertCoOcc.addBatch();
            batchCounter++;
            checkBatch();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to add co-occurrence: " + first + ", " + second, e);
        }
    }

    public void setMembership(String first, String second, double membership) {
        if (first.equals(second))
            return;

        try {
            String t1 = first.compareTo(second) < 0 ? first : second;
            String t2 = first.compareTo(second) < 0 ? second : first;

            psInsertMembership.setString(1, t1);
            psInsertMembership.setString(2, t2);
            psInsertMembership.setDouble(3, membership);
            psInsertMembership.addBatch();
            batchCounter++;
            checkBatch();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to set membership: " + first + ", " + second, e);
        }
    }

    private void checkBatch() {
        if (batchCounter >= BATCH_SIZE_THRESHOLD) {
            flush();
        }
    }

    public void flush() {
        try {
            psUpsertTerm.executeBatch();
            psUpsertCoOcc.executeBatch();
            conn.commit();
            batchCounter = 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to flush batch", e);
        }
    }

    public double getFrequency(String term) {
        long total = getTotalObservations();
        if (total == 0)
            return 0.0;

        try (PreparedStatement pst = conn.prepareStatement(SQL_SELECT_TERM)) {
            pst.setString(1, term);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? (double) rs.getLong(1) / total : 0.0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get frequency for term: " + term, e);
            return 0.0;
        }
    }

    public double getFrequency(String first, String second) {
        long total = getTotalObservations();
        if (total == 0)
            return 0.0;

        String t1 = first.compareTo(second) < 0 ? first : second;
        String t2 = first.compareTo(second) < 0 ? second : first;

        try (PreparedStatement pst = conn.prepareStatement(SQL_SELECT_CO_OCC)) {
            pst.setString(1, t1);
            pst.setString(2, t2);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? (double) rs.getLong(1) / total : 0.0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get frequency for: " + first + ", " + second, e);
            return 0.0;
        }
    }

    public double getMembership(String first, String second) {
        if (first.equals(second))
            return 1.0;

        String t1 = first.compareTo(second) < 0 ? first : second;
        String t2 = first.compareTo(second) < 0 ? second : first;

        try (PreparedStatement pst = conn.prepareStatement(SQL_SELECT_MEMBERSHIP)) {
            pst.setString(1, t1);
            pst.setString(2, t2);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? (double) rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get membership for: " + first + ", " + second, e);
            return 0.0;
        }
    }

    public Map<String, Map<String, Double>> getMemberships() {
        Map<String, Map<String, Double>> memberships = new HashMap<>();

        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(SQL_SELECT_ALL_MEMBERSHIPS)) {
            while (rs.next()) {
                String term = rs.getString(1);
                String concept = rs.getString(2);
                double membership = rs.getDouble(3);
                memberships.computeIfAbsent(term, k -> new HashMap<>()).put(concept, membership);
                memberships.computeIfAbsent(concept, k -> new HashMap<>()).put(term, membership);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get memberships", e);
        }

        return memberships;
    }

    public List<String> getTerms() {
        List<String> terms = new ArrayList<>();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(SQL_SELECT_ALL_TERMS)) {
            while (rs.next()) {
                terms.add(rs.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get terms", e);
        }
        return terms;
    }

    public List<String> getConcepts() {
        List<String> concepts = new ArrayList<>();

        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(SQL_SELECT_ALL_CONCEPTS)) {
            while (rs.next()) {
                concepts.add(rs.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get concepts", e);
        }

        return concepts;
    }

    public void deleteConcept(String concept) {
        try (PreparedStatement pst = conn.prepareStatement(SQL_DELETE_CONCEPT)) {
            pst.setString(1, concept);
            pst.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete concept: " + concept, e);
        }
    }

    public void filterTermsByFrequency(double lower, double upper) {
        if (getTotalObservations() == 0)
            return;

        try {
        	/* Remove terms out of the [lower, upper] range. */
            try (PreparedStatement pst = conn.prepareStatement(SQL_DELETE_OUT_OF_RANGE)) {
                pst.setDouble(1, lower);
                pst.setDouble(2, upper);
                pst.executeUpdate();
            }

            /* 2. Manually remove orphan co-occurences
             * (those where term_a or term_b are not in the main terms table) */
            try (Statement st = conn.createStatement()) {
                st.execute(SQL_CLEAN_CO_OCC);
            }

            conn.commit();

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to rollback", ex);
            }
            LOGGER.log(Level.SEVERE, "Failed to filter terms by frequency", e);
        }
    }

    public List<Object[]> getSpecificities(double lambda) {
        List<Object[]> results = new ArrayList<>();
        String sql = 
            "WITH sym_mem AS (" +
            "  SELECT term as t1, concept as t2, membership FROM memberships " +
            "  UNION ALL " +
            "  SELECT concept as t1, term as t2, membership FROM memberships " +
            "  UNION ALL " +
            "  SELECT term as t1, term as t2, 1.0 as membership FROM terms" +
            "), " +
            "denominators AS (" +
            "  SELECT t1 as subconcept, SUM(membership) as denom " +
            "  FROM sym_mem " +
            "  WHERE t1 IN (SELECT term FROM terms WHERE isConcept = TRUE) " +
            "  GROUP BY t1 " +
            "), " +
            "numerators AS (" +
            "  SELECT m1.t1 as subconcept, m2.t1 as concept, SUM(LEAST(m1.membership, m2.membership)) as num " +
            "  FROM sym_mem m1 " +
            "  JOIN sym_mem m2 ON m1.t2 = m2.t2 " +
            "  WHERE m1.t1 IN (SELECT term FROM terms WHERE isConcept = TRUE) " +
            "    AND m2.t1 IN (SELECT term FROM terms WHERE isConcept = TRUE) " +
            "  GROUP BY m1.t1, m2.t1 " +
            ") " +
            "SELECT n.subconcept, n.concept, n.num / d.denom as specificity " +
            "FROM numerators n " +
            "JOIN denominators d ON n.subconcept = d.subconcept " +
            "WHERE (n.num / d.denom) >= ?";

        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setDouble(1, lambda);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    results.add(new Object[]{rs.getString(1), rs.getString(2), rs.getDouble(3)});
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to compute specificities", e);
        }

        return results;
    }

    private long getTotalObservations() {
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(SQL_SELECT_TOTAL)) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get total observations", e);
            return 0;
        }
    }

    /** Normalizes the membership value in membership table in the [0, 1] range */
    public void normalizeMemberships() {
        double min = 0;
        double max = 0;

        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(SQL_SELECT_MIN_MAX_MEMBERSHIP)) {
            if (rs.next()) {
                min = rs.getDouble(1);
                max = rs.getDouble(2);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to get min/max membership", e);
            return;
        }

        if (Double.compare(min, max) == 0) {
            return;
        }

        try (PreparedStatement pst = conn.prepareStatement(SQL_NORMALIZE_MEMBERSHIP)) {
            pst.setDouble(1, min);
            pst.setDouble(2, max);
            pst.setDouble(3, min);
            pst.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to rollback", ex);
            }
            LOGGER.log(Level.SEVERE, "Failed to normalize memberships", e);
        }
    }

    @Override
    public void close() {
        flush();
        try {
            if (psUpsertTerm != null)
                psUpsertTerm.close();
            if (psUpsertCoOcc != null)
                psUpsertCoOcc.close();
            if (conn != null)
                conn.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to close database resources", e);
        }
    }
}
