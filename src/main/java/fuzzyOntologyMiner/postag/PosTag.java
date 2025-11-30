package fuzzyOntologyMiner.postag;

/** Enumeration of possible POS tags */
public enum PosTag {
  ADJECTIVE("JJ"),
  ADJECTIVE_COMPARATIVE("JJR"), 
  ADJECTIVE_SUPERLATIVE("JJS"), 
  ADVERB("RB"), 
  ADVERB_COMPARATIVE("RBR"),
  ADVERB_SUPERLATIVE("RBS"), 
  CARDINAL_NUMBER("CD"), 
  COORDINATING_CONJUNCTION("CC"),
  DETERMINER("DT"), 
  EXISTENTIAL_THERE("EX"), 
  FOREIGN_WORD("FW"), 
  INTERJECTION("UH"), 
  LIST_ITEM_MARKER("LS"),
  MODAL("MD"), 
  NOUN("NN"), 
  NOUN_PLURAL("NNS"), 
  NOUN_PROPER("NNP"),
  NOUN_PROPER_PLURAL("NNPS"), 
  PARTICLE("RP"), 
  POSSESSIVE_ENDING("POS"), 
  PREDETERMINER("PDT"),
  PREPOSITION_OR_SUBORDINATING_CONJUNCTION("IN"), 
  PRONOUN_PERSONAL("PRP"), 
  PRONOUN_POSSESSIVE("PRP$"), 
  SYMBOL("SYM"), 
  TO_PARTICLE("TO"), 
  VERB_BASE_FORM("VB"),
  VERB_GERUND("VBG"), 
  VERB_NON_THIRD_SINGULAR_PRESENT("VBP"), 
  VERB_PAST_PARTICIPLE("VBN"), 
  VERB_PAST_TENSE("VBD"),
  VERB_THIRD_SINGULAR_PRESENT("VBZ"), 
  WH_ADVERB("WRB"), 
  WH_DETERMINER("WDT"), 
  WH_POSSESSIVE("WP$"), 
  WH_PRONOUN("WP");

  /** Value storing the string description of the tag */
  private final String tag;

  /**
   * Constructor
   * 
   * @param tag the string description of the tag
   */
  private PosTag(final String tag) {
    this.tag = tag;
  }

  /**
   * Checks for equality with a string representing a tag. This is done to
   * abstract this representation from the used POS Tagging library.
   * 
   * @param tag the tag to test
   * @return true if this tag and the given one are equals.
   */
  public boolean equals(String tag) {
    return this.tag.equals(tag);
  }

  public String value() {
    return tag;
  }
}
