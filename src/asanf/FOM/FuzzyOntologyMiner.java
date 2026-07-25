package asanf.FOM;

import asanf.FOM.Util.BMI;
import asanf.FOM.Util.CorrelationFunction;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.tagger.common.Tagger;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import nars.$;
import nars.term.Term;
import nars.truth.Truth;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.util.*;

import static edu.stanford.nlp.tagger.maxent.MaxentTagger.DEFAULT_NLP_GROUP_MODEL_PATH;


public class FuzzyOntologyMiner {

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        TermFrequencies<String> tf = new TermFrequencies<String>();
        String[] tags = new String[2];
        tags[0] = "NN";
        //tags[1] = "NN";
        int windowSize = 10;
        // vecchi reuters 0.01 / 0.03
        double lower = 0.03;
        double upper = 0.55;
        double alpha = 0.02;
        double beta = 0.55;
        double lambda = 0.022;

        String path = "/tmp/u/silent_weapons_quiet_wars.txt";
        String domain = "quietwars";

        tf = FuzzyOntologyMiner.extractFrequencies(path, windowSize, tags);

        //System.out.println("totWin: " + tf.getTotWindows());

        tf.filterTerms(lower, upper);

        //for(String e: tf){
        //System.out.println(e + ": " + tf.getFrequency(e));
        //}
        Collection<String> concepts = tf.getTerms();
        Term[] conceptTerms = concepts.stream().map(x -> $.quote(x)).toArray(i->new Term[i]);
        System.out.println( $.inh( $.sete(conceptTerms), $.the(domain) ) + "." );

        for(String i: tf){
            for(String j: tf){
                if(!i.equals(j)){
                    double f = tf.getFrequency(i, j);
                    if( f > 0 ) {
                        correlation(i, j, f);
                    }
                }
            }
        }

        ContextVectors<String> cv;
        CorrelationFunction cf = new BMI(beta);
        cv = FuzzyOntologyMiner.createContextVectors(tf, cf, alpha);

        Collection<String> terms = tf.getTerms();

        //System.out.println(concepts.size() != terms.size());


        for(String concept: concepts)
            for(String term: terms)
                if(!concept.equals(term)){
                    double mem = cv.getMembership(concept, term);
                    inh(term, concept, mem);
                }

        Taxonomy<String> taxonomy;


        taxonomy = FuzzyOntologyMiner.createTaxonomy(cv, lambda);
        taxonomy.taxonomyPruning();

        for(String c1: concepts)
            for(String c2: concepts){
                if (c1.equals(c2)) continue;
                double spec = taxonomy.getSpecificity(c1, c2);
                if(spec > 0) {
                    sim(c1, c2, spec);
                }
            }

    }

    private static void inh(String term, String concept, double mem) {
        Truth t = t(mem);
        if (t!=null)
            System.out.println("(" + q(term) + "-->" + q(concept) + "). " + t);
    }
    private static void sim(String term, String concept, double spec) {
        Truth t = t(spec);
        if (t!=null)
            System.out.println("(" + q(term) + "<->" + q(concept) + "). " + t);
    }

    static void correlation(String i, String j, double f) {
        Truth t = t(f);
        if (t!=null)
            System.out.println("(" + q(i) + "&" + q(j) + "). " + t);
    }

    private static String q(String j) {
        return "\"" + j + "\"";
    }

    private static Truth t(double f) {
        return $.t(1f, (float)f);
    }



    private static final int MIN_WORD_LENGTH = 2;

    /**
     * Metodo che restituisce la matrice di frequenze dei termini nella collezione di documenti
     *
     * @param path          Path della cartella contenente i documenti da cui estrarre l'ontologia
     * @param lower         Soglia minima di frequenza, al di sotto della quale bisogna scartare i termini
     * @param upper         Soglia massima di frequenza, al di sopra della quale bisogna scartare i termini
     * @param windowSize    Numero di termini di cui deve essere termosta la finestra scorrevole
     * @param searchPattern Array di Tag indicanti il pattern di termini su cui ci si vuole focalizzare:
     *                      non vi è controllo sulla correttezza di questi tag, si da per scontato che siano correttamente inseriti
     *                      e siano nella forma "NN","NNS","ADJ"
     * @return Un oggetto di tipo TermFrequencies<String> contenente le frequenze mutue e assolute dei termini.
     * @throws IllegalArgumentException Se il numero di pattern è maggiore della grandezza della finestra scorrevole,
     *                                  o se il path non è una directory.
     * @throws ClassNotFoundException
     * @throws IOException
     */

    public static TermFrequencies<String> extractFrequencies(String path, int windowSize, String[] searchPattern) throws FileNotFoundException {
        return extractFrequencies(new BufferedReader(new FileReader(path)), windowSize, searchPattern);
    }

    public static TermFrequencies<String> extractFrequencies(
            Reader input, int windowSize, String[] searchPattern)
            throws IllegalArgumentException {

        //http://www-nlp.stanford.edu/nlp/javadoc/javanlp/edu/stanford/nlp/tagger/maxent/MaxentTagger.html
        MaxentTagger tagger = new MaxentTagger("/tmp/t/english-left3words-distsim.tagger");
        //tagger = new MaxentTagger();
        //MaxentTagger tagger = new MaxentTagger("edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");

        if (searchPattern.length > windowSize)
            throw new IllegalArgumentException("Il numero di tag non può essere maggiore della grandezza della finestra.");

        TermFrequencies<String> freq = new TermFrequencies<String>();
        File dir = null;
        File[] listOfFiles;


        StopWords sw = new StopWords();


        List<List<HasWord>> sentences = null;
        List<TaggedWord> tSentence = new ArrayList<TaggedWord>();
        Morphology stemmer = new Morphology();
        TaggedWord curr;
        String term;


        sentences = MaxentTagger.tokenizeText(input);

				
				/*
				 * Lavoro su ogni frase separatamente
				 */
        for (List<HasWord> sentence : sentences) {
            // si effettua il POS tagging sulla frase

            tSentence = tagger.tagSentence(sentence);
					
					/* filtered sentence conterrà parole taggate escluse stop words
					 * e punteggiatura su cui è già stato effettuato lo stemming
					 */
            ArrayList<TaggedWord> filteredSentence = new ArrayList<TaggedWord>();

            // dalla frase taggata bisogna eliminare stop words ed effettuare lo stemming
            for (TaggedWord word : tSentence) {
						
						/* la parola viene portata in lowercase ed è soggetta a stemming.
						 * La parola ottenuta viene aggiunta ai termini filtrati solo se non è una stopword
						 * e non è un segno di punteggiatura (ovvero rispetta la regexp [a-zA-Z0-9]+)
						 */
                if (word.word().length() < MIN_WORD_LENGTH)
                    continue;

                word.setWord(word.word().toLowerCase());
                word.setWord(stemmer.lemma(word.word(), word.tag()));
                if (!sw.isStopWord(word.word()) && word.word().matches("[a-zA-Z]+")) { //\\w+")){
                    filteredSentence.add(word);
                }
            }
					
					/*
					 * Per ogni finestra di testo di largheza windowSize, bisogna controllare
					 * se i tag delle parole rispettano il pattern di ricerca specificato come
					 * parametro della classe, nel qual caso si memorizzano le relative frequenze.
					 */
            for (int i = 0; i < filteredSentence.size() - windowSize + 1; i++) {

                // Array contenente i termini già visti in questa finestra
                Set<String> alreadySeen = new HashSet<String>();
                freq.augmentWindows();

                // Per ogni termine nella finestra
                for (int j = i; j < i + windowSize; j++) {


                    if ((j + searchPattern.length) < filteredSentence.size()) {

                        term = "";

                        // per ogni tag nel pattern di ricerca
                        for (int k = 0; k < searchPattern.length; k++) {

                            // estraggo il termine j+k
                            curr = filteredSentence.get(j + k);

                            // se il tag non corrisponde a quello cercato
                            if (!curr.tag().equalsIgnoreCase(searchPattern[k])) {
										
										/* Allora il termine j-esimo non è il primo del pattern 
										 * cercato, quindi è un termine che devo considerare da solo
										 */
                                term = filteredSentence.get(j).word();
                                break;
                            }

                            // se invece il j+k-esimo termine rispetta il tag lo aggiungo a term
                            term += (curr.word() + " ");
									
									/* se sono all'ultima iterazione e term contiene il termine che
									 * rispetta il searchPattern, allora devo spostarmi in avanti di
									 * k termini all'interno della filteredSentence.  
									 */
                            if (k + 1 == searchPattern.length)
                                j += k;
                        }
                    } else {
                        term = filteredSentence.get(j).word();
                    }
							
							/* Se il termine non è mai stato visto
							 *  in questa finestra
							 */
                    if (!alreadySeen.contains(term)) {

                        // Aggiungo un'occorrenza del termine
                        freq.addOccurrence(term);
								
								/*
								 * Aggiungo le occorrenze del termine attuale
								 * con tutti quelli già visti
								 */
                        for (String w : alreadySeen) {
                            freq.addOccurrence(term, w);
                        }

                        // Aggiungo il termine alla lista dei già visti
                        alreadySeen.add(term);
                    }
                }
            }
        }

        // calcolo le frequenze dividendo per il numero di finestre
        freq.computeFrequencies();
        //freq.filterTerms(lower, upper);
        return freq;
    }


    /**
     * Metodo che crea i vettori di concetto per ogni termine
     *
     * @param frequencies Matrice contenente le frequenze di tutti i termini
     * @param corrFunc    Funzione di correlazione da utilizzare
     * @param alpha       Valore soglia per alpha-cut
     * @return un ContextVectors, oggetto contenente tutti i vettori di contesto
     */
    public static ContextVectors<String> createContextVectors(
            TermFrequencies<String> frequencies, CorrelationFunction corrFunc, double alpha) {
        int count;
        double fc, ft, fct, membership;
        Collection<String> terms = frequencies.getTerms();
        ContextVectors<String> contextVectors = new ContextVectors<String>(terms);


        // Per ogni candidato concetto
        for (String concept : terms) {
            count = 0;
            // prendo la frequenza del concetto candidato
            fc = frequencies.getFrequency(concept);

            // Per ogni termine
            for (String term : terms) {
                if (concept.equals(term)) continue;
				/* Recupero la frequenza del termine, e
				 * la frequenza mutua (termine, concetto) 
				 */

                ft = frequencies.getFrequency(term);
                fct = frequencies.getFrequency(concept, term);

                // calcolo la membership, appartenenza di term a concept
                membership = corrFunc.calculateCorrelation(fc, ft, fct);

                contextVectors.setMembership(term, concept, membership);
            }
			/* se tutti i termini hanno membership minore della soglia
			 * allora concept non è un concetto abbastanza importante
			 * da essere considerato: conservo quest'informazione 
			 * ponendo la membership di concept a se stesso a -1
			 */


        }
        contextVectors.normalizeMemberships();

        for (String concept : terms) {
            count = 0;
            for (String term : terms) {
                if (contextVectors.getMembership(concept, term) < alpha) count++;
            }

            if (count == terms.size() - 1)
                contextVectors.deleteConcept(concept);
        }

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
        ArrayList<String> concepts = contextVectors.getConcepts();

        // Lista di termini
        Collection<String> terms = contextVectors.getTerms();

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
                        //num += ((membership_1 * membership_2)/(membership_1 + membership_2 - membership_1*membership_2));

                        // Aggiungo la membership in c1 al denominatore
                        denom += membership_1;
                    }

                    // calcolo la specificità
                    spec = num / denom;

                    // se la specificità è maggiore della soglia
                    if (spec > lambda)
                        // la memorizzo
                        taxonomy.setSpecificity(c1, c2, spec);
                }
            }
        }
        taxonomy.taxonomyPruning();

        return taxonomy;
    }

    /**
     * Metodo che, a partire dal vettore dei concetti e dalle tassonomie, crea l'ontologia
     *
     * @throws OWLOntologyCreationException
     * @throws OWLOntologyStorageException
     * @throws FileNotFoundException
     **/
    public void saveOWL2FuzzyOntology(Taxonomy tax, String fileName) throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException {
        int i, j;
        OWLClass s, c;
        //iteratore dei concetti
        ArrayList<String> concetti = tax.getConcepts();

        //gestore dell'ontologia
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        //iri di riferimento per l'ontologia
        IRI ontIRI = IRI.create("http://www.semanticweb.org/ontologies/concetti");
        //crea ontologia
        OWLOntology ont = m.createOntology(ontIRI);
        //gestore interfaccia per l'ontologia
        OWLDataFactory factory = m.getOWLDataFactory();

        //Per ogni concetto crea una classe nell'ontologia
        Map<String, OWLClass> classi = new HashMap<String, OWLClass>();
        for (String con : concetti) {

            IRI iriClass = IRI.create(ontIRI + "#" + con);
            OWLClass cl = factory.getOWLClass(iriClass);
            classi.put(con, cl);
        }

        for (i = 0; i < concetti.size(); i++) {
            for (j = 0; j < concetti.size(); j++) {
                String sub = concetti.get(j);
                String cls = concetti.get(i);
                double spec = tax.getSpecificity(sub, cls);
                if (spec > 0) {
                    OWLAnnotation fuzzyAnnotation = createFuzzyAnnotation(factory, spec);

                    Set<OWLAnnotation> annotazioni = new HashSet<OWLAnnotation>();
                    annotazioni.add(fuzzyAnnotation);

                    s = classi.get(sub);
                    c = classi.get(cls);
                    OWLAxiom axiom = factory.getOWLSubClassOfAxiom(s, c, annotazioni);

                    AddAxiom add = new AddAxiom(ont, axiom);
                    m.applyChange(add);
                }
            }
        }

        //salvo l'ontologia su file
        m.saveOntology(ont, new FileOutputStream(fileName));

    }

    public static OWLAnnotation createFuzzyAnnotation(OWLDataFactory f, double value) {
        OWLAnnotationProperty fuzzyTag = f.getOWLAnnotationProperty(IRI.create("#fuzzyLabel"));
        String fuzzyowl2 = "<fuzzyOwl2 fuzzyType=\"axiom\">\n\t<Degree value=\"" + value + "\"/></fuzzyOwl2>";
        OWLLiteral fuzzyLiteral = f.getOWLLiteral(fuzzyowl2);
        return f.getOWLAnnotation(fuzzyTag, fuzzyLiteral);
    }

}
