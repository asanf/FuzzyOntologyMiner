package fuzzyOntologyMiner.correlationFunctions;

/**
 * Defines a method to calculate correlation between two terms.
 */
public interface CorrelationFunction {

    /**
     * Calculates the correlation between two terms, X and Y.
     * 
     * @param freq_x  frequency of the term X
     * @param freq_y  frequencyt of the term Y
     * @param freq_xy mutual frequency of X and Y (how often they appear together)
     * @return a double indicating the correlation: the higher the value, the more
     *         the terms are related to each other and vice-versa.
     */
    double calculate(double freq_x, double freq_y, double freq_xy);

}