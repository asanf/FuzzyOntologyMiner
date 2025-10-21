package fuzzyOntologyMiner.domain;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a taxonomy, which is an is-a relation between concepts.
 * 
 * @param <ConceptType> type representing a concept
 */
public class Taxonomy<ConceptType> implements Iterable<ConceptType> {

	/**
	 * Constructor.
	 * 
	 * @param size number of concepts to manage.
	 */
	public Taxonomy(int size) {
		concepts = new ArrayList<ConceptType>();
		relations = new double[size][size];
	}

	/**
	 * Constructor.
	 * 
	 * @param concepts list of concepts to manage.
	 */
	public Taxonomy(List<ConceptType> concepts) {
		this.concepts = concepts;
		int size = concepts.size();
		relations = new double[size][size];
	}

	/**
	 * Sets the degree of specificity of subConcept to concept
	 * 
	 * @param subConcept  contained concept
	 * @param concept     containing concept
	 * @param specificity degree of membership of subConcept to concept
	 */
	public void setSpecificity(ConceptType subConcept, ConceptType concept, double specificity) {
		int i, j;
		i = concepts.indexOf(subConcept);
		j = concepts.indexOf(concept);
		relations[i][j] = specificity;
	}

	/**
	 * Returns the specificity of subConcept to concept
	 * 
	 * @param subConcept contained concept
	 * @param concept    containing concept
	 * @return degree of membership of subConcept to concept
	 */
	public double getSpecificity(ConceptType subConcept, ConceptType concept) {
		int i, j;
		i = concepts.indexOf(subConcept);
		j = concepts.indexOf(concept);
		return relations[i][j];
	}

	/**
	 * Performs a pruning of the relations.
	 * 
	 * It removes a membership rel(A, B) if there is a chain of memberships rel(A,
	 * Xi) ... rel(Xi, Xj) ... rel(Xj, B) such that the minimum membership of this
	 * chain is greater than rel(A, B).
	 * 
	 * This means that there is a chain of sub-concepts which is stronger in its
	 * weakest link than the direct membership between the first and last.
	 */
	public void taxonomyPruning() {
		/* Value used to force high distance between disconnected nodes */
		int INFINITE = Integer.MAX_VALUE;

		/* Iteration indexes */
		int i, j, k;

		/**/
		double cumulativeDistance;
		int numConcepts = concepts.size();
		double distance[][] = new double[numConcepts][numConcepts];

		/*
		 * Keep only the strongest relationships. This is the maximum between
		 * relations[i][j] and relations[j][i]
		 */
		for (i = 0; i < numConcepts; i++) {
			for (j = i; j < numConcepts; j++) {
				if (relations[i][j] != relations[j][i]) {
					if (relations[i][j] > relations[j][i]) {
						relations[j][i] = 0;
					} else {
						relations[i][j] = 0;
					}
				}
			}
		}

		/* Initializes the distances matrix */
		for (i = 0; i < numConcepts; i++) {
			for (j = 0; j < numConcepts; j++) {
				/* distance of a node to itself is 0 */
				if (i == j) {
					distance[i][j] = 0;
					continue;
				}

				/*
				 * distance[i][j] is set to an arbitrary high distance if the nodes have
				 * negative relationship
				 */
				distance[i][j] = (relations[i][j] > 0) ? relations[i][j] : INFINITE;
			}
		}

		/*
		 * Adaptation of Floyd-Warshall algorithms for all-pairs shortest path.
		 */
		for (k = 0; k < numConcepts; k++) {
			for (i = 0; i < numConcepts; i++) {
				for (j = 0; j < numConcepts; j++) {

					/*
					 * The following checks discards a path from i to j passing by k if any of the
					 * connections in the path is weaker than the direct one from i to j.
					 * 
					 * i.e. rel(i, j) < min(rel(i, k), rel(k, j)) must be true for each k
					 */
					if (relations[i][k] > relations[i][j] && relations[k][j] > relations[i][j]) {
						cumulativeDistance = distance[i][k] + distance[k][j];
						if (cumulativeDistance < distance[i][j]) {
							distance[i][j] = cumulativeDistance;
						}
					}
				}
			}
		}

		/*
		 * Actually delete a relationship if a strongest chain of relations has been
		 * found
		 */
		for (i = 0; i < numConcepts; i++) {
			for (j = 0; j < numConcepts; j++) {
				if ((distance[i][j] > relations[i][j]) && relations[i][j] > 0) {
					relations[i][j] = 0;
				}
			}
		}
	}

	@Override
	/**
	 * Iterates over the managed concepts
	 */
	public Iterator<ConceptType> iterator() {
		return concepts.iterator();
	}

	/**
	 * 
	 * @return the list of managed concepts
	 */
	public List<ConceptType> getConcepts() {
		return concepts;
	}

	/** List of managed concepts */
	private List<ConceptType> concepts;

	/** Values expressing relationships between concepts */
	private double[][] relations;
}