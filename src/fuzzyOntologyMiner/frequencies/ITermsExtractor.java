package fuzzyOntologyMiner.frequencies;

import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.TaggedWord;
import fuzzyOntologyMiner.postag.PosTagPattern;

/**
 * Interface for an object responsible to match a list of text against a list of
 * {@link PosTagPattern} objects.
 * 
 * TODO this shouldn't be a matcher, rather a terms extractor
 */
public interface ITermsExtractor {

  /**
   * Prepares the matcher to match against the given list of patterns.
   * 
   * @param patterns list of patterns to match
   */
  void compile(List<PosTagPattern> patterns);

  /**
   * Extract terms from the given list of tagged words based on the patterns 
   * registered with {@link ITermsExtractor#compile(List)}.
   * 
   * Words are considered singularly when no match with a registered pattern is found
   * or no pattern has been registered.
   * 
   * @param taggedWords list of {@link TaggedWord} to check.
   * @return set of strings representing the terms 
   */
  Set<String> extract(List<TaggedWord> taggedWords);
}
