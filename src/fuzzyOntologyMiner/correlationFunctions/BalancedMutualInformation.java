package fuzzyOntologyMiner.correlationFunctions;

/**
 * Balanced Mutual Information (BMI)
 * 
 * Implementation of the {@link CorrelationFunction} which uses the <b>Balanced
 * Mutual Information</b>, which takes in consideration both the presence and
 * the <i>absence</i> of terms.
 * 
 * This uses {@link PointwiseMutualInformation} to compute single terms
 * correlating all the permutations of presence and absence of terms. A
 * parameter controls how much importance to give to the presence of a term when
 * the other is absent.
 * 
 * BMI(X, Y) = beta * {[p(X, Y) * MI(X, Y)] + [p(!X, !Y) * MI(!X, !Y)]} - (1 - *
 * beta) * {[p(X, !Y) * MI(X, !Y)] + [p(!X, Y) * MI(!X, Y)]}
 * 
 * The first term, multiplied by beta, considers the mutual presence and mutual
 * absence of the two terms. The second term, multiplied by (1 - beta), checks
 * situations in which a term is present and other is absent.
 */
public class BalancedMutualInformation implements CorrelationFunction {

	/** Base constructor, uses default beta value */
	public BalancedMutualInformation() {
		this(def_beta);
	}

	/**
	 * Constructor
	 * 
	 * @param beta parameter in [0, 1] controlling the importance mutual presence
	 *             and absence of the terms
	 * @throws IllegalArgumentException if the parameter is outside the range [0, 1]
	 */
	public BalancedMutualInformation(double beta) throws IllegalArgumentException {
		if (beta < 0.0 || beta > 1.0) {
			throw new IllegalArgumentException("Beta must be in the range [0, 1]");
		}

		this.beta = beta;
	}

	@Override
	public double calculate(double p_x, double p_y, double p_x_y) {
		double p_nx_ny = 1 - (p_x + p_y - p_x_y); // Pr(!x,!y)
		double p_x_ny = p_x - p_x_y; // Pr(x, !y)
		double p_nx_y = p_y - p_x_y; // Pr(!x, y)
		double p_nx = 1 - p_x; // Pr(!x)
		double p_ny = 1 - p_y; // Pr(!y)
		double mi_x_y, mi_nx_ny, mi_x_ny, mi_nx_y;

		mi_x_y = mutualInformation.calculate(p_x, p_y, p_x_y);
		mi_nx_ny = mutualInformation.calculate(p_nx, p_ny, p_nx_ny);
		mi_x_ny = mutualInformation.calculate(p_x, p_ny, p_x_ny);
		mi_nx_y = mutualInformation.calculate(p_nx, p_y, p_nx_y);

		return beta * (p_x_y * mi_x_y + p_nx_ny * mi_nx_ny) - (1 - beta) * (p_x_ny * mi_x_ny + p_nx_y * mi_nx_y);
	}

	/**
	 * Parameter which controls the importance of the presence of a term when the
	 * other is absent
	 */
	private double beta;

	/** Default value of the parameter {@link BalancedMutualInformation#beta} */
	private static final double def_beta = 0.51f;

	/** Pointwise mutual information used to calculate the terms of the BMI */
	private static PointwiseMutualInformation mutualInformation = new PointwiseMutualInformation();
}