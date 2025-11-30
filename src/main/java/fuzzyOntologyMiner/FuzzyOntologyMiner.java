package fuzzyOntologyMiner;

import java.io.File;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import fuzzyOntologyMiner.correlationFunctions.ICorrelationFunction;
import fuzzyOntologyMiner.domain.ContextVectors;
import fuzzyOntologyMiner.domain.StopWords;
import fuzzyOntologyMiner.domain.Taxonomy;
import fuzzyOntologyMiner.domain.TermFrequencies;
import fuzzyOntologyMiner.frequencies.ITermsExtractor;
import fuzzyOntologyMiner.frequencies.NaiveTermsExtractor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import edu.stanford.nlp.process.Morphology;

public class FuzzyOntologyMiner {

  /**
   * Single entry method to extract an ontology from a corpus of documents.
   * 
   * @param params mining parameters
   */
  public static void mine(IMiningParams params) {
    /* Extract terms and their frequencies */
    TermFrequencies<String> termFrequencies = extractFrequencies(params);
    
    /* Create context vectors */
    ContextVectors<String> contextVectors = createContextVectors(termFrequencies, 
                                                                 params.getCorrelationFunction(),
                                                                 params.getConceptFilteringThreshold());
    
    /* Create a taxonomy */
    Taxonomy<String> taxonomy = createTaxonomy(contextVectors, params.getConceptRelationFilterThreshold());
    
    /* Save it as an OWL ontology */
    saveOWL2FuzzyOntology(taxonomy, params.getOutputOntologyLocation());
  }

  /**
   * Method which computes the frequency matrix of the terms in the document
   * corpus.
   * 
   * @param params {@link IMiningParams} object containing the mining
   *               configuration
   * 
   * @return A {@link TermFrequencies<String>} containing the terms and their
   *         absolute and relative frequencies.
   */
  public static TermFrequencies<String> extractFrequencies(IMiningParams params) {
    /* Output object */
    TermFrequencies<String> frequencies = new TermFrequencies<String>();
    
    /* POS Tagger
     * TODO tagger loading could be done in a singleton */
    MaxentTagger tagger = new MaxentTagger(params.getTaggerModel());
    
    /* Object to extract terms of interest from sentences based on POS Tag patterns */
    ITermsExtractor termsExtractor = new NaiveTermsExtractor();
    termsExtractor.compile(params.getMiningPatterns());
    int windowSize = params.getWindowSize();

    /* Document corpus navigation 
     * TODO: this navigation is not recursive */
    File dir = params.getInputDocumentsLocation().toFile();
    File[] listOfFiles = dir.listFiles();
    for (File file : listOfFiles) {
      /* Examines files only */
      if (!file.isFile()) continue;

      List<List<HasWord>> sentences;
      try {
        // TODO generalize document read (pdf, text, word, ecc.)
        sentences = MaxentTagger.tokenizeText(new BufferedReader(new FileReader(file)));
      } catch (FileNotFoundException e) {
        continue;
      }

      /* For each sentence */
      for (List<HasWord> sentence : sentences) {
        /* prepare the sentence for processing */
        List<TaggedWord> preparedSentence = prepareSentence(tagger.tagSentence(sentence));

        /* For each text window */
        int numWindows = preparedSentence.size() - windowSize + 1;
        for (int windowIdx = 0; windowIdx < numWindows; windowIdx++) {
          frequencies.augmentWindows();
          List<TaggedWord> textWindow = preparedSentence.subList(windowIdx, windowIdx + windowSize);
          TreeSet<String> termsInWindow = new TreeSet<>();
          termsInWindow.addAll(termsExtractor.extract(textWindow));

          for (String term : termsInWindow) {
            frequencies.addOccurrence(term);
            for (String otherTerm : termsInWindow.tailSet(term, false)) {
              frequencies.addOccurrence(term, otherTerm);
            }
          }
        }
      }
    }

    /* Calculate the frequencies */
    frequencies.computeFrequencies();
    
    /* Filter out noisy terms */
    frequencies.filterTerms(params.getMinAllowedTermFrequency(), params.getMaxAllowedTermFrequency());
    return frequencies;
  }

