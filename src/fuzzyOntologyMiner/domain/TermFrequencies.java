package fuzzyOntologyMiner.domain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fuzzyOntologyMiner.dataStructures.TriangularMatrix;

/**
 * Allows to compute both single and mutual frequencies of terms.
 * 
 * The frequency is to be intended as
 *
 * number of text windows in which the term(s) appeared
 * ----------------------------------------------------- 
 * total number of text windows
 *
 * @param <TermType> Type of the terms
 */
public class TermFrequencies<TermType> extends TriangularMatrix<TermType> implements Iterable<TermType> {

	/**
	 * Constructor.
	 */
	public TermFrequencies() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param size initial size.
	 */
	public TermFrequencies(int size) {
		super(size);
		frequenciesAlreadyCalculated = false;
	}

	/**
	 * Adds an occurrence of the given term.
	 * 
	 * @param term term to register
	 * @return false if the frequencies were already calculated (i.e. the occurrence
	 *         has not been counted)
	 */
	public boolean addOccurrence(TermType term) {
		if (frequenciesAlreadyCalculated)
			return false;

		add(term, 1);

		return true;
	}

	/**
	 * Adds an occurrence of the given couple of terms.
	 * 
	 * @param firstTerm  first term of the couple
	 * @param secondTerm second term of the couple
	 * @return false if the frequencies were already calculated (i.e. the occurrence
	 *         has not been counted)
	 */
	public boolean addOccurrence(TermType firstTerm, TermType secondTerm) {
		if (frequenciesAlreadyCalculated)
			return false;

		add(firstTerm, secondTerm, 1);
		return true;
	}

	/**
	 * Calculates the actual frequencies of terms. To be called once the terms
	 * occurrence counting phase is ended.
	 * 
	 * @return false if the frequencies have already been computed
	 */
	public boolean computeFrequencies() {
		if (frequenciesAlreadyCalculated)
			return false;

		normalizeBy(totWindows);
		frequenciesAlreadyCalculated = true;
		return true;
	}

	/**
	 * Increments the total number of text windows.
	 * 
	 * @return false if the frequencies have already been computed
	 */
	public boolean augmentWindows() {
		if (frequenciesAlreadyCalculated)
			return false;

		totWindows += 1;
		return true;
	}

	/**
	 * Returns the frequency of the given term.
	 * 
	 * @param term term whose frequency is needed.
	 * @return the frequency of the term. Special values: -1: the term does not
	 *         exist; -2: the frequencies have already been calculated
	 */
	public double getFrequency(TermType term) {
		if (!frequenciesAlreadyCalculated)
			return -2;

		return getValue(term);
	}

	/**
	 * Returns the frequency of the given pair of terms.
	 * 
	 * @param firstTerm  first term
	 * @param secondTerm second term
	 * @return the frequency of the couple. Special values: -1: the term does not
	 *         exist; -2: the frequencies have already been calculated
	 */
	public double getFrequency(TermType firstTerm, TermType secondTerm) {

		if (!frequenciesAlreadyCalculated)
			return -2;
		return getValue(firstTerm, secondTerm);
	}

	/**
	 * @return the total number of windows
	 */
	public int getTotWindows() {
		return totWindows;
	}

	/**
	 * Filters the terms whose frequency is too low or too high.
	 * 
	 * TODO: why is this done only for single terms and not for couples?
	 * 
	 * @param lower threshold below which a term must be deleted
	 * @param upper threshold over which a term must be deleted
	 * @return false if the frequencies have not been calculated yet
	 */
	public boolean filterTerms(double lower, double upper) {
		double f;
		int termIdx;

		if (!frequenciesAlreadyCalculated)
			return false;

		for (TermType term : terms) {
			f = getFrequency(term);
			if ((f < lower || f > upper)) {
				termIdx = terms.indexOf(term);
				terms.set(termIdx, null);
			}
		}

		return true;
	}

	@Override
	/**
	 * Iterates over terms
	 */
	public Iterator<TermType> iterator() {
		return getTerms().iterator();
	}

	/**
	 * @return the list of terms
	 */
	public List<TermType> getTerms() {
		List<TermType> notNull = new ArrayList<TermType>();

		for (TermType term : terms) {
			if (term != null)
				notNull.add(term);
		}

		return notNull;
	}

	/** Total number of text windows. Used to compute frequencies */
	private int totWindows;

	/** Flag indicating the frequencies have already been calculated */
	private boolean frequenciesAlreadyCalculated;

}