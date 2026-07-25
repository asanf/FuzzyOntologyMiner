package asanf.FOM;


import asanf.FOM.Util.DTMatrix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ContextVectors<E> extends DTMatrix<E> implements Iterable<E> {


    /**
     * Costruttore
     *
     * @param terms lista di termini di interesse
     */
    public ContextVectors(Collection<E> terms) {
        super(terms.size());

        for (E term : terms)
            setValue(term, 1);
    }

    public void setMembership(E term, E context, double membership) {
        setValue(term, context, membership);
    }


    /**
     * @return
     * @brief iteratore sui termini
     */
    public ArrayList<E> getConcepts() {
        ArrayList<E> filteredConcepts = new ArrayList<E>();
        terms.forEachKey(concept -> {
            if (getValue(concept) >= 0)
                filteredConcepts.add(concept);
        });
        return filteredConcepts;
    }


    public Iterator<E> iterator() {
        return getConcepts().iterator();
    }

    /**
     * @param classTerm
     * @param term
     * @return
     * @brief restituisce la correlazione fra due termini dati
     */
    public double getMembership(E classTerm, E term) {
        return getValue(classTerm, term);
    }


    /**
     * Elimina un concetto candidato, settando a -1 la sua membership
     *
     * @param concept
     */
    public void deleteConcept(E concept) {
        setValue(concept, -1);
    }

    public void normalizeMemberships() {
        int i;
        ArrayList<E> concepts = getConcepts();
        double min = 100000, max = -100000;

        for (E concept : concepts) {
            i = index(concept);
            for (int j = 0; j < this.values[i].length - 1; j++) {
                if (values[i][j] < min) min = values[i][j];
                if (values[i][j] > max) max = values[i][j];
            }
        }


        for (E concept : concepts) {
            i = index(concept);

            for (int j = 0; j < this.values[i].length - 1; j++) {
                values[i][j] = (values[i][j] - min) / (max - min);
            }
        }

    }

    public String toString() {
        StringBuilder toRet = new StringBuilder();
        terms.forEachKey(t1 -> {

            toRet.append(t1 + ": ");
            terms.forEachKey(t2 -> {

                toRet.append(getValue(t1, t2) + "\t");

            });

            toRet.append("\n");
        });
        return toRet.toString();
    }

    public Collection<E> getTerms() {
        return terms.keySet();
    }

}
