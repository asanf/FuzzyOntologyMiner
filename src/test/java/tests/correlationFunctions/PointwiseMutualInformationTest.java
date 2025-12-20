package tests.correlationFunctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fuzzyOntologyMiner.correlationFunctions.PointwiseMutualInformation;

public class PointwiseMutualInformationTest {

    private PointwiseMutualInformation pmi = new PointwiseMutualInformation();

    @Test
    public void testCalculateZeroFrequency() {
        // When freq_x_y = 0, should return -4096
        double result = pmi.calculate(0.5, 0.5, 0.0);
        assertEquals(-4096, result);
    }

    @Test
    public void testCalculateOneFrequency() {
        // When freq_x_y = 1, should return 4096
        double result = pmi.calculate(0.5, 0.5, 1.0);
        assertEquals(4096, result);
    }

    @Test
    public void testCalculateIndependent() {
        // When terms are independent, PMI should be 0
        double result = pmi.calculate(0.5, 0.5, 0.25);
        assertEquals(0.0, result, 1e-10);
    }

    @Test
    public void testCalculatePositiveCorrelation() {
        // When terms are positively correlated
        double result = pmi.calculate(0.5, 0.5, 0.4);
        assertTrue(result > 0);
    }

    @Test
    public void testCalculateNegativeCorrelation() {
        // When terms are negatively correlated
        double result = pmi.calculate(0.5, 0.5, 0.1);
        assertTrue(result < 0);
    }

    @Test
    public void testCalculateNormalCase() {
        // Normal calculation: PMI = log2(p_xy / (p_x * p_y))
        double p_x = 0.3;
        double p_y = 0.4;
        double p_xy = 0.2;
        double expected = (Math.log(p_xy) - Math.log(p_x) - Math.log(p_y)) / Math.log(2);
        double result = pmi.calculate(p_x, p_y, p_xy);
        assertEquals(expected, result, 1e-10);
    }
}