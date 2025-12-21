package fuzzyOntologyMiner.dataStructures;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.DoubleStream;
import java.util.Arrays;
import java.util.HashMap;

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
	}

	/**
	 * Constructor.
	 * 
	 * @param size size of the matrix, which will be size * (size + 1)/2
	 */
	public TriangularMatrix(int size) {
		items = new HashMap<>(size);
		values = new double[(size * (size + 1))/2];
		idGenerator = new AtomicInteger();
	}

	/**
	 * @return a {@link Set<T>} the item saved in this matrix
	 */
	public List<T> getItems() {
		return List.copyOf(this.items.keySet());
	}

	/**
	 * Sets a value for the given term.
	 * 
	 * @param term  term whose value has to be set
	 * @param value value to associate to the given term
	 */
	public void setValue(T term, double value) {
		int termIdx = indexOf(term);
		setValue(termIdx, termIdx, value);
	}

	/**
	 * @param term term whose value is needed
	 * @return the value associated to the given term.
	 */
	public double getValue(T term) {
		int termIdx = indexOf(term);
		return values[convertIndex(termIdx, termIdx)];
	}

	/**
	 * Sets a value for a couple of terms.
	 * 
	 * @param first  first term
	 * @param second second term
	 * @param value  value to associate to the pair (first, second)
	 */
	public void setValue(T first, T second, double value) {
		setValue(indexOf(first), indexOf(second), value);
	}

	/**
	 * @param first  first term
	 * @param second second term
	 * @return value associated to the couple (first, second)
	 */
	public double getValue(T first, T second) {
		return values[convertIndex(indexOf(first), indexOf(second))];
	}

	/**
	 * Adds the term to If it is not already present, adds it to the list of terms
	 * and adjust the size of the value matrix if needed.
	 * 
	 * @param term term to add
	 * @return the index of the term
	 */
	public int indexOf(T term) {
		int index = items.computeIfAbsent(term, key -> idGenerator.getAndIncrement());
		expand();
		return index;
	}
	
	/**
	 * Increments the value associated to the given term
	 * 
	 * @param term      term whose value must be increased
	 * @param increment increment value
	 */
	public void add(T term, double increment) {
		double value = getValue(term);
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
	public void add(T first, T second, double increment) {
		double value = getValue(first, second);
		value += increment;
		setValue(first, second, value);
	}

	public void delete(List<T> terms) {
		for (T term: terms) {
			items.remove(term);
		}
	}

	/**
	 * Normalizes the values by dividing them by the given value
	 * 
	 * @param normalizer dividing factor to use to normalize
	 */
	public void normalizeBy(double normalizer) {
		if (normalizer != 0) {
			Arrays.setAll(values, i -> values[i]/normalizer);
		}
	}

	public double max() {
		OptionalDouble maxOpt = DoubleStream.of(values).parallel().max();
		return maxOpt.isEmpty() ? 0.0 : maxOpt.getAsDouble();
	}

	public double min() {
		OptionalDouble minOpt = DoubleStream.of(values).parallel().min();
		return minOpt.isEmpty() ? 0.0 : minOpt.getAsDouble();
	}

	public void applyLinearScaling()	{
		final double min = min();
		final double max = max();
		final double range = max - min;

		/* normalize using linear scaling */
		if (range != 0) {
			Arrays.setAll(values, i -> (values[i] - min)/range);
		}
	}

	/**
	 * Converts the given row and column to an index for the underlying
	 * 1-d array.
	 * 
	 * @param row row index
	 * @param col col index
	 * @return the linearized index
	 */
	private int convertIndex(int row, int col) {
		if (row < col) {
			int tmp = row;
			row = col;
			col = tmp;
		}

		return ((row * (row + 1)) / 2) + col;
	}

	/**
	 * Expands the size of the matrix by adding a new row and column.
	 */
	private void expand() {
		int currentSize = items.size();
		int valueSize = convertIndex(currentSize, currentSize) + 1;

		if (valueSize >= values.length) {
			values = Arrays.copyOf(values, values.length * 2);
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
		values[index] = value;
	}

	/** Linearized 2D matrix */
	private double[] values;

	/** List of objects representing rows and columns of the matrix */
	private Map<T, Integer> items;

	/** Object used to generate unique, progressive integers */
	private AtomicInteger idGenerator;

	/** Minimum size of the matrix */
	private static final int minSize = 100;
}