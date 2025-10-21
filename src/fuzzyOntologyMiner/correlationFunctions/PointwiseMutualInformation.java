package fuzzyOntologyMiner.correlationFunctions;


/*
 * Mutual Information
 * metodo che calcola la dipendenza tra due entit�
 */
public class PointwiseMutualInformation implements CorrelationFunction{
	private static final int num = 2048;
	

	
	public double calculateCorrelation(double p_x, double p_y, double p_x_y) {
		
		int isZero = Double.compare(p_x_y, 0.0);
		int isOne = Double.compare(p_x_y, 1.0);
		
		if (isZero == 0)
			return -num;
		
		if (isOne == 0)
			return num;
		
		return  (Math.log(p_x_y) - Math.log(p_x) - Math.log(p_y))/Math.log(2);
	}
}
