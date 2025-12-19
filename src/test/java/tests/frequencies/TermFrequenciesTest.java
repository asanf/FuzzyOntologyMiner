package tests.frequencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import fuzzyOntologyMiner.frequencies.TermFrequencies;

public class TermFrequenciesTest {
  private TermFrequencies<String> termFrequencies;

  @BeforeEach
  public void setUp() {
    termFrequencies = new TermFrequencies<>();
  }

  @Test
  public void testAddOccurrence() {
    assertTrue(termFrequencies.addOccurrence("term1"));
    assertFalse(termFrequencies.getTerms().isEmpty());
  }

  @Test
  public void testAddOccurrencePair() {
    assertTrue(termFrequencies.addOccurrence("term1", "term2"));
  }

  @Test
  public void testAugmentWindows() {
    assertTrue(termFrequencies.augmentWindows());
    assertEquals(1, termFrequencies.getTotWindows());
  }

  @Test
  public void testComputeFrequencies() {
    termFrequencies.augmentWindows();
    termFrequencies.addOccurrence("term1");
    assertTrue(termFrequencies.computeFrequencies());
    assertFalse(termFrequencies.computeFrequencies());
  }

  @Test
  public void testGetFrequencyBeforeComputation() {
    termFrequencies.addOccurrence("term1");
    assertEquals(-2.0, termFrequencies.getFrequency("term1"), 0.0);
  }

  @Test
  public void testGetFrequency() {
    termFrequencies.augmentWindows();
    termFrequencies.augmentWindows();
    termFrequencies.addOccurrence("term1");
    termFrequencies.computeFrequencies();
    assertEquals(0.5, termFrequencies.getFrequency("term1"), 0.0);
  }

  @Test
  public void testGetFrequencyPair() {
    termFrequencies.augmentWindows();
    termFrequencies.addOccurrence("term1", "term2");
    termFrequencies.computeFrequencies();
    assertEquals(1.0, termFrequencies.getFrequency("term1", "term2"), 0.0);
  }

  @Test
  public void testFilterTerms() {
    termFrequencies.augmentWindows();
    termFrequencies.augmentWindows();
    termFrequencies.addOccurrence("term1");
    termFrequencies.addOccurrence("term2");
    termFrequencies.computeFrequencies();
    assertTrue(termFrequencies.filterTerms(0.3, 0.7));
  }

  @Test
  public void testIterator() {
    termFrequencies.addOccurrence("term1");
    termFrequencies.addOccurrence("term2");
    assertNotNull(termFrequencies.iterator());
  }

  @Test
  public void testGetTerms() {
    termFrequencies.addOccurrence("term1");
    termFrequencies.addOccurrence("term2");
    assertEquals(2, termFrequencies.getTerms().size());
  }

  @Test
  public void testConstructorWithSize() {
    TermFrequencies<String> tf = new TermFrequencies<>(10);
    assertTrue(tf.addOccurrence("term1"));
  }
}
