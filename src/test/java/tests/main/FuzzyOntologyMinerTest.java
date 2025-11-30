package tests.main;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import fuzzyOntologyMiner.FuzzyOntologyMiner;
import fuzzyOntologyMiner.IMiningParams;
import fuzzyOntologyMiner.MiningParams;
import fuzzyOntologyMiner.correlationFunctions.BalancedMutualInformation;
import fuzzyOntologyMiner.domain.TermFrequencies;
import fuzzyOntologyMiner.postag.PosTag;

class FuzzyOntologyMinerTest {
  private static String outputDir;
  private static IMiningParams miningParams;
  
  @BeforeAll
  static void setup() throws FileNotFoundException {
    outputDir = System.getProperty("testOutputDir");
    if (outputDir == null) 
      throw new IllegalStateException("System property 'testOutputDir' not set by Gradle");
    
    miningParams = new MiningParams();
    
    /* Search patterns */
    miningParams.addMiningPattern(PosTag.NOUN);
    
    /* Term frequencies extraction parameters */
    miningParams.setTaggerModel("models/left3words-wsj-0-18.tagger");
    miningParams.setWindowSize(7);
    miningParams.setTermFrequencyThresholds(0.03, 0.65);
    
    /* Concept extraction parameters */
    miningParams.setConceptFilteringThreshold(0.2);
    miningParams.setCorrelationFunction(new BalancedMutualInformation(0.55));
    
    /* Taxonomy parameters */
    miningParams.setConceptRelationFilterThreshold(0.2);
  }
  
  @Test
  void extractFrequenciesTest() {
    TermFrequencies<String> frequencies = FuzzyOntologyMiner.extractFrequencies(miningParams);
    assertNotEquals(0, frequencies.getTotWindows(), "At least a window must be found");
    assertNotEquals(0, frequencies.getTerms().size(), "At least a term must be found");
  }

  @Test
  void hackerTest() {
    generalMineTest("test_data/hackers", "hackers_ontology.owl");
  }
  
  @Test
  void cyberSecurityTest() {
    generalMineTest("test_data/cyber", "crypto_ontology.owl");
  }
  
  @Test
  void reutersTest() {
    generalMineTest("test_data/reuters", "reuters_ontology.owl");
  }
  
  private static Path getResourcePath(String resource) {
    URL resourceURL = FuzzyOntologyMinerTest.class.getClassLoader().getResource(resource);
    if (resourceURL == null) return null;
    
    URI resourceURI;
    try {
      resourceURI = resourceURL.toURI();
      if (!resourceURI.getScheme().equals("file")) return null;
    } catch (URISyntaxException e) {
      return null;
    }
    
    return Paths.get(resourceURI);
  }
  
  private static void generalMineTest(String input, String output) {
    Path OUTPUT = Paths.get(outputDir, output);
    miningParams.setInputDocumentsLocation(getResourcePath(input));
    miningParams.setOutputOntologyLocation(OUTPUT);
    FuzzyOntologyMiner.mine(miningParams);
    
    assertTrue(OUTPUT.toFile().exists());
  }
}
