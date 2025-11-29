package fuzzyOntologyMiner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import fuzzyOntologyMiner.correlationFunctions.ICorrelationFunction;
import fuzzyOntologyMiner.postag.PosTag;
import fuzzyOntologyMiner.postag.PosTagPattern;

/**
 * Concrete implementation of the IMiningParams interface providing storage for
 * all ontology extraction parameters and setting common default values.
 */
public class MiningParams implements IMiningParams {

  /**
   * Constructs MiningParams with sensible default configuration values. Paths and
   * the CorrelationFunction are initialized to null and must be set externally.
   */
  public MiningParams() {
    // Paths are null until explicitly set
    this.inputLocation = null;
    this.outputOntologyPath = null;
    this.taggerModelFile = null;

    // Set default configuration parameters
    this.windowSize = DEFAULT_WINDOW_SIZE;
    this.minTermFrequency = DEFAULT_MIN_FREQ;
    this.maxTermFrequency = DEFAULT_MAX_FREQ;
    this.conceptFilteringThreshold = DEFAULT_CONCEPT_FILTER_THRESHOLD;
    this.conceptRelationFilterThreshold = DEFAULT_RELATION_FILTER_THRESHOLD;

    // Correlation function is null until explicitly set
    this.correlationFunction = null;
    
    // Empty list of patterns
    this.patterns = new ArrayList<>();
  }

  // --- Implementation of IMiningParams methods ---

  @Override
  public void setInputDocumentsLocation(Path inputLocation) {
    if (!inputLocation.toFile().isDirectory()) throw new IllegalArgumentException("Path is not a directory");
    this.inputLocation = inputLocation;
  }

  @Override
  public Path getInputDocumentsLocation() {
    return this.inputLocation;
  }

  @Override
  public void setOutputOntologyLocation(Path ontologyPath) {
    this.outputOntologyPath = ontologyPath;
  }

  @Override
  public Path getOutputOntologyLocation() {
    return this.outputOntologyPath;
  }

  @Override
  public void setTaggerModel(String modelFile) {
    this.taggerModelFile = modelFile;
  }

  @Override
  public String getTaggerModel() {
    return this.taggerModelFile;
  }

  @Override
  public void setWindowSize(byte windowSize) {
    if (windowSize <= 0) {
      throw new IllegalArgumentException("Window size must be a positive number.");
    }
    this.windowSize = windowSize;
  }

  @Override
  public byte getWindowSize() {
    return this.windowSize;
  }

  @Override
  public void setTermFrequencyThresholds(double lower, double upper) {

    checkUnitValue(lower, "Term frequencies lower threshold");
    checkUnitValue(upper, "Term frequencies upper threshold");

    if (lower >= upper) {
      throw new IllegalArgumentException("'lower' must be strictly less than 'upper'.");
    }

    this.minTermFrequency = lower;
    this.maxTermFrequency = upper;
  }

  @Override
  public double getMinAllowedTermFrequency() {
    return this.minTermFrequency;
  }

  @Override
  public double getMaxAllowedTermFrequency() {
    return this.maxTermFrequency;
  }

  @Override
  public void setCorrelationFunction(ICorrelationFunction function) {
    this.correlationFunction = function;
  }

  @Override
  public ICorrelationFunction getCorrelationFunction() {
    return this.correlationFunction;
  }

  @Override
  public void setConceptFilteringThreshold(double threshold) {
    checkUnitValue(threshold, "Concept filtering threshold");
    this.conceptFilteringThreshold = threshold;
  }

  @Override
  public double getConceptFilteringThreshold() {
    return this.conceptFilteringThreshold;
  }

  @Override
  public void setConceptRelationFilterThreshold(double threshold) {
    checkUnitValue(threshold, "Concept relation threshold");
    this.conceptRelationFilterThreshold = threshold;
  }
  
  @Override
  public void addMiningPattern(PosTag... tags) {
    this.patterns.add(new PosTagPattern(tags)); 
  }

  @Override
  public List<PosTagPattern> getMiningPatterns() {
    return patterns;
  }

  @Override
  public double getConceptRelationFilterThreshold() {
    return this.conceptRelationFilterThreshold;
  }
  
  /** Checks if the given value is in the range [0, 1]
   *  
   * @param value value to verify
   * @param valueName name to include in the exception
   */
  private void checkUnitValue(double value, String valueName) {
    if (value < 0 || value > 1) {
      throw new IllegalArgumentException(valueName + " must be between 0 and 1.");
    }
  }
  
  // --- Configuration Fields ---
  private Path                 inputLocation;
  private Path                 outputOntologyPath;
  private String               taggerModelFile;
  private byte                 windowSize;
  private double               minTermFrequency;
  private double               maxTermFrequency;
  private ICorrelationFunction correlationFunction;
  private double               conceptFilteringThreshold;
  private double               conceptRelationFilterThreshold;
  private List<PosTagPattern>  patterns;

  // --- Default Values for Initialization ---
  // A window size often recommended for context analysis
  private static final byte DEFAULT_WINDOW_SIZE = 7;
  // Discard terms less frequent than 0.5%
  private static final double DEFAULT_MIN_FREQ = 0.005;
  // Discard terms more frequent than 99% (acts as a stop-word filter)
  private static final double DEFAULT_MAX_FREQ = 0.99;
  // Alpha threshold for concept relevance
  private static final double DEFAULT_CONCEPT_FILTER_THRESHOLD = 0.1;
  // Lambda threshold for relation strength
  private static final double DEFAULT_RELATION_FILTER_THRESHOLD = 0.5;
}