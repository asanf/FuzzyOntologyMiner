package fuzzyOntologyMiner.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DbManager implements AutoCloseable {
    
    // SQL Queries
    private static final String SQL_INIT_METADATA = "CREATE TABLE IF NOT EXISTS metadata (id INTEGER PRIMARY KEY, total_obs BIGINT)";
    private static final String SQL_INIT_METADATA_VAL = "INSERT OR IGNORE INTO metadata VALUES (1, 0)";
    private static final String SQL_INIT_TERMS = "CREATE TABLE IF NOT EXISTS terms (term VARCHAR PRIMARY KEY, count BIGINT)";
    private static final String SQL_INIT_CO_OCC = "CREATE TABLE IF NOT EXISTS co_occurrences (term_a VARCHAR, term_b VARCHAR, count BIGINT, PRIMARY KEY (term_a, term_b))";
    
    private static final String SQL_UPDATE_OBS = "UPDATE metadata SET total_obs = total_obs + 1 WHERE id = 1";
    private static final String SQL_UPSERT_TERM = "INSERT INTO terms (term, count) VALUES (?, 1) ON CONFLICT (term) DO UPDATE SET count = count + 1";
    private static final String SQL_UPSERT_CO_OCC = "INSERT INTO co_occurrences (term_a, term_b, count) VALUES (?, ?, 1) ON CONFLICT (term_a, term_b) DO UPDATE SET count = count + 1";
    
    private static final String SQL_SELECT_TOTAL = "SELECT total_obs FROM metadata WHERE id = 1";
    private static final String SQL_SELECT_TERM = "SELECT count FROM terms WHERE term = ?";
    private static final String SQL_SELECT_CO_OCC = "SELECT count FROM co_occurrences WHERE term_a = ? AND term_b = ?";
    private static final String SQL_SELECT_ALL_TERMS = "SELECT term FROM terms";

    private static final String SQL_DELETE_OUT_OF_RANGE = 
    "DELETE FROM terms WHERE (CAST(count AS DOUBLE) / (SELECT total_obs FROM metadata WHERE id = 1)) < ? " +
    "OR (CAST(count AS DOUBLE) / (SELECT total_obs FROM metadata WHERE id = 1)) > ?";

    private static final String SQL_CLEAN_CO_OCC = 
    "DELETE FROM co_occurrences WHERE term_a NOT IN (SELECT term FROM terms) " +
    "OR term_b NOT IN (SELECT term FROM terms)";

    private final Connection conn;
    private final PreparedStatement psUpsertTerm;
    private final PreparedStatement psUpsertCoOcc;
    
    private int batchCounter = 0;
    private static final int BATCH_SIZE_THRESHOLD = 1000; 

    public DbManager(String dbPath) throws SQLException {
        try {
            Class.forName("org.duckdb.DuckDBDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("DuckDB JDBC driver not found", e);
        }
        Path path = Paths.get(dbPath);
        if (Files.exists(path)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        this.conn = DriverManager.getConnection("jdbc:duckdb:" + dbPath);
        this.conn.setAutoCommit(false);
        
        configureStorage();
        initDatabase();
        
        this.psUpsertTerm = conn.prepareStatement(SQL_UPSERT_TERM);
        this.psUpsertCoOcc = conn.prepareStatement(SQL_UPSERT_CO_OCC);
    }

    private void configureStorage() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA memory_limit='6GB'");
            st.execute("PRAGMA threads=4");
        }
    }

    private void initDatabase() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(SQL_INIT_METADATA);
            st.execute(SQL_INIT_METADATA_VAL);
            st.execute(SQL_INIT_TERMS);
            st.execute(SQL_INIT_CO_OCC);
            conn.commit();
        }
    }

    public void newObservation() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(SQL_UPDATE_OBS);
        }
        checkBatch();
    }

    public void addOccurrence(String term) throws SQLException {
        psUpsertTerm.setString(1, term);
        psUpsertTerm.addBatch();
        batchCounter++;
        checkBatch();
    }

    public void addOccurrence(String first, String second) throws SQLException {
        if (first.equals(second)) return;
        String t1 = first.compareTo(second) < 0 ? first : second;
        String t2 = first.compareTo(second) < 0 ? second : first;

        psUpsertCoOcc.setString(1, t1);
        psUpsertCoOcc.setString(2, t2);
        psUpsertCoOcc.addBatch();
        batchCounter++;
        checkBatch();
    }

    private void checkBatch() throws SQLException {
        if (batchCounter >= BATCH_SIZE_THRESHOLD) {
            flush();
        }
    }

    public void flush() throws SQLException {
        psUpsertTerm.executeBatch();
        psUpsertCoOcc.executeBatch();
        conn.commit();
        batchCounter = 0;
    }

    public double getFrequency(String term) throws SQLException {
        long total = getTotalObservations();
        if (total == 0) return 0.0;

        try (PreparedStatement pst = conn.prepareStatement(SQL_SELECT_TERM)) {
            pst.setString(1, term);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? (double) rs.getLong(1) / total : 0.0;
            }
        }
    }

    public double getFrequency(String first, String second) throws SQLException {
        long total = getTotalObservations();
        if (total == 0) return 0.0;

        String t1 = first.compareTo(second) < 0 ? first : second;
        String t2 = first.compareTo(second) < 0 ? second : first;

        try (PreparedStatement pst = conn.prepareStatement(SQL_SELECT_CO_OCC)) {
            pst.setString(1, t1);
            pst.setString(2, t2);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() ? (double) rs.getLong(1) / total : 0.0;
            }
        }
    }

    public List<String> getTerms() throws SQLException {
        List<String> terms = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(SQL_SELECT_ALL_TERMS)) {
            while (rs.next()) {
                terms.add(rs.getString(1));
            }
        }
        return terms;
    }

    public void filter(double lower, double upper) throws SQLException {
        if (getTotalObservations() == 0) return;

    try {
        // 1. Identifica e rimuovi i termini fuori range dalla tabella 'terms'
        try (PreparedStatement pst = conn.prepareStatement(SQL_DELETE_OUT_OF_RANGE)) {
            pst.setDouble(1, lower);
            pst.setDouble(2, upper);
            pst.executeUpdate();
        }

        // 2. Rimuovi manualmente le co-occorrenze orfane 
        // (quelle dove term_a o term_b non esistono più in 'terms')
        try (Statement st = conn.createStatement()) {
            st.execute(SQL_CLEAN_CO_OCC);
        }

        // 3. Conferma le modifiche
        conn.commit();
        
    } catch (SQLException e) {
        conn.rollback();
        throw e;
    }
    }

    private long getTotalObservations() throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(SQL_SELECT_TOTAL)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    @Override
    public void close() throws SQLException {
        flush(); 
        if (psUpsertTerm != null) psUpsertTerm.close();
        if (psUpsertCoOcc != null) psUpsertCoOcc.close();
        if (conn != null) conn.close();
    }
}
