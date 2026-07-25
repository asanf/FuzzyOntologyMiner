package tests.database;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fuzzyOntologyMiner.database.DbManager;

public class DbManagerTest {

    private DbManager dbManager;

    @BeforeEach
    void setUp() {
        dbManager = new DbManager(":memory:");
    }

    @AfterEach
    void tearDown() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Test
    void testConstructorInitializesDatabase() {
        // Test that tables are created and metadata is initialized
        List<String> terms = dbManager.getTerms();
        assertNotNull(terms);
        assertTrue(terms.isEmpty());
    }

    @Test
    void testNewObservation() {
        // Initially no observations
        assertEquals(0.0, dbManager.getFrequency("nonexistent"));

        dbManager.newObservation();
        // Still 0 frequency since no terms added
        assertEquals(0.0, dbManager.getFrequency("nonexistent"));
    }

    @Test
    void testAddSingleOccurrence() {
        String term = "testTerm";

        // Initially 0
        assertEquals(0.0, dbManager.getFrequency(term));

        dbManager.newObservation();
        dbManager.addOccurrence(term);
        dbManager.flush();

        assertEquals(1.0, dbManager.getFrequency(term));
    }

    @Test
    void testAddMultipleOccurrences() {
        String term = "testTerm";

        dbManager.newObservation();
        dbManager.newObservation();
        dbManager.addOccurrence(term);
        dbManager.addOccurrence(term);
        dbManager.flush();

        assertEquals(1.0, dbManager.getFrequency(term)); // 2/2 = 1.0
    }

    @Test
    void testAddCoOccurrence() {
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
    void testAddCoOccurrenceSameTerm() {
        String term = "sameTerm";

        dbManager.newObservation();
        dbManager.addOccurrence(term, term);
        dbManager.flush();

        // Should not add co-occurrence for same term
        assertEquals(0.0, dbManager.getFrequency(term, term));
    }

    @Test
    void testGetTerms() {
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
    void testBatchFlush() {
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
    void testFrequencyWithZeroObservations() {
        String term = "term";
        assertEquals(0.0, dbManager.getFrequency(term));
        assertEquals(0.0, dbManager.getFrequency(term, "other"));
    }

    @Test
    void testClose() {
        DbManager tempDb = new DbManager(":memory:");
        // Just ensure no exception on close
        assertDoesNotThrow(() -> tempDb.close());
    }

    @Test
    void testSetAndGetMembership() {
        String term = "term1";
        String concept = "concept1";
        double membership = 0.85;

        // Initially 0.0 (or 1.0 if terms are equal)
        assertEquals(0.0, dbManager.getMembership(term, concept));

        dbManager.setMembership(term, concept, membership);
        dbManager.flush();

        assertEquals(membership, dbManager.getMembership(term, concept));
    }

    @Test
    void testSetMembershipSameTerm() {
        String term = "same";
        // Should be ignored
        dbManager.setMembership(term, term, 0.5);
        dbManager.flush();
        // Always 1.0 for same term
        assertEquals(1.0, dbManager.getMembership(term, term));
    }

    @Test
    void testGetConcepts() {
        // Currently no way to set isConcept=true via API, so should be empty.
        // Also verifying the SQL query is syntactically correct (after fix).
        List<String> concepts = dbManager.getConcepts();
        assertNotNull(concepts);
        assertTrue(concepts.isEmpty());
    }

    @Test
    void testFilterTermsByFrequency() {
        // Total observations: need to set total_obs.
        // newObservation() increments total_obs by 1.
        // Let's create enough observations and term occurrences.

        // Scenario:
        // Total obs = 10
        // Term "low" : 1 occurrence (0.1 freq)
        // Term "mid" : 5 occurrences (0.5 freq)
        // Term "high" : 9 occurrences (0.9 freq)

        for (int i = 0; i < 10; i++) {
            dbManager.newObservation();
        }

        dbManager.addOccurrence("low");

        for (int i = 0; i < 5; i++)
            dbManager.addOccurrence("mid");

        for (int i = 0; i < 9; i++)
            dbManager.addOccurrence("high");

        // Add co-occurrence to verify cleanup
        dbManager.addOccurrence("low", "mid");

        dbManager.flush();

        // Verify before filter
        assertEquals(0.1, dbManager.getFrequency("low"));
        assertEquals(0.5, dbManager.getFrequency("mid"));
        assertEquals(0.9, dbManager.getFrequency("high"));
        assertEquals(0.1, dbManager.getFrequency("low", "mid"));

        // Filter: keep [0.4, 0.6] -> should keep "mid" only
        dbManager.filterTermsByFrequency(0.4, 0.6);

        // Verify "low" and "high" are gone (0.0 frequency)
        assertEquals(0.0, dbManager.getFrequency("low"), "Low freq term should be removed");
        assertEquals(0.0, dbManager.getFrequency("high"), "High freq term should be removed");

        // Verify "mid" is still there
        assertEquals(0.5, dbManager.getFrequency("mid"), "Mid freq term should remain");

        // Verify co-occurrence involving removed term is gone
        assertEquals(0.0, dbManager.getFrequency("low", "mid"), "Co-occurrence should be cleaned");
    }

    @Test
    void testExceptionHandlingAfterClose() {
        // Close the database to trigger SQLExceptions internally
        dbManager.close();

        // Methods should gracefully handle errors (log and return default/do nothing)
        assertDoesNotThrow(() -> dbManager.newObservation());
        assertDoesNotThrow(() -> dbManager.addOccurrence("term"));
        assertDoesNotThrow(() -> dbManager.addOccurrence("t1", "t2"));
        assertDoesNotThrow(() -> dbManager.setMembership("t", "c", 0.5));

        assertEquals(0.0, dbManager.getFrequency("term"));
        assertEquals(0.0, dbManager.getFrequency("t1", "t2"));
        assertEquals(0.0, dbManager.getMembership("t", "c"));

        List<String> terms = dbManager.getTerms();
        assertNotNull(terms);
        assertTrue(terms.isEmpty());
    }

    @Test
    void testNormalizeMemberships() {
        // Linear scaling: (x - min) / (max - min)
        // Values: 0.2, 0.5, 0.8
        // Min: 0.2, Max: 0.8, Range: 0.6
        // 0.2 -> 0.0
        // 0.5 -> 0.5
        // 0.8 -> 1.0

        dbManager.setMembership("t1", "c1", 0.2);
        dbManager.setMembership("t2", "c2", 0.5);
        dbManager.setMembership("t3", "c3", 0.8);
        dbManager.flush();

        dbManager.normalizeMemberships();

        assertEquals(0.0, dbManager.getMembership("t1", "c1"), 0.0001);
        assertEquals(0.5, dbManager.getMembership("t2", "c2"), 0.0001);
        assertEquals(1.0, dbManager.getMembership("t3", "c3"), 0.0001);
    }

    @Test
    void testNormalizeMembershipsSingleValue() {
        // If all values are same (min == max), database shouldn't be updated (0
        // division avoidance)
        dbManager.setMembership("t1", "c1", 0.5);
        dbManager.setMembership("t2", "c2", 0.5);
        dbManager.flush();

        dbManager.normalizeMemberships();

        assertEquals(0.5, dbManager.getMembership("t1", "c1"));
        assertEquals(0.5, dbManager.getMembership("t2", "c2"));
    }
}