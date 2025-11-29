package fuzzyOntologyMiner;

import java.nio.file.Path;
import java.util.List;

import fuzzyOntologyMiner.correlationFunctions.ICorrelationFunction;
import fuzzyOntologyMiner.domain.ContextVectors;
import fuzzyOntologyMiner.postag.PosTag;
import fuzzyOntologyMiner.postag.PosTagPattern;

/**
 * Collection of parameters to customize the ontology extraction.
 */
public interface IMiningParams {
	
	/**
	 * Sets the path to the folder containing the documents to analyze.
	 * @param inputLocation path to the folder 
	 */
	void setInputDocumentsLocation(Path inputLocation);
	
	/**
	 * @return the path to the folder containing the documents to analyze
	 */
	Path getInputDocumentsLocation();
	
	/**
	 * Sets the path in which the extracted ontology is to be saved.
	 *  
	 * @param ontologyPath path to the desired ontology file
	 */
	void setOutputOntologyLocation(Path ontologyPath);
	
	/**
	 * @return the path to the desired ontology file
	 */
	Path getOutputOntologyLocation();
	
	/**
	 * Sets the path to the model to be used with the Stanford Pos Tagger
	 * @param modelFile path to the model file
	 */
	void setTaggerModel(String modelFile);
	
	/**
	 * @return the path to the tagger model file
	 */
	String getTaggerModel();
	
	/**
	 * Sets the size of the sliding text window used to compute 
	 * term frequencies. It is suggested to keep the window fairly small,
	 * avoiding it to cover an entire sentences 
	 * (i.e. something around 5 to 10 words).
	 * 
	 * @param windowSize number of terms in a tagged sentence to be considered a window
	 */
	void setWindowSize(int windowSize);
	
	/**
	 * @return the number of terms to be analyzed in a single window
	 */
	int getWindowSize();
	
	/**
	 * Adds a pattern of POS Tags to search in the document corpus and treat as terms of interest.  
	 * @param tags ordered sequence of {@link PosTag} objects representing the pattern
	 */
	void addMiningPattern(PosTag...tags);
	
	
	List<PosTagPattern> getMiningPatterns();

	/**
	 * Sets thresholds value to discard noisy terms whose
	 * frequency is either too low or too high.
	 * 
	 * @param lower minimum frequency allowed, discard terms with a strictly lower frequency. Value in [0, 1]
	 * @param upper maximum frequency allowed, discard terms with a strictly higher frequency. Value in [0, 1]
	 */
	void setTermFrequencyThresholds(double lower, double upper);
	
	/**
	 * @return the frequency under which terms are discarded
	 */
	double getMinAllowedTermFrequency();
	
	/**
	 * @return the frequency over which terms are discarded
	 */
	double getMaxAllowedTermFrequency();
	
	// TODO Patterns to observe
	
	/**
	 * Sets the {@link ICorrelationFunction} to be used in computing the 
	 * {@link ContextVectors}.
	 * 
	 * @param function correlation function to be used.
	 */
	void setCorrelationFunction(ICorrelationFunction function);
	
	/**
	 * @return the correlation function to use in computing {@link ContextVectors}
	 */
	ICorrelationFunction getCorrelationFunction();
	
	/**
	 * Represents the parameter alpha in the original work.
	 * 
	 * If the Context Vector of a concept contains only 
	 * terms which a really low membership value, than
	 * that concept it is not meaningful in the ontology,
	 * and therefore discarded.
	 * 
	 * @param treshold a value in range [0, 1]
	 */
	void setConceptFilteringThreshold(double treshold);
	
	/**
	 * Returns the parameter alpha to be used in filtering
	 * concepts.
	 * 
	 * @return returns a value in [0, 1]
	 */
	double getConceptFilteringThreshold();

	/**
	 * Represents the value lambda in the original work.
	 * It is used to filter non meaningful relations 
	 * between concepts.
	 * 
	 * @param threshold value in [0, 1]
	 */
	void setConceptRelationFilterThreshold(double threshold);
	
	/**
	 * @return the minimum allowed value for a relation between two concepts in a 
	 *  	   Taxonomy.
	 */
	double getConceptRelationFilterThreshold();
}
