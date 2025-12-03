package fuzzyOntologyMiner.domain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalDouble;

import fuzzyOntologyMiner.dataStructures.TriangularMatrix;

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
		super(terms);

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
	 * Finalizes the Context vectors calculation by
	 *  - normalizing memberships in the [0, 1] range
	 *  - deleting concepts which have no describing term with a membership value
	 *    greater than alpha
	 * @param alpha threshold value in range [0, 1] to determine meaningful concepts. 
	 *              A concept is meaningful if it has at least a describing term with membership value >= alpha
	 * @throws IllegalArgumentException if alpha is outside the range [0, 1]
	 */
	public void finalize(double alpha) throws IllegalArgumentException {
	  normalizeMemberships();
	  conceptPruning(alpha);
	}

	/**
	 * Normalizes the membership degrees in the [0, 1] interval.
	 */
	private void normalizeMemberships() {
		OptionalDouble minOpt = values.stream().mapToDouble(Double::doubleValue).min();
		OptionalDouble maxOpt = values.stream().mapToDouble(Double::doubleValue).max();

		if (minOpt.isEmpty() || maxOpt.isEmpty()) {
			return;
		}

		final double min = minOpt.getAsDouble();
		final double max = maxOpt.getAsDouble();
		final double range = max - min;

		/* normalize using linear scaling */
		values.replaceAll(value -> (value - min) / range);
	}
	
	/**
	 * Cleans the list of concept by deleting those with no describing term whose relationship value
	 * is greater than a threshold.
	 * @param alpha parameter in range [0, 1] to use to prune concepts.
	 * @throws IllegalArgumentException if alpha is outside the range [0, 1]
	 */
	private void conceptPruning(double alpha) throws IllegalArgumentException {
	  if (alpha < 0 || alpha > 1) throw new IllegalArgumentException("alpha not in [0, 1]");
	  
	  boolean significant;
	  int termIdx;
	  
	  for (TermType concept: terms) {
	    significant = false;
	    termIdx = 0;
	    
	    /*
       * Check if the concept has at least a term describing it with a membership of
       * at least alpha
       */
	    while (!significant && termIdx < terms.size()) {
	      significant = getMembership(concept, terms.get(termIdx)) >= alpha;
	      termIdx++;
	    }
	    
	    /* If there is no such term, the concept is not relevant */
	    if (!significant) {
	      deleteConcept(concept);
	    }
	  }
	}
	
	 /**
   * Deletes a concept.
   * 
   * @param concept the concept to delete.
   */
  private void deleteConcept(TermType concept) {
    setValue(concept, -1);
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