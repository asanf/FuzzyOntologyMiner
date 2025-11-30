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
import fuzzyOntologyMiner.domain.TermFrequencies;

class FuzzyOntologyMinerTest {
  
  private static Path INPUT;
  private static Path OUTPUT;
  private static IMiningParams miningParams;
  
  @BeforeAll
  static void setup() throws FileNotFoundException {
    INPUT = getResourcePath("TestData/Hackers");
    if (INPUT == null) throw new FileNotFoundException("Invalid input");
    OUTPUT = Paths.get("ontology.owl");
    
    miningParams = MiningParams.createDefault();
    
    /* Data input */
    miningParams.setInputDocumentsLocation(INPUT);
    
    /* Ontology output */
    miningParams.setOutputOntologyLocation(OUTPUT);
  }
  
  @Test
  void extractFrequenciesTest() {
    TermFrequencies<String> frequencies = FuzzyOntologyMiner.extractFrequencies(miningParams);
    assertNotEquals(0, frequencies.getTotWindows(), "At least a window must be found");
    assertNotEquals(0, frequencies.getTerms().size(), "At least a term must be found");
  }

  @Test
  void mineTest() {
    FuzzyOntologyMiner.mine(miningParams);
    
    assertTrue(OUTPUT.toFile().exists());
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
