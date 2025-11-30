package fuzzyOntologyMiner.dataStructures;

import java.util.List;
import java.util.ArrayList;

/**
 * Models a Triangular matrix with dynamic expansion.
 * 
 * The matrix stores values associated to a list of objects, and allows to store
 * values associated to a single object (i.e. on the main diagonal of the
 * matrix) or to couples of objects.
 * 
 * @param <T> type of the objects to associated to rows and columns of the
 *            matrix.
 */
public class TriangularMatrix<T> {

	/**
	 * Constructor.
	 * 
	 * Allocates a dynamic triangular matrix using a default size.
	 */
	public TriangularMatrix() {
		this(minSize);
	}

	/**
	 * Constructor.
	 * 
	 * @param size size of the matrix, which will be size * (size + 1)/2
	 */
	public TriangularMatrix(int size) {
		terms = new ArrayList<T>();
		values = allocateMatrix(size);
	}

	/**
	 * Allocates a lower triangular matrix.
	 * 
	 * @param size size of the matrix
	 * @return a 2D array representing a lower triangular matrix
	 */
	protected double[][] allocateMatrix(int size) {
		double[][] tmp = new double[size][];

		for (int i = 0; i < size; i++) {
			tmp[i] = new double[i + 1];
		}

		return tmp;
	}

	/**
	 * Allocates a new lower triangular matrix whose size is double the original.
	 * Copies the content of the original matrix into the new one.
	 * 
	 * @param matrix original matrix
	 * @return a 2D array with double the size of the original, storing the same
	 *         data as before
	 */
	protected double[][] growSize(double[][] matrix) {
		int newSize = matrix.length * 2;
		double[][] newMatrix = allocateMatrix(newSize);
		for (int i = 0; i < matrix.length; i++) {
			System.arraycopy(matrix[i], 0, newMatrix[i], 0, matrix[i].length);
		}
		return newMatrix;
	}

	/**
	 * Grows the size of the matrix if needed.
	 */
	protected void checkSize() {
		if (terms.size() > values.length) {
			values = growSize(values);
		}
	}

	/**
	 * Sets a value for the given term.
	 * 
	 * @param term  term whose value has to be set
	 * @param value value to associate to the given term
	 */
	protected void setValue(T term, double value) {
		int termIdx = addTerm(term);
		values[termIdx][termIdx] = value;
	}

	/**
	 * @param term term whose value is needed
	 * @return the value associated to the given term.
	 */
	protected double getValue(T term) {
		if (!terms.contains(term)) {
			return -1;
		}

		int termIdx = terms.indexOf(term);

		return values[termIdx][termIdx];
	}

	/**
	 * Sets a value for a couple of terms.
	 * 
	 * @param first  first term
	 * @param second second term
	 * @param value  value to associate to the pair (first, second)
	 */
	protected void setValue(T first, T second, double value) {
		int i = addTerm(first);
		int j = addTerm(second);

		if (i < j) {
			values[j][i] = value;
		} else {
			values[i][j] = value;
		}
	}

	/**
	 * @param first  first term
	 * @param second second term
	 * @return value associated to the couple (first, second)
	 */
	protected double getValue(T first, T second) {
		if (!(terms.contains(first) && terms.contains(second))) {
			return -1;
		}

		int i = terms.indexOf(first);
		int j = terms.indexOf(second);

		if (i < j) {
			return values[j][i];
		} else {
			return values[i][j];
		}
	}

	/**
	 * Adds the term to If it is not already present, adds it to the list of terms
	 * and adjust the size of the value matrix if needed.
	 * 
	 * @param term term to add
	 * @return the index of the term
	 */
	protected int addTerm(T term) {
		if (!terms.contains(term)) {
			terms.add(term);
			checkSize();
		}

		return terms.indexOf(term);
	}

	/**
	 * Increments the value associated to the given term
	 * 
	 * @param term      term whose value must be increased
	 * @param increment increment value
	 */
	protected void add(T term, double increment) {
		double value = 0;
		if (terms.contains(term)) {
			value = getValue(term);
		}

		value += increment;

		setValue(term, value);
	}

	/**
	 * Increments the value associated to a pair of terms.
	 * 
	 * @param first     first term
	 * @param second    second term
	 * @param increment increment value
	 */
	protected void add(T first, T second, double increment) {
		double value = 0;
		if (terms.contains(first) && terms.contains(second)) {
			value = getValue(first, second);
		}

		value += increment;

		setValue(first, second, value);
	}

	/**
	 * Normalizes the values by dividing them by the given value
	 * 
	 * @param value dividing factor to use to normalize
	 */
	protected void normalizeBy(double value) {
		for (int i = 0; i < values.length; i++) {
			for (int j = 0; j < values[i].length; j++) {
				values[i][j] /= value;
			}
		}
	}

	/** Actual 2D matrix */
	protected double values[][];

	/** List of objects representing rows and columns of the matrix */
	protected List<T> terms;

	/** Minimum size of the matrix */
	private static final int minSize = 100;
}