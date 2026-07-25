package asanf.FOM.Util;

/*
 * Interfaccia per le misure di correlazione
 */
public interface CorrelationFunction {
	
	public double calculateCorrelation(double freq_x, double freq_y, double freq_xy);

}
