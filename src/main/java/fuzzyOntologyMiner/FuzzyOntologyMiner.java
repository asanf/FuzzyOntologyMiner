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
import java.util.HashSet;
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

  public static void mine(IMiningParams params) throws ClassNotFoundException, IOException, OWLOntologyCreationException, OWLOntologyStorageException {
    TermFrequencies<String> termFrequencies = extractFrequencies(params);
    ContextVectors<String> contextVectors = createContextVectors(termFrequencies, 
                                                                 params.getCorrelationFunction(),
                                                                 params.getConceptFilteringThreshold());
    Taxonomy<String> taxonomy = createTaxonomy(contextVectors, params.getConceptRelationFilterThreshold());
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
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public static TermFrequencies<String> extractFrequencies(IMiningParams params)
      throws ClassNotFoundException, IOException {

    File dir = params.getInputDocumentsLocation().toFile();

    TermFrequencies<String> freq = new TermFrequencies<String>();
    MaxentTagger tagger = new MaxentTagger(params.getTaggerModel());
    File[] listOfFiles = dir.listFiles();
    ITermsExtractor termsExtractor = new NaiveTermsExtractor();
    termsExtractor.compile(params.getMiningPatterns());
    int windowSize = params.getWindowSize();

    for (File file : listOfFiles) {
      if (file.isFile()) {
        // TODO generalize document read (pdf, text, word, ecc.)
        List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new BufferedReader(new FileReader(file)));

        /* For each sentence */
        for (List<HasWord> sentence : sentences) {
          /*
           * filtered sentence conterrà parole taggate escluse stop words e punteggiatura
           * su cui è già stato effettuato lo stemming
           */
          List<TaggedWord> preparedSentence = prepareSentence(tagger, sentence);

          /*
           * Per ogni finestra di testo di largheza windowSize, bisogna controllare se i
           * tag delle parole rispettano il pattern di ricerca specificato come parametro
           * della classe, nel qual caso si memorizzano le relative frequenze.
           */

          int numWindows = preparedSentence.size() - windowSize + 1;
          for (int windowIdx = 0; windowIdx < numWindows; windowIdx++) {
            // TODO what if windowSize > sentence.length()?
            // Array contenente i termini già visti in questa finestra
            TreeSet<String> termsInWindow = new TreeSet<>();
            freq.augmentWindows();
            termsInWindow.addAll(termsExtractor.extract(preparedSentence.subList(windowIdx, windowIdx + windowSize)));

            for (String term : termsInWindow) {
              freq.addOccurrence(term);
              for (String otherTerm : termsInWindow.tailSet(term, false)) {
                freq.addOccurrence(term, otherTerm);
              }
            }

          } // Windows loop end
        }
      }
    }

    // calcolo le frequenze dividendo per il numero di finestre
    freq.computeFrequencies();
    freq.filterTerms(params.getMinAllowedTermFrequency(), params.getMaxAllowedTermFrequency());
    return freq;
  }

  private static List<TaggedWord> prepareSentence(MaxentTagger tagger, List<HasWord> sentence) {
    List<TaggedWord> preparedSentence = new ArrayList<TaggedWord>();

    // si effettua il POS tagging sulla frase
    List<TaggedWord> taggedSentence = tagger.tagSentence(sentence);

    // dalla frase taggata bisogna eliminare stop words ed effettuare lo stemming
    for (TaggedWord taggedWord : taggedSentence) {

      /*
       * la parola viene portata in lowercase ed è soggetta a stemming. La parola
       * ottenuta viene aggiunta ai termini filtrati solo se non è una stopword e non
       * è un segno di punteggiatura (ovvero rispetta la regexp [a-zA-Z0-9]+)
       */

      taggedWord.setWord(Morphology.lemmaStatic(taggedWord.word(), taggedWord.tag(), true));
      if (!StopWords.isStopWord(taggedWord.word()) && taggedWord.word().matches("[a-zA-Z]+")) { // \\w+")){
        preparedSentence.add(taggedWord);
      }
    }
    return preparedSentence;
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

    contextVectors.finalize(alpha);

    return contextVectors;
  }

  /**
   * Metodo che costruisce una tassonomia
   * 
   * @param contextVectors Vettori di contesto
   * @param lambda         soglia minima per la relazione fra concetti
   * @return Un oggetto di tipo Taxonomy<String>
   */
  public static Taxonomy<String> createTaxonomy(ContextVectors<String> contextVectors, double lambda) {

    // Lista di concetti
    List<String> concepts = contextVectors.getConcepts();

    // Lista di termini
    List<String> terms = contextVectors.getTerms();

    // Oggetto tassonomia che verrà restitutito
    Taxonomy<String> taxonomy = new Taxonomy<String>(concepts);

    // numeratore e denominatore della formula Spec(ci, cj)
    double num = 0, denom = 0;

    // variabili che conterranno la membership di un termine ai due concetti
    double membership_1, membership_2;

    // specificità di ci rispetto a cj
    double spec;

    // Per ogni coppia di concetti
    for (String c1 : concepts) {
      for (String c2 : concepts) {

        // se i concetti sono uguali, allora la specificità è 1
        if (c1.equals(c2))
          taxonomy.setSpecificity(c1, c2, 1);
        else {

          // Inizializzo numeratore e denominatore
          num = 0;
          denom = 0;

          // per ogni termine
          for (String term : terms) {
            // Calcolo la membership rispetto a c1 e c2
            membership_1 = contextVectors.getMembership(c1, term);
            membership_2 = contextVectors.getMembership(c2, term);

            // Aggiungo la più piccola delle due al numeratore
            num += Math.min(membership_1, membership_2);
            // num += ((membership_1 * membership_2)/(membership_1 + membership_2 -
            // membership_1*membership_2));

            // Aggiungo la membership in c1 al denominatore
            denom += membership_1;
          }

          // calcolo la specificità
          spec = num / denom;

          // se la specificità è maggiore della soglia
          if (spec > lambda) {
            // la memorizzo
            taxonomy.setSpecificity(c1, c2, spec);
          }
        }
      }
    }

    taxonomy.taxonomyPruning();

    return taxonomy;
  }

  /**
   * Metodo che, a partire dal vettore dei concetti e dalle tassonomie, crea
   * l'ontologia
   * 
   * @throws OWLOntologyCreationException
   * @throws OWLOntologyStorageException
   * @throws FileNotFoundException
   **/
  public static void saveOWL2FuzzyOntology(Taxonomy<String> tax, Path output)
      throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {

    // gestore dell'ontologia
    OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
    // iri di riferimento per l'ontologia
    IRI ontologyIRI = IRI.create("http://www.semanticweb.org/ontologies/concepts");
    // crea ontologia
    OWLOntology ontology = ontologyManager.createOntology(ontologyIRI);
    // gestore interfaccia per l'ontologia
    OWLDataFactory factory = ontologyManager.getOWLDataFactory();

    // Per ogni concetto crea una classe nell'ontologia
    Map<String, OWLClass> owlClasses = new HashMap<String, OWLClass>();
    List<String> concepts = tax.getConcepts();
    for (String concept : concepts) {
      IRI iriClass = IRI.create(ontologyIRI + "#" + concept);
      OWLClass cl = factory.getOWLClass(iriClass);
      owlClasses.put(concept, cl);
    }
    
    int i, j;
    OWLClass s, c;
    for (i = 0; i < concepts.size(); i++) {
      for (j = 0; j < concepts.size(); j++) {
        String sub = concepts.get(j);
        String cls = concepts.get(i);
        double spec = tax.getSpecificity(sub, cls);
        if (spec > 0) {
          OWLAnnotation fuzzyAnnotation = createFuzzyAnnotation(factory, spec);

          Set<OWLAnnotation> annotations = new HashSet<OWLAnnotation>();
          annotations.add(fuzzyAnnotation);

          s = owlClasses.get(sub);
          c = owlClasses.get(cls);
          OWLAxiom axiom = factory.getOWLSubClassOfAxiom(s, c, annotations);

          AddAxiom add = new AddAxiom(ontology, axiom);
          ontologyManager.applyChange(add);
        }
      }
    }

    // salvo l'ontologia su file
    ontologyManager.saveOntology(ontology, new FileOutputStream(output.toString()));
  }

  public static OWLAnnotation createFuzzyAnnotation(OWLDataFactory f, double value) {
    OWLAnnotationProperty fuzzyTag = f.getOWLAnnotationProperty(IRI.create("#fuzzyLabel"));
    String fuzzyowl2 = "<fuzzyOwl2 fuzzyType=\"axiom\">\n\t<Degree value=\"" + value + "\"/></fuzzyOwl2>";
    OWLLiteral fuzzyLiteral = f.getOWLLiteral(fuzzyowl2);
    return f.getOWLAnnotation(fuzzyTag, fuzzyLiteral);
  }

}
