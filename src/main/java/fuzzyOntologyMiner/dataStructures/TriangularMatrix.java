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

	public TriangularMatrix(List<T> terms) {
		this(terms.size());
		expand();
	}

	/**
	 * Constructor.
	 * 
	 * @param size size of the matrix, which will be size * (size + 1)/2
	 */
	public TriangularMatrix(int size) {
		terms = new ArrayList<>(size);
		values = new ArrayList<>(size);
	}

	
	/**
	 * Sets a value for the given term.
	 * 
	 * @param term  term whose value has to be set
	 * @param value value to associate to the given term
	 */
	protected void setValue(T term, double value) {
		int termIdx = addTerm(term);
		setValue(termIdx, termIdx, value);
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

		return values.get(convertIndex(termIdx, termIdx));
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

		setValue(i, j, value);
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

		return values.get(convertIndex(i, j));
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
			expand();
		}

		return terms.indexOf(term);
	}

	/**
	 * Expands the size of the matrix by adding a new row and column.
	 */
	private void expand() {
		int currentSize = terms.size();
		int valueSize = convertIndex(currentSize, currentSize) + 1;

		while (values.size() <= valueSize) {
			values.add(0.0);
		}
	}

	/**
	 * Sets the value at the given row and column.
	 * 
	 * @param row   row index
	 * @param col   column index
	 * @param value value to set
	 */
	private void setValue(int row, int col, double value) {
		int index = convertIndex(row, col);

		values.set(index, value);
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
	 * @param normalizer dividing factor to use to normalize
	 */
	protected void normalizeBy(double normalizer) {
		if (normalizer != 0) {
			values.replaceAll(value -> value/normalizer);
		}
	}

	protected int convertIndex(int row, int col) {
		if (row < col) {
			int tmp = row;
			row = col;
			col = tmp;
		}

		return ((row * (row + 1)) / 2) + col;
	}

	/** Linearized 2D matrix */
	protected List<Double> values;

	/** List of objects representing rows and columns of the matrix */
	protected List<T> terms;

	/** Minimum size of the matrix */
	private static final int minSize = 100;
}