  /**
   * Computes the {@link ContextVectors} by using a given
   * {@link ICorrelationFunction}.
   * 
   * @param frequencies Object containing single and mutual frequencies of terms
   * @param corrFunc    Correlation function to use
   * @param alpha       Threshold value in [0, 1] to discard non relevant concepts
   * @return a {@link ContextVectors} object containing all relevant concepts
   */
  public static ContextVectors<String> createContextVectors(TermFrequencies<String> frequencies,
      ICorrelationFunction corrFunc, double alpha) {

    List<String> terms = frequencies.getTerms();
    ContextVectors<String> contextVectors = new ContextVectors<String>(terms);

    /* Each term that has been found is a candidate concept */
    for (String concept : terms) {

      /* Retrieve the frequency of the candidate concept */
      double fc = frequencies.getFrequency(concept);

      /* Each term potentially describes the candidate concept */
      for (String describingTerm : terms) {
        /* A concept does not describe itself */
        if (concept.equals(describingTerm))
          continue;

        /* Retrieve single and mutual frequencies */
        double ft = frequencies.getFrequency(describingTerm);
        double fct = frequencies.getFrequency(concept, describingTerm);

        /*
         * Sets the membership of term to concept to the value of the correlation
         * function
         */
        contextVectors.setMembership(describingTerm, concept, corrFunc.calculate(fc, ft, fct));
      }
    }

    /* Finalizes the context vectors calculation by normalizing values and discarding
     * non relevant concepts */
    contextVectors.finalize(alpha);

    return contextVectors;
  }

  /**
   * Creates a taxonomy, a graph of relations between concepts.
   * 
   * @param contextVectors Context vectors needed for the construction of the taxonomy.
   * @param lambda         threshold to filter out noisy relations. Interesting relations are at least lambda.
   * @return a {@link Taxonomy<String>} object containing the relations between concepts.
   */
  public static Taxonomy<String> createTaxonomy(ContextVectors<String> contextVectors, double lambda) {
    /* Get the list of concepts */
    List<String> concepts = contextVectors.getConcepts();
        
    /* Output object */
    Taxonomy<String> taxonomy = new Taxonomy<String>(concepts);

    /* For each pair of concepts */
    for (String subconcept : concepts) {
      for (String concept : concepts) {
        /* compute specificity and discards values less than lambda */
        double spec = computeSpecificity(contextVectors, subconcept, concept);
        if (spec >= lambda) {
          taxonomy.setSpecificity(subconcept, concept, spec);
        }
      }
    }

    /* Simplify the taxonomy by removing redundant relations */
    taxonomy.taxonomyPruning();

    return taxonomy;
  }


