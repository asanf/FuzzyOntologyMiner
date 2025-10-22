package fuzzyOntologyMiner.correlationFunctions;

/**
 * Pointwise Mutual Information.
 * 
 * Implementation of the {@link ICorrelationFunction} interface which calculates
 * the correlation using standard pointwise mutual information, i.e.
 * 
 * PMI(X, Y) = log2{p(X, Y) / [p(X)p(Y)]}
 */
public class PointwiseMutualInformation implements ICorrelationFunction {

	@Override
	public double calculate(double freq_x, double freq_y, double freq_x_y) {

		int isZero = Double.compare(freq_x_y, 0.0);
		int isOne = Double.compare(freq_x_y, 1.0);

		/*
		 * The terms are really disconnected from one another, return really low integer
		 * to reinforce non-correlation
		 */
		if (isZero == 0) {
			return -arbitrarilyHighInt;
		}

		/*
		 * The terms are intertwined, return really high integer to reinforce
		 * correlation
		 */
		if (isOne == 0) {
			return arbitrarilyHighInt;
		}

		/*
		 * Returns the pointwise mutual information The original formula has been
		 * rewritten to minimize machine errors due to double products and divisions.
		 */
		return (Math.log(freq_x_y) - Math.log(freq_x) - Math.log(freq_y)) / Math.log(2);
	}

	/* Integer used to express very high or very low correlation between terms */
	private static final int arbitrarilyHighInt = 4096;
}