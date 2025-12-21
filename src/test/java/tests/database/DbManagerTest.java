package tests.database;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fuzzyOntologyMiner.database.DbManager;

public class DbManagerTest {

    private DbManager dbManager;

    @BeforeEach
    void setUp() throws SQLException {
        dbManager = new DbManager(":memory:");
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    void testConstructorInitializesDatabase() throws SQLException {
        // Test that tables are created and metadata is initialized
        List<String> terms = dbManager.getTerms();
        assertNotNull(terms);
        assertTrue(terms.isEmpty());
    }

    @Test
    void testNewObservation() throws SQLException {
        // Initially no observations
        assertEquals(0.0, dbManager.getFrequency("nonexistent"));

        dbManager.newObservation();
        // Still 0 frequency since no terms added
        assertEquals(0.0, dbManager.getFrequency("nonexistent"));
    }

    @Test
    void testAddSingleOccurrence() throws SQLException {
        String term = "testTerm";

        // Initially 0
        assertEquals(0.0, dbManager.getFrequency(term));

        dbManager.newObservation();
        dbManager.addOccurrence(term);
        dbManager.flush();

        assertEquals(1.0, dbManager.getFrequency(term));
    }

    @Test
    void testAddMultipleOccurrences() throws SQLException {
        String term = "testTerm";

        dbManager.newObservation();
        dbManager.newObservation();
        dbManager.addOccurrence(term);
        dbManager.addOccurrence(term);
        dbManager.flush();

        assertEquals(1.0, dbManager.getFrequency(term)); // 2/2 = 1.0
    }

    @Test
    void testAddCoOccurrence() throws SQLException {
        String term1 = "term1";
        String term2 = "term2";

        // Initially 0
        assertEquals(0.0, dbManager.getFrequency(term1, term2));

        dbManager.newObservation();
        dbManager.addOccurrence(term1, term2);
        dbManager.flush();

        assertEquals(1.0, dbManager.getFrequency(term1, term2));
        // Order shouldn't matter
        assertEquals(1.0, dbManager.getFrequency(term2, term1));
    }

    @Test
    void testAddCoOccurrenceSameTerm() throws SQLException {
        String term = "sameTerm";

        dbManager.newObservation();
        dbManager.addOccurrence(term, term);
        dbManager.flush();

        // Should not add co-occurrence for same term
        assertEquals(0.0, dbManager.getFrequency(term, term));
    }

    @Test
    void testGetTerms() throws SQLException {
        String term1 = "term1";
        String term2 = "term2";

        List<String> terms = dbManager.getTerms();
        assertTrue(terms.isEmpty());

        dbManager.newObservation();
        dbManager.addOccurrence(term1);
        dbManager.addOccurrence(term2);
        dbManager.flush();

        terms = dbManager.getTerms();
        assertEquals(2, terms.size());
        assertTrue(terms.contains(term1));
        assertTrue(terms.contains(term2));
    }

    @Test
    void testBatchFlush() throws SQLException {
        String term = "batchTerm";

        // Add many occurrences to trigger batch
        dbManager.newObservation();
        for (int i = 0; i < 1001; i++) {
            dbManager.addOccurrence(term);
        }
        dbManager.flush(); // Ensure all batches are committed
        assertEquals(1001.0, dbManager.getFrequency(term));
    }

    @Test
    void testFrequencyWithZeroObservations() throws SQLException {
        String term = "term";
        assertEquals(0.0, dbManager.getFrequency(term));
        assertEquals(0.0, dbManager.getFrequency(term, "other"));
    }

    @Test
    void testClose() throws SQLException {
        DbManager tempDb = new DbManager(":memory:");
        // Just ensure no exception on close
        assertDoesNotThrow(() -> tempDb.close());
    }
}