package fuzzyOntologyMiner.postag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pattern of {@link PosTag}.
 * 
 * This is to be used in term frequency calculation to recognize patterns of
 * interest such as (NN, NN) or (ADJ, NN)
 */
public class PosTagPattern implements Iterable<PosTag> {

  /**
   * Constructor.
   * 
   * @param tags list of {@link PosTag} objects
   */
  public PosTagPattern(PosTag... tags) {
    pattern = new ArrayList<>();

    if (tags != null) {
      for (PosTag tag : tags) {
        pattern.add(tag);
      }
    }
  }
  
  @Override
  public Iterator<PosTag> iterator() {
    return pattern.iterator();
  }
  
  /**
   * @return the size of the pattern
   */
  public int size() {
    return pattern.size();
  }

  @Override
  public String toString() {
    String toReturn = "PosTagPattern(";
    toReturn += pattern.stream().map(PosTag::name).collect(Collectors.joining(", "));
    toReturn += ")";

    return toReturn;
  }

  /** Actual list of PosTag elements; */
  private List<PosTag> pattern;

}
