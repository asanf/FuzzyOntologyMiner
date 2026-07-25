package fuzzyOntologyMiner;

import java.io.File;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import fuzzyOntologyMiner.correlationFunctions.ICorrelationFunction;
import fuzzyOntologyMiner.database.DbManager;
import fuzzyOntologyMiner.domain.Taxonomy;
import fuzzyOntologyMiner.frequencies.ITermsExtractor;
import fuzzyOntologyMiner.frequencies.NaiveTermsExtractor;
import fuzzyOntologyMiner.frequencies.StopWords;

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
import java.util.stream.IntStream;

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
    try (DbManager database = new DbManager("mining.db")) {
      extractFrequencies(params, database);

      /* Create context vectors */
      createContextVectors(database, params.getCorrelationFunction(), params.getConceptFilteringThreshold());

      /* Create a taxonomy */
      Taxonomy<String> taxonomy = createTaxonomy(database, params.getConceptRelationFilterThreshold());

      /* Save it as an OWL ontology */
      saveOWL2FuzzyOntology(taxonomy, params.getOutputOntologyLocation());
    }
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
  public static void extractFrequencies(IMiningParams params, DbManager database) {

    try {
      /*
       * POS Tagger
       * TODO tagger loading could be done in a singleton
       */
      MaxentTagger tagger = new MaxentTagger(params.getTaggerModel());

      /*
       * Object to extract terms of interest from sentences based on POS Tag patterns
       */
      ITermsExtractor termsExtractor = new NaiveTermsExtractor();
      termsExtractor.compile(params.getMiningPatterns());
      int windowSize = params.getWindowSize();

      /*
       * Document corpus navigation
       * TODO: this navigation is not recursive
       */
      File dir = params.getInputDocumentsLocation().toFile();
      File[] listOfFiles = dir.listFiles();
      for (File file : listOfFiles) {
        /* Examines files only */
        if (!file.isFile())
          continue;

        List<List<HasWord>> sentences;
        // TODO generalize document read (pdf, text, word, ecc.)
        sentences = MaxentTagger.tokenizeText(new BufferedReader(new FileReader(file)));

        /* For each sentence */
        for (List<HasWord> sentence : sentences) {
          /* prepare the sentence for processing */
          List<TaggedWord> preparedSentence = prepareSentence(tagger.tagSentence(sentence));

          /* For each text window */
          int numWindows = preparedSentence.size() - windowSize + 1;
          for (int windowIdx = 0; windowIdx < numWindows; windowIdx++) {
            database.newObservation();
            List<TaggedWord> textWindow = preparedSentence.subList(windowIdx, windowIdx + windowSize);
            TreeSet<String> termsInWindow = new TreeSet<>();
            termsInWindow.addAll(termsExtractor.extract(textWindow));

            for (String term : termsInWindow) {
              database.addOccurrence(term);
              for (String otherTerm : termsInWindow.tailSet(term, false)) {
                database.addOccurrence(term, otherTerm);
              }
            }
          }
        }
      }
      database.filterTermsByFrequency(params.getMinAllowedTermFrequency(), params.getMaxAllowedTermFrequency());
    } catch (IOException | ClassNotFoundException exception) {
      exception.printStackTrace();
    }
  }

  /**
   * Computes the {@link ContextVectors} by using a given
   * {@link ICorrelationFunction}.
   * 
   * @param database Object containing single and mutual frequencies of terms
   * @param corrFunc Correlation function to use
   * @param alpha    Threshold value in [0, 1] to discard non relevant concepts
   * 
   */
  public static void createContextVectors(DbManager database, ICorrelationFunction corrFunc, double alpha) {

    List<String> terms = database.getTerms();

    IntStream.range(0, terms.size())
        .mapToObj(i -> IntStream.range(i + 1, terms.size()).mapToObj(j -> new String[] { terms.get(i), terms.get(j) }))
        .flatMap(s -> s)
        .forEach(pair -> {
          double ft = database.getFrequency(pair[0]);
          double fc = database.getFrequency(pair[1]);
          double ftc = database.getFrequency(pair[0], pair[1]);
          database.setMembership(pair[0], pair[1], corrFunc.calculate(fc, ft, ftc));
        });

    database.normalizeMemberships();

    /* Concept pruning */
    for (String concept : terms) {
      boolean significant = terms.stream()
          .filter(term -> !term.equals(concept))
          .anyMatch(term -> {
            return (database.getMembership(term, concept) >= alpha);
          });

      if (!significant) {
        database.deleteConcept(concept);
      }
    }
  }

  /**
   * Creates a taxonomy, a graph of relations between concepts.
   * 
   * @param contextVectors Context vectors needed for the construction of the
   *                       taxonomy.
   * @param lambda         threshold to filter out noisy relations. Interesting
   *                       relations are at least lambda.
   * @return a {@link Taxonomy<String>} object containing the relations between
   *         concepts.
   */
  public static Taxonomy<String> createTaxonomy(DbManager database, double lambda) {
    /* Get the list of concepts */
    List<String> concepts = database.getConcepts();

    /* Output object */
    Taxonomy<String> taxonomy = new Taxonomy<String>(concepts);

    /* Fetch all specificities >= lambda from the database */
    List<Object[]> specificities = database.getSpecificities(lambda);
    for (Object[] spec : specificities) {
      taxonomy.setSpecificity((String) spec[0], (String) spec[1], (Double) spec[2]);
    }

    /* Simplify the taxonomy by removing redundant relations */
    taxonomy.taxonomyPruning();

    return taxonomy;
  }

  /**
   * Saves the given {@link Taxonomy} object as a OWL2 ontology
   * 
   * @param taxonomy object containing the taxonomy information
   * @param output   path in which the OWL ontology must be created
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
      for (String concept : concepts) {
        for (String subConcept : concepts) {
          if (concept.equals(subConcept))
            continue;

          /*
           * If the specificity is positive, add a dedicated axiom
           * containing the OWL annotation to the ontology
           */
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
   * 
   * @param taggedSentence the POS tagged sentence to clean
   * @return A list of tagged word stems without stop words
   */
  private static List<TaggedWord> prepareSentence(List<TaggedWord> taggedSentence) {
    /* output list */
    List<TaggedWord> preparedSentence = new ArrayList<TaggedWord>();

    for (TaggedWord taggedWord : taggedSentence) {

      /* Perform word stemming and transform the word to lower-case */
      taggedWord.setWord(Morphology.lemmaStatic(taggedWord.word(), taggedWord.tag(), true));

      /* Discard stop words and punctuation */
      if (!StopWords.isStopWord(taggedWord.word()) && taggedWord.word().matches("\\w+")) {
        /* Store word */
        preparedSentence.add(taggedWord);
      }
    }

    return preparedSentence;
  }



  /**
   * Creates a FuzzyOwl2 annotation for the given value
   * 
   * @param factory {@link OWLDataFactory} to create the annotation
   * @param value   fuzzy value to embed into the annotation
   * @return a {@link OWLAnnotation} object representing the fuzzy value.
   */
  private static OWLAnnotation createFuzzyAnnotation(OWLDataFactory factory, double value) {
    OWLAnnotationProperty fuzzyTag = factory.getOWLAnnotationProperty(IRI.create("#fuzzyLabel"));
    String fuzzyowl2 = "<fuzzyOwl2 fuzzyType=\"axiom\">\n\t<Degree value=\"" + value + "\"/></fuzzyOwl2>";
    OWLLiteral fuzzyLiteral = factory.getOWLLiteral(fuzzyowl2);
    return factory.getOWLAnnotation(fuzzyTag, fuzzyLiteral);
  }
}
