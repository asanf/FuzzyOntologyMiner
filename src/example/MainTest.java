package example;

import java.io.IOException;
import java.nio.file.Paths;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import fuzzyOntologyMiner.FuzzyOntologyMiner;
import fuzzyOntologyMiner.IMiningParams;
import fuzzyOntologyMiner.MiningParams;
import fuzzyOntologyMiner.correlationFunctions.BalancedMutualInformation;
import fuzzyOntologyMiner.postag.PosTag;

public class MainTest {
  public static void main(String[] args) {
    String[] inputPaths = new String[3];
    inputPaths[0] = "Reuters";
    inputPaths[1] = "Sicurezza";
    inputPaths[2] = "Hackers";
    
    IMiningParams miningParams = new MiningParams();
    
    miningParams.setInputDocumentsLocation(Paths.get(inputPaths[2]));
    
    /* Search patterns */
    miningParams.addMiningPattern(PosTag.NOUN);
    miningParams.addMiningPattern(PosTag.NOUN, PosTag.NOUN_PLURAL);
    miningParams.addMiningPattern(PosTag.NOUN_PROPER, PosTag.NOUN_PLURAL);
    miningParams.addMiningPattern(PosTag.NOUN_PROPER, PosTag.NOUN_PROPER);
    miningParams.addMiningPattern(PosTag.ADJECTIVE, PosTag.NOUN);
    
    /* Term frequencies extraction parameters */
    miningParams.setTaggerModel("left3words-wsj-0-18.tagger");
    miningParams.setWindowSize((byte) 7);
    miningParams.setTermFrequencyThresholds(0.03, 0.55);
    
    /* Concept extraction parameters */
    miningParams.setConceptFilteringThreshold(0.02);
    miningParams.setCorrelationFunction(new BalancedMutualInformation(0.55));
    
    /* Taxonomy parameters */
    miningParams.setConceptRelationFilterThreshold(0.022);
    
    /* Ontology output */
    miningParams.setOutputOntologyLocation(Paths.get("ontology"));
    
    try {
      FuzzyOntologyMiner.mine(miningParams);
    } catch (ClassNotFoundException | OWLOntologyCreationException | OWLOntologyStorageException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
