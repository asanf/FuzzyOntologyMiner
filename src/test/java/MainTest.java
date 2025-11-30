import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import fuzzyOntologyMiner.FuzzyOntologyMiner;
import fuzzyOntologyMiner.IMiningParams;
import fuzzyOntologyMiner.MiningParams;
import fuzzyOntologyMiner.correlationFunctions.BalancedMutualInformation;
import fuzzyOntologyMiner.postag.PosTag;

class MainTest {
  
  private static Path INPUT;
  private static Path OUTPUT;
  
  @BeforeAll
  static void setup() throws FileNotFoundException {
    INPUT = getResourcePath("TestData/Hackers");
    if (INPUT == null) throw new FileNotFoundException("Invalid input");
    OUTPUT = Paths.get("ontology.owl");
  }

  @Test
  void test() {
    IMiningParams miningParams = new MiningParams();
    
    miningParams.setInputDocumentsLocation(INPUT);
    
    /* Search patterns */
    miningParams.addMiningPattern(PosTag.NOUN);
    miningParams.addMiningPattern(PosTag.NOUN, PosTag.NOUN_PLURAL);
    miningParams.addMiningPattern(PosTag.NOUN_PROPER, PosTag.NOUN_PLURAL);
    miningParams.addMiningPattern(PosTag.NOUN_PROPER, PosTag.NOUN_PROPER);
    miningParams.addMiningPattern(PosTag.ADJECTIVE, PosTag.NOUN);
    
    /* Term frequencies extraction parameters */
    miningParams.setTaggerModel("models/left3words-wsj-0-18.tagger");
    miningParams.setWindowSize(10);
    miningParams.setTermFrequencyThresholds(0.03, 0.8);
    
    /* Concept extraction parameters */
    miningParams.setConceptFilteringThreshold(0.05);
    miningParams.setCorrelationFunction(new BalancedMutualInformation(0.67));
    
    /* Taxonomy parameters */
    miningParams.setConceptRelationFilterThreshold(0.022);
    
    /* Ontology output */
    miningParams.setOutputOntologyLocation(OUTPUT);
    
    try {
      FuzzyOntologyMiner.mine(miningParams);
    } catch (ClassNotFoundException | OWLOntologyCreationException | OWLOntologyStorageException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private static Path getResourcePath(String resource) {
    URL resourceURL = MainTest.class.getClassLoader().getResource(resource);
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