  /**
   * Saves the given {@link Taxonomy} object as a OWL2 ontology
   * @param taxonomy object containing the taxonomy information
   * @param output path in which the OWL ontology must be created
   */
  public static void saveOWL2FuzzyOntology(Taxonomy<String> taxonomy, Path output) {

    /* OWL management objects */
    OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
    OWLDataFactory factory = ontologyManager.getOWLDataFactory();
    
    try {
      /* Create the ontology */
      IRI ontologyIRI = IRI.create("http://www.semanticweb.org/ontologies/concepts");  
      OWLOntology ontology = ontologyManager.createOntology(ontologyIRI);

      /* Create an OWL Class for each concept */
      List<String> concepts = taxonomy.getConcepts();
      Map<String, OWLClass> owlClasses = new HashMap<>();
      for (String concept : concepts) {
        IRI iriClass = IRI.create(ontologyIRI + "#" + concept);
        OWLClass cl = factory.getOWLClass(iriClass);
        owlClasses.put(concept, cl);
      }

      /* For each pair of distinct concepts */
      for (String concept: concepts) {
        for (String subConcept: concepts) {
          if (concept.equals(subConcept)) continue;

          /* If the specificity is positive, add a dedicated axiom 
           * containing the OWL annotation to the ontology */
          double specificity = taxonomy.getSpecificity(subConcept, concept);
          if (specificity > 0) {
            OWLAnnotation fuzzyAnnotation = createFuzzyAnnotation(factory, specificity);

            OWLAxiom axiom = factory.getOWLSubClassOfAxiom(owlClasses.get(subConcept), 
                                                           owlClasses.get(concept), 
                                                           Set.of(fuzzyAnnotation));

            AddAxiom add = new AddAxiom(ontology, axiom);
            ontologyManager.applyChange(add);
          }
        }
      }

      /* Write ontology on file */
      ontologyManager.saveOntology(ontology, new FileOutputStream(output.toString()));
    } catch (OWLOntologyCreationException e) {
      e.printStackTrace();
    } catch (OWLOntologyStorageException e) {
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Prepares the tagged sentence for processing by,
   * 1) Removing stop words
   * 2) Perform word stemming
   * @param taggedSentence the POS tagged sentence to clean
   * @return A list of tagged word stems without stop words
   */
  private static List<TaggedWord> prepareSentence(List<TaggedWord> taggedSentence) {
    /* output list */
    List<TaggedWord> preparedSentence = new ArrayList<TaggedWord>();
    
    for (TaggedWord taggedWord : taggedSentence) {
      /* Discard stop words and punctuation */
      if (StopWords.isStopWord(taggedWord.word()) || !taggedWord.word().matches("\\w+")) continue;
      
      /* Perform word stemming and transform the word to lower-case */
      taggedWord.setWord(Morphology.lemmaStatic(taggedWord.word(), taggedWord.tag(), true));
      
      /* Store word */
      preparedSentence.add(taggedWord);
    }
    
    return preparedSentence;
  }
  
  /**
   * Computes the specificity of a concept to another. This is, the degree to which concept A is a sub-concept
   * of concept B.
   * 
   * @param contextVectors {@link ContextVectors<String>} object containing the terms which describes the two concepts
   * @param subconcept candidate sub-concept
   * @param concept candidate concept
   * @return a value in range [0, 1] describing how much subconcept is actually a sub-concept of concept.
   */
  private static double computeSpecificity(ContextVectors<String> contextVectors, String subconcept, String concept) {
    /* A concept has specificity 1 to itself */
    if (subconcept.equals(concept)) return 1;
    
    /* Initialize numerator and denominator of the specificity formula */
    double num = 0;
    double denom = 0;

    /* For each term */
    for (String term : contextVectors.getTerms()) {
      /* Retrieve the membership of the term to subconcept and concept */
      double membership_subconcept = contextVectors.getMembership(subconcept, term);
      double membership_concept = contextVectors.getMembership(concept, term);

      /* Add to the numerator the smallest one */
      num += Math.min(membership_subconcept, membership_concept);

      /* Add to the denominator the membership to the subconcept */
      denom += membership_subconcept;
    }

    /* Return the specificity. Assume 1 if the denominator is zero */
    return denom > 0 ? num / denom : 1;
  }

  /**
   * Creates a FuzzyOwl2 annotation for the given value
   * @param factory {@link OWLDataFactory} to create the annotation
   * @param value fuzzy value to embed into the annotation
   * @return a {@link OWLAnnotation} object representing the fuzzy value.
   */
  private static OWLAnnotation createFuzzyAnnotation(OWLDataFactory factory, double value) {
    OWLAnnotationProperty fuzzyTag = factory.getOWLAnnotationProperty(IRI.create("#fuzzyLabel"));
    String fuzzyowl2 = "<fuzzyOwl2 fuzzyType=\"axiom\">\n\t<Degree value=\"" + value + "\"/></fuzzyOwl2>";
    OWLLiteral fuzzyLiteral = factory.getOWLLiteral(fuzzyowl2);
    return factory.getOWLAnnotation(fuzzyTag, fuzzyLiteral);
  }
}
