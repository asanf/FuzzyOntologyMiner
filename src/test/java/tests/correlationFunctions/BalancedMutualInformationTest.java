package tests.correlationFunctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fuzzyOntologyMiner.correlationFunctions.BalancedMutualInformation;

public class BalancedMutualInformationTest {

    @Test
    public void testDefaultConstructor() {
        BalancedMutualInformation bmi = new BalancedMutualInformation();
        // Default beta should be 0.51, test indirectly by calculating
        double result = bmi.calculate(0.5, 0.5, 0.25);
        // Just check it doesn't throw and returns a value
        assertTrue(!Double.isNaN(result));
    }

    @Test
    public void testConstructorWithValidBeta() {
        BalancedMutualInformation bmi = new BalancedMutualInformation(0.7);
        // Test calculation
        double result = bmi.calculate(0.5, 0.5, 0.25);
        // Since beta is private, test that it doesn't throw and calculate correctly
        assertTrue(!Double.isNaN(result));
    }

    @Test
    public void testConstructorWithInvalidBetaLow() {
        assertThrows(IllegalArgumentException.class, () -> {
            new BalancedMutualInformation(-0.1);
        });
    }

    @Test
    public void testConstructorWithInvalidBetaHigh() {
        assertThrows(IllegalArgumentException.class, () -> {
            new BalancedMutualInformation(1.1);
        });
    }

    @Test
    public void testCalculateIndependent() {
        BalancedMutualInformation bmi = new BalancedMutualInformation(0.5);
        double result = bmi.calculate(0.5, 0.5, 0.25);
        // For independent terms, should be around 0
        assertEquals(0.0, result, 1e-5);
    }

    @Test
    public void testCalculatePositiveCorrelation() {
        BalancedMutualInformation bmi = new BalancedMutualInformation(0.5);
        double result = bmi.calculate(0.5, 0.5, 0.4);
        assertTrue(result > 0);
    }

    @Test
    public void testCalculateNegativeCorrelation() {
        BalancedMutualInformation bmi = new BalancedMutualInformation(0.5);
        double result = bmi.calculate(0.5, 0.5, 0.1);
        assertTrue(result < 0);
    }

    @Test
    public void testCalculateWithDifferentBeta() {
        BalancedMutualInformation bmi1 = new BalancedMutualInformation(0.0);
        BalancedMutualInformation bmi2 = new BalancedMutualInformation(1.0);
        double result1 = bmi1.calculate(0.5, 0.5, 0.3);
        double result2 = bmi2.calculate(0.5, 0.5, 0.3);
        // Results should be different
        assertTrue(Math.abs(result1 - result2) > 1e-10);
    }
}