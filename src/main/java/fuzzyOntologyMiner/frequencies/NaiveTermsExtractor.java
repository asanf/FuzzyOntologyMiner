package fuzzyOntologyMiner.frequencies;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.TaggedWord;
import fuzzyOntologyMiner.postag.PosTag;
import fuzzyOntologyMiner.postag.PosTagPattern;

public class NaiveTermsExtractor implements ITermsExtractor {

  @Override
  public void compile(List<PosTagPattern> patterns) {
    this.patterns = patterns;
  }

  @Override
  public Set<String> extract(List<TaggedWord> taggedWords) {
    Set<String> results = new HashSet<>();
    boolean matched;
    
    for (int currWord = 0; currWord < taggedWords.size(); currWord++) {
      matched = false;
      
      for (PosTagPattern pattern: patterns) {
        String term = getTerm(taggedWords, currWord, pattern);
        if (!(term.isBlank() || term.isEmpty())) {
          results.add(term);
          matched = true;
        }
      }
      
      if (!matched) {
        results.add(taggedWords.get(currWord).word());
      }
    }
    
    return results;
  }
  
  String getTerm(List<TaggedWord> taggedWords, int startIdx, PosTagPattern pattern) {
    String term = "";
    
    /* Not enough terms to match the pattern */
    if (pattern.size() >= taggedWords.size() - startIdx + 1) return term;
    
    int currIdx = startIdx;
    
    for (PosTag tag: pattern) {
      TaggedWord currWord = taggedWords.get(currIdx); 
      if (tag.equals(currWord.tag())) {
        term += currWord.word() + " ";
      }
      
      currIdx++;
    }
    
    term = term.trim();
    
    return term;
  }

  /** List of patterns to match */
  private List<PosTagPattern> patterns;
}
