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
		/* Keep only the strongest relation between two concepts */
		discardWeakestRelation();

		/* Calculate distance matrix */
    double[][] distance = computeDistances();

		/* Delete relations for which a strongest chain of links has been found */
    int numConcepts = concepts.size();
		for (int i = 0; i < numConcepts; i++) {
			for (int j = 0; j < numConcepts; j++) {
				if ((distance[i][j] > relations[i][j]) && relations[i][j] > 0) {
					relations[i][j] = 0;
				}
			}
		}
	}

	/**
	 * Computes a distance matrix based on relationships between concepts.
	 * 
	 * The matrix is calculated as such: 
	 * - the distance of a concept to itself is 0
	 * - the distance of two concepts (i, j) is 
	 *   - infinite if their relationship is negative
	 *   - equal to the shortest path from i to j if each edge of the path is strongest than the relationship
	 *     between i and j
	 *   - equal to their relationship otherwise
	 *   
	 * This makes it so that weak links between concepts are not considered if there is a strongest
	 * chain of links.
	 * 
	 * For this goal an adaptation of the Floyd-Warshall algorithm has been developed.
	 * The algorithm discards a path i-k, k-j if any of those two edges is weaker than the direct
	 * edge i-j. In this way it is assured that the path is considered only if
	 * 
	 * rel(i, j) < min(rel(i, k), rel(k, j)) must be true for each k
	 * 
	 * TODO: what if instead of shortest path on relations, we perform shortest path on 1-relation?
	 * If two concepts have a relation equal to.8, then 1 - rel(i, j) = .2
	 * This would make searching for the shortest path more meaningful, as a lower value indicates
	 * a stronger relation.
	 * 
	 * @return a matrix of doubles containing the distances between each pair of concepts.
	 */
  private double[][] computeDistances() {
    int i, j, k;
    int numConcepts = concepts.size();
    double distance[][] = new double[numConcepts][numConcepts];
		
    /* Initialize distance of a concept to itself */
    for (i = 0; i < numConcepts; i++) {
      distance[i][i] = 0;
    }
    
    /* Initialize distance between couple of concepts */
		for (i = 0; i < numConcepts; i++) {
			for (j = 0; j < numConcepts; j++) {			
				distance[i][j] = (relations[i][j] > 0) ? relations[i][j] : Integer.MAX_VALUE;
			}
		}
		
    /* Adaptation of Floyd-Warshall algorithms for all-pairs shortest path. */
		double cumulativeDistance;
    for (k = 0; k < numConcepts; k++) {
      for (i = 0; i < numConcepts; i++) {
        for (j = 0; j < numConcepts; j++) {

          /* Consider the path passing by k only if both i-k and k-j are strongest than the direct link */
          if (relations[i][k] > relations[i][j] && relations[k][j] > relations[i][j]) {
            cumulativeDistance = distance[i][k] + distance[k][j];
            if (cumulativeDistance < distance[i][j]) {
              distance[i][j] = cumulativeDistance;
            }
          }
        }
      }
    }
		
    return distance;
  }

	/**
   * Keeps only the strongest relationships. This is the maximum between
   * relations[i][j] and relations[j][i]
   */
  private void discardWeakestRelation() {
    int i;
    int j;
    int numConcepts = concepts.size();
    
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