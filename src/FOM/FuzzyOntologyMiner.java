package FOM;

import java.io.File;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import FOM.StopWords;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.ling.WordTag;

import CFM.CorrelationFunction;

public class FuzzyOntologyMiner {
	
	/**
	 * Metodo che restituisce la matrice di frequenze dei termini nella collezione di documenti
	 * @param path Path della cartella contenente i documenti da cui estrarre l'ontologia
	 * @param lower Soglia minima di frequenza, al di sotto della quale bisogna scartare i termini
	 * @param upper Soglia massima di frequenza, al di sopra della quale bisogna scartare i termini
	 * @param windowSize Numero di termini di cui deve essere termosta la finestra scorrevole
	 * @param searchPattern Array di Tag indicanti il pattern di termini su cui ci si vuole focalizzare:
	 * 						non vi è controllo sulla correttezza di questi tag, si da per scontato che siano correttamente inseriti
	 * 						e siano nella forma "NN","NNS","ADJ"
	 * @return Un oggetto di tipo TermFrequencies<String> contenente le frequenze mutue e assolute dei termini.
	 * @throws IllegalArgumentException Se il numero di pattern è maggiore della grandezza della finestra scorrevole,
	 * 									o se il path non è una directory.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static TermFrequencies<String> extractFrequencies(
			String path, int windowSize, String[] searchPattern) 
					throws IllegalArgumentException, ClassNotFoundException, IOException{
		
		
		if( searchPattern.length > windowSize)
			throw new IllegalArgumentException("Il numero di tag non può essere maggiore della grandezza della finestra.");
		
		TermFrequencies<String> freq = new TermFrequencies<String>();
		File dir = null;
		File[] listOfFiles;
		
		dir = new File(path);
		
		if(!dir.isDirectory())
			throw new IllegalArgumentException(path + " is not a directory.");
		
		StopWords sw = new StopWords();
		
		listOfFiles = dir.listFiles();
		
		for(File file: listOfFiles){
			if(file.isFile()){
				MaxentTagger tagger = null;	
				List<List<HasWord>> sentences = null;
				ArrayList<TaggedWord> tSentence = new ArrayList<TaggedWord>();
				Morphology stemmer = new Morphology();
				TaggedWord curr;
				String term;
				
				tagger = new MaxentTagger("left3words-wsj-0-18.tagger");
				
				sentences = MaxentTagger.tokenizeText(new BufferedReader(new FileReader(file)));
				
				
				/*
				 * Lavoro su ogni frase separatamente
				 */
				for(List<HasWord> sentence: sentences){
					// si effettua il POS tagging sulla frase
					tSentence = tagger.tagSentence(sentence);
					
					/* filtered sentence conterrà parole taggate escluse stop words
					 * e punteggiatura su cui è già stato effettuato lo stemming
					 */
					ArrayList<TaggedWord> filteredSentence = new ArrayList<TaggedWord>();
					
					// dalla frase taggata bisogna eliminare stop words ed effettuare lo stemming
					for(TaggedWord word: tSentence){
						
						/* la parola viene portata in lowercase ed è soggetta a stemming.
						 * La parola ottenuta viene aggiunta ai termini filtrati solo se non è una stopword
						 * e non è un segno di punteggiatura (ovvero rispetta la regexp [a-zA-Z0-9]+)
						 */
						word.setWord(word.word().toLowerCase());
						word.setWord(stemmer.lemma(word.word(), word.tag()));
						if(!sw.isStopWord(word.word()) && word.word().matches("[a-zA-Z]+")){ //\\w+")){
							filteredSentence.add(word);
						}
					}
					
					/*
					 * Per ogni finestra di testo di largheza windowSize, bisogna controllare
					 * se i tag delle parole rispettano il pattern di ricerca specificato come
					 * parametro della classe, nel qual caso si memorizzano le relative frequenze.
					 */
					for(int i = 0; i < filteredSentence.size() - windowSize + 1; i++ ){
						
						// Array contenente i termini già visti in questa finestra
						ArrayList<String> alreadySeen = new ArrayList<String>();
						freq.augmentWindows();
						
						// Per ogni termine nella finestra
						for(int j = i; j < i + windowSize; j++){
							
														
							if( (j + searchPattern.length) < filteredSentence.size()){
								
								term = "";
								
								// per ogni tag nel pattern di ricerca 
								for(int k=0; k < searchPattern.length; k++){
									
									// estraggo il termine j+k
									curr = filteredSentence.get(j+k);
							
									// se il tag non corrisponde a quello cercato
									if(!curr.tag().equalsIgnoreCase(searchPattern[k])){
										
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
									if( k + 1 == searchPattern.length )
										j += k;
								}
							} else {
								term = filteredSentence.get(j).word();
							}
							
							/* Se il termine non è mai stato visto
							 *  in questa finestra
							 */
							if(!alreadySeen.contains(term)){
								
								// Aggiungo un'occorrenza del termine
								freq.addOccurrence(term);
								
								/*
								 * Aggiungo le occorrenze del termine attuale
								 * con tutti quelli già visti
								 */
								for(String w: alreadySeen){
									freq.addOccurrence(term, w);
								}
								
								// Aggiungo il termine alla lista dei già visti
								alreadySeen.add(term);
							}
						}
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
	 * @param frequencies Matrice contenente le frequenze di tutti i termini
	 * @param corrFunc Funzione di correlazione da utilizzare
	 * @param alpha Valore soglia per alpha-cut
	 * @return un ContextVectors, oggetto contenente tutti i vettori di contesto
	 */
	public static ContextVectors<String> createContextVectors(
			TermFrequencies<String> frequencies, CorrelationFunction corrFunc, double alpha){
		int count;
		double fc, ft, fct, membership;
		ArrayList<String> terms = frequencies.getTerms();
		ContextVectors<String> contextVectors = new ContextVectors<String>(terms);
		
		
		// Per ogni candidato concetto
		for(String concept: terms){
			count = 0;
			// prendo la frequenza del concetto candidato
			fc = frequencies.getFrequency(concept);
			
			// Per ogni termine
			for(String term: terms){
				if(concept.equals(term)) continue;
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

		for(String concept: terms){
			count = 0;
			for(String term: terms){
				if ( contextVectors.getMembership(concept, term) < alpha) count++;
			}
			
			if(count == terms.size()-1)
				contextVectors.deleteConcept(concept);
		}
		
		return contextVectors;
	}
	
	
	
	/**
	 * Metodo che costruisce una tassonomia
	 * @param contextVectors Vettori di contesto
	 * @param lambda soglia minima per la relazione fra concetti
	 * @return Un oggetto di tipo Taxonomy<String>
	 */
	public static Taxonomy<String> createTaxonomy(ContextVectors<String> contextVectors, double lambda){
		
		// Lista di concetti
		ArrayList<String> concepts = contextVectors.getConcepts();
		
		// Lista di termini
		ArrayList<String> terms = contextVectors.getTerms();
		
		// Oggetto tassonomia che verrà restitutito
		Taxonomy<String> taxonomy = new Taxonomy<String>(concepts);
		
		// numeratore e denominatore della formula Spec(ci, cj)
		double num = 0, denom = 0;
		
		// variabili che conterranno la membership di un termine ai due concetti
		double membership_1, membership_2;
		
		// specificità di ci rispetto a cj
		double spec;
		
		// Per ogni coppia di concetti
		for(String c1: concepts){
			for(String c2: concepts){
				
				// se i concetti sono uguali, allora la specificità è 1
				if (c1.equals(c2))
					taxonomy.setSpecificity(c1, c2, 1);
				else {
					
					// Inizializzo numeratore e denominatore
					num = 0;
					denom = 0;
					
					// per ogni termine
					for(String term: terms){
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
					if(spec > lambda)
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
	 * @throws OWLOntologyCreationException 
	 * @throws OWLOntologyStorageException 
	 * @throws FileNotFoundException 
	 **/
	public void saveOWL2FuzzyOntology(Taxonomy tax, String fileName) throws OWLOntologyCreationException, OWLOntologyStorageException, FileNotFoundException{
		int i,j;
		OWLClass s,c;
		//iteratore dei concetti
		ArrayList<String> concetti=tax.getConcepts();
		
		//gestore dell'ontologia
		OWLOntologyManager m = OWLManager.createOWLOntologyManager();
		//iri di riferimento per l'ontologia
		IRI ontIRI = IRI.create("http://www.semanticweb.org/ontologies/concetti");
		//crea ontologia
		OWLOntology ont = m.createOntology(ontIRI);
		//gestore interfaccia per l'ontologia
		OWLDataFactory factory = m.getOWLDataFactory();
		
		//Per ogni concetto crea una classe nell'ontologia
		Map<String,OWLClass> classi=new HashMap<String,OWLClass>();
		for(String con: concetti){
						
			IRI iriClass = IRI.create(ontIRI + "#" + con);
			OWLClass cl=factory.getOWLClass(iriClass);
			classi.put(con, cl);
		}
		
		for(i=0; i<concetti.size(); i++){
			for(j=0; j<concetti.size(); j++){
				String sub=concetti.get(j);
				String cls=concetti.get(i);
				double spec=tax.getSpecificity(sub, cls);
				if(spec > 0){
					OWLAnnotation fuzzyAnnotation = createFuzzyAnnotation(factory, spec);
										
					Set<OWLAnnotation> annotazioni = new HashSet<OWLAnnotation>();
					annotazioni.add(fuzzyAnnotation);
					
					s=classi.get(sub);
					c=classi.get(cls);
					OWLAxiom axiom = factory.getOWLSubClassOfAxiom(s, c, annotazioni);
				
					AddAxiom add = new AddAxiom(ont, axiom);
					m.applyChange(add);
				}
			}
		}
		
		//salvo l'ontologia su file
		m.saveOntology(ont, new FileOutputStream(fileName));
		
	}
	
	public static OWLAnnotation createFuzzyAnnotation(OWLDataFactory f, double value){
		OWLAnnotationProperty fuzzyTag = f.getOWLAnnotationProperty(IRI.create("#fuzzyLabel"));
		String fuzzyowl2 = "<fuzzyOwl2 fuzzyType=\"axiom\">\n\t<Degree value=\"" + value + "\"/></fuzzyOwl2>";
		OWLLiteral fuzzyLiteral = f.getOWLLiteral(fuzzyowl2);
		return f.getOWLAnnotation(fuzzyTag, fuzzyLiteral);
	}
	
}
