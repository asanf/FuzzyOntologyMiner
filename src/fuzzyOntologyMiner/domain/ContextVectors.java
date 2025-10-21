package fuzzyOntologyMiner.domain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Context vectors creation and storage.
 * 
 * Manages context vectors, which are structures used to correlate concepts to
 * the terms describing them. For each term describing a context, a degree of
 * membership is stored indicating "how much" that term describes that concept.
 * 
 * @param <TermType> Type of the data to hold
 */
public class ContextVectors<TermType> extends TriangularMatrix<TermType> implements Iterable<TermType> {

	/**
	 * Constructor
	 * 
	 * @param terms list of terms over which concepts must be built
	 */
	public ContextVectors(List<TermType> terms) {
		super(terms.size());
		this.terms = terms;

		for (TermType term : this.terms) {
			/*
			 * The value on the diagonal of the triangular matrix is used as boolean to mark
			 * the term as concept. 1 - the term is a concept 0 - the term is not a concept
			 */
			setValue(term, 1);
		}
	}

	/**
	 * Assigns a degree of membership of a term to a concept.
	 * 
	 * @param term       term describing a concept
	 * @param concept    the concept being described by term
	 * @param membership the degree of membership of term to concept
	 */
	public void setMembership(TermType term, TermType concept, double membership) {
		setValue(term, concept, membership);
	}

	/**
	 * Returns the membership of a given term to a given concept
	 * 
	 * @param term    term describing the concept
	 * @param concept concept of interest
	 *
	 * @return the membership of term to concept
	 */
	public double getMembership(TermType term, TermType concept) {
		return getValue(term, concept);
	}

	/**
	 * Extracts the concepts from the list of managed terms.
	 *
	 * @return a list of concepts
	 */
	public List<TermType> getConcepts() {
		ArrayList<TermType> concepts = new ArrayList<TermType>();
		for (TermType concept : terms)
			if (getValue(concept) >= 0)
				concepts.add(concept);
		return concepts;
	}

	/**
	 * @return a list of all the managed terms
	 */
	public List<TermType> getTerms() {
		return terms;
	}

	/**
	 * Iterates over concepts
	 */
	public Iterator<TermType> iterator() {
		return getConcepts().iterator();
	}

	/**
	 * Deletes a concept
	 * 
	 * @param concept the concept to delete.
	 */
	public void deleteConcept(TermType concept) {
		setValue(concept, -1);
	}

	/**
	 * Normalizes the membership degrees in the [0, 1] interval.
	 */
	public void normalizeMemberships() {
		int termIdx;
		List<TermType> concepts = getConcepts();
		double min = 100000, max = -100000;

		/* Find the minimum and maximum membership values */
		for (TermType concept : concepts) {
			termIdx = terms.indexOf(concept);
			for (int j = 0; j < this.values[termIdx].length - 1; j++) {
				if (values[termIdx][j] < min)
					min = values[termIdx][j];
				if (values[termIdx][j] > max)
					max = values[termIdx][j];
			}
		}

		/* normalize using linear scaling */
		for (TermType concept : concepts) {
			termIdx = terms.indexOf(concept);

			for (int j = 0; j < this.values[termIdx].length - 1; j++) {
				values[termIdx][j] = (values[termIdx][j] - min) / (max - min);
			}
		}

	}

	public String toString() {
		String toRet = "";
		for (TermType t1 : terms) {
			toRet += (t1 + ": ");
			for (TermType t2 : terms)
				toRet += (getValue(t1, t2) + "\t");
			toRet += "\n";
		}
		return toRet;
	}
}