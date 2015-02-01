package FOM;
import java.util.ArrayList;
import CFM.CorrelationFunction;
import java.util.Iterator;
import Util.DTMatrix;

public class ContextVectors<E> extends DTMatrix<E> implements Iterable<E>{


	/**
	 * Costruttore
	 * @param terms lista di termini di interesse
	 */
	public ContextVectors(ArrayList<E> terms){
		super(terms.size());
		this.terms = terms;
		
		for(E term: this.terms)
			setValue(term,1);
	}

	public void setMembership(E term, E context, double membership){
		setValue(term, context, membership);
	}
	

    /**
     * @brief iteratore sui termini
     *
     * @return
     */
    public ArrayList<E> getConcepts(){
        ArrayList<E> filteredConcepts = new ArrayList<E>();
        for(E concept: terms)
        	if(getValue(concept) >= 0)
        		filteredConcepts.add(concept);
        return filteredConcepts;
    }

    public ArrayList<E> getTerms(){
    	return terms;
    }
    
    public Iterator<E> iterator(){
    	return getConcepts().iterator();
    }

    /**
     * @brief restituisce la correlazione fra due termini dati
     *
     * @param classTerm
     * @param term
     *
     * @return
     */
    public double getMembership(E classTerm, E term){
        return getValue(classTerm, term);
    }

    
    /**
     * Elimina un concetto candidato, settando a -1 la sua membership
     * @param concept
     */
    public void deleteConcept(E concept){
    	setValue(concept, -1);
    }

    public void normalizeMemberships(){
    	int i;
        ArrayList<E> concepts = getConcepts();
        double min = 100000, max = -100000;
    
        for(E concept: concepts){
                i = terms.indexOf(concept);
                for(int j = 0; j < this.values[i].length - 1; j++){
                                if(values[i][j] < min) min = values[i][j];
                                if(values[i][j] > max) max = values[i][j];
                }
        }
        
        
        for(E concept: concepts){
                i = terms.indexOf(concept);
                
                for(int j = 0; j < this.values[i].length - 1; j++){
                                values[i][j] = (values[i][j]- min)/(max - min);
                }
        }

    }
    
    public String toString(){
    	String toRet = "";
    	for(E t1: terms){
    		toRet += (t1 + ": ");
    		for(E t2: terms)
    			toRet += (getValue(t1, t2) + "\t");
    		toRet += "\n";
    	}
    	return toRet;
    }

}
