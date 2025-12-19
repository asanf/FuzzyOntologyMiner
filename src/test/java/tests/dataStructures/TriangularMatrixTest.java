package tests.dataStructures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;

import org.junit.jupiter.api.Test;

import fuzzyOntologyMiner.dataStructures.TriangularMatrix;

public class TriangularMatrixTest {

  @Test
  void creation() {
    TriangularMatrix<String> matrix = new TriangularMatrix<>();
    assertEquals(0, matrix.getItems().size());
  }

  @Test
  void addTerm() {
    TriangularMatrix<String> matrix = new TriangularMatrix<>();
    String term = "test";
    double expected = 1;
    matrix.add(term, expected);
    assertEquals(expected, matrix.getValue(term));

    matrix.add(term, 9);
    assertEquals(10, matrix.getValue(term));
  }

  @Test
  void addTerms() {
    TriangularMatrix<String> matrix = new TriangularMatrix<>();
    String first = "first";
    String second = "second";
    double expected = 10;

    matrix.add(first, second, expected);
    assertEquals(expected, matrix.getValue(first, second));

    matrix.add(first, second, expected);
    assertEquals(2*expected, matrix.getValue(first, second));
  }

  @Test
  void getValue() {
    TriangularMatrix<String> matrix = new TriangularMatrix<>();
    String term = "test";
    double expected = 0;

    assertEquals(expected, matrix.getValue(term));
  }

  @Test
  void getValueTerms() {
    TriangularMatrix<String> matrix = new TriangularMatrix<>();
    String first = "first";
    String second = "second";
    double expected = 0;

    assertEquals(expected, matrix.getValue(first, second));
  }

  @Test
  void size() {
    TriangularMatrix<String> matrix = new TriangularMatrix<>();

    List<String> terms = Arrays.asList("first", "second", "third");
    for (String term: terms) {
      matrix.add(term, 1);
    }

    assertEquals(terms.size(), matrix.getItems().size());
  }

  @Test
  void expand() {
    List<String> terms = List.of("First", "Second");
    TriangularMatrix<String> matrix = new TriangularMatrix<>(terms);

    matrix.add("Third", "Fourth", 10);

    assertEquals(0, matrix.getValue("First", "Second"));
  }

  @Test
  void normalize() {
    TriangularMatrix<String> matrix = new TriangularMatrix<>();
    List<String> terms = List.of("First", "Second", "Third");
    double[] values = {2, 4 ,6};

    for (int i = 0; i < 3; i++) {
      matrix.add(terms.get(i), values[i]);
    }

    matrix.normalizeBy(2);

    for (int i = 0; i < 3; i++) {
      assertEquals(values[i]/2, matrix.getValue(terms.get(i)));
    }
  }

  @Test
  void normalizeZero() {
    TriangularMatrix<String> matrix = new TriangularMatrix<>();
    List<String> terms = List.of("First", "Second", "Third");
    double[] values = {2, 4 ,6};

    for (int i = 0; i < 3; i++) {
      matrix.add(terms.get(i), values[i]);
    }

    matrix.normalizeBy(0);

    for (int i = 0; i < 3; i++) {
      assertEquals(values[i], matrix.getValue(terms.get(i)));
    }
  }

  @Test
  void linearScaling() {
    TriangularMatrix<String> matrix = new TriangularMatrix<>();
    List<String> terms = List.of("First", "Second", "Third", "Fourhh", "Fifth");
    double[] values = {5, 1, 10, 20, 15};
    /* Note: the minimum is 0 as no couple value has been setted  */
    double[] expected = DoubleStream.of(values).map(v -> (v)/(20)).toArray();

    for (int i = 0; i < 5; i++) {
      matrix.add(terms.get(i), values[i]);
    }

    matrix.applyLinearScaling();

    for (int i = 0; i < 5; i++) {
      assertEquals(expected[i], matrix.getValue(terms.get(i)));
    }
  }

  @Test
  void voidLinearScaling() {
    TriangularMatrix<String> matrix = new TriangularMatrix<>();
    matrix.applyLinearScaling();
  }
}
