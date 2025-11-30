package fuzzyOntologyMiner.main;

import java.io.IOException;
import java.nio.file.Paths;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import fuzzyOntologyMiner.FuzzyOntologyMiner;
import fuzzyOntologyMiner.IMiningParams;
import fuzzyOntologyMiner.MiningParams;
import fuzzyOntologyMiner.correlationFunctions.BalancedMutualInformation;
import fuzzyOntologyMiner.postag.PosTag;

public class Main {
  public static void main(String[] args) {
    
    IMiningParams miningParams = new MiningParams();
    
    miningParams.setInputDocumentsLocation(Paths.get(args[0]));
    
    /* Search patterns */
    miningParams.addMiningPattern(PosTag.NOUN);
    miningParams.addMiningPattern(PosTag.NOUN, PosTag.NOUN_PLURAL);
    miningParams.addMiningPattern(PosTag.NOUN_PROPER, PosTag.NOUN_PLURAL);
    miningParams.addMiningPattern(PosTag.NOUN_PROPER, PosTag.NOUN_PROPER);
    miningParams.addMiningPattern(PosTag.ADJECTIVE, PosTag.NOUN);
    
    /* Term frequencies extraction parameters */
    miningParams.setTaggerModel("resources/models/left3words-wsj-0-18.tagger");
    miningParams.setWindowSize(10);
    miningParams.setTermFrequencyThresholds(0.03, 0.8);
    
    /* Concept extraction parameters */
    miningParams.setConceptFilteringThreshold(0.05);
    miningParams.setCorrelationFunction(new BalancedMutualInformation(0.67));
    
    /* Taxonomy parameters */
    miningParams.setConceptRelationFilterThreshold(0.022);
    
    /* Ontology output */
    miningParams.setOutputOntologyLocation(Paths.get(args[1]));
    
    try {
      System.out.println("Start mining...");
      FuzzyOntologyMiner.mine(miningParams);
      System.out.println("Finished");
    } catch (ClassNotFoundException | OWLOntologyCreationException | OWLOntologyStorageException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
