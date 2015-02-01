package FOM;
import CFM.BMI;
import CFM.PMI;
import CFM.CorrelationFunction;
import java.util.ArrayList;

public class MainTest {
	public static void main(String[] args)
	{
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
		String[] path = new String[3];
		path[0] = "Reuters";
		path[1] = "Sicurezza";
		path[2] = "Hackers";
		try{
			System.out.println("Calcolo le frequenze");
			tf = FuzzyOntologyMiner.extractFrequencies(path[1] , windowSize, tags);
			System.out.println("totWin: " + tf.getTotWindows());
			tf.filterTerms(lower, upper);
			
		}catch(Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
		
		for(String e: tf){
			System.out.println(e + ": " + tf.getFrequency(e));
		}
		
		for(String i: tf){
			for(String j: tf){
				if(!i.equals(j)){
					double f = tf.getFrequency(i, j);
					if( f > 0 )
						System.out.println(i + " - " + j + ": " + f);
				}
			}
		}
		
		ContextVectors<String> cv;
		CorrelationFunction cf = new BMI(beta);
		System.out.println("Creo i vettori di contesto");
		cv = FuzzyOntologyMiner.createContextVectors(tf, cf, alpha);
		
		ArrayList<String> concepts = cv.getConcepts();
		ArrayList<String> terms = tf.getTerms();
		
		System.out.println(concepts.size() != terms.size());
		
		
		for(String concept: concepts)
			for(String term: terms)
				if(!concept.equals(term)){
					double mem = cv.getMembership(concept, term);
					
					System.out.println(term + " in " + concept + ": " +mem);
				}
		
		Taxonomy<String> taxonomy;
		
		System.out.println("Creo la tassonomia");
		taxonomy = FuzzyOntologyMiner.createTaxonomy(cv, lambda);
		taxonomy.taxonomyPruning();
		
		for(String c1: concepts)
			for(String c2: concepts){
				double spec = taxonomy.getSpecificity(c1, c2);
				if(spec > 0)
					System.out.println(c1 + " is " + c2 + ": " + taxonomy.getSpecificity(c1, c2));
			}
		
	}

}
