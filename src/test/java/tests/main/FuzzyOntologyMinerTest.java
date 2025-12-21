package tests.main;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import fuzzyOntologyMiner.FuzzyOntologyMiner;
import fuzzyOntologyMiner.IMiningParams;
import fuzzyOntologyMiner.MiningParams;
import fuzzyOntologyMiner.correlationFunctions.BalancedMutualInformation;
import fuzzyOntologyMiner.postag.PosTag;

class FuzzyOntologyMinerTest {
  private static IMiningParams miningParams;
  
  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of("test_data/hackers", "hackers_ontology.owl"),
        Arguments.of("test_data/cyber", "crypto_ontology.owl")
        /* TODO data set too big, causes OutOfMemoryException: Java Heap in TriangularMatrix class
        Arguments.of("test_data/reuters", "reuters_ontology.owl")
        */
    );
  }

  @BeforeAll
  public static void setup() {
    miningParams = new MiningParams();
    /* Search patterns */
    miningParams.addMiningPattern(PosTag.NOUN);
    
    /* Term frequencies extraction parameters */
    miningParams.setTaggerModel("models/left3words-wsj-0-18.tagger");
    miningParams.setWindowSize(7);
    miningParams.setTermFrequencyThresholds(0.03, 0.55);
    
    /* Concept extraction parameters */
    miningParams.setConceptFilteringThreshold(0.02);
    miningParams.setCorrelationFunction(new BalancedMutualInformation(0.55));
    
    /* Taxonomy parameters */
    miningParams.setConceptRelationFilterThreshold(0.02);
  }
  
  @ParameterizedTest
  @MethodSource("data")
  public void testMiner(String inFile, String outFile) {
    Path output = Paths.get(outFile);
    miningParams.setInputDocumentsLocation(getResourcePath(inFile));
    miningParams.setOutputOntologyLocation(output);
    FuzzyOntologyMiner.mine(miningParams);
    
    assertTrue(output.toFile().exists());
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
}
