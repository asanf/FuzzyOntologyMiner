package Util;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Classe che modella una matrice triangolare espandibile dinamicamente
 * 
 */
public class DTMatrix<E> {
	
	
	
	/**
	 * Costruttore base
	 */
	public DTMatrix(){
		this(minSize);
	}
	
	
	/**
	 * Costruttore con parametro
	 * @param size grandezza della matrice, che sarà size * (size + 1)/2
	 */
	public DTMatrix(int size){
		terms = new ArrayList<E>();
		values = allocateMatrix(size);
	}
	
	/**
	 * Alloca una matrice triangolare inferiore
	 * @param size Grandezza della matrice
	 * @return Una matrice triangolare inferiore
	 */
	protected double[][] allocateMatrix(int size)
	{
		double[][] tmp = new double[size][];
		for(int i = 0; i < size; i++)
			tmp[i] = new double[i+1];
		return tmp;
	}
	
	
	/**
	 * Aumenta le dimensioni di una matrice
	 * @param matrix La matrice iniziale
	 * @return Una matrice grande il doppio, con gli stessi dati dell'originale
	 */
	protected double[][] growSize(double[][] matrix)
	{
		int newSize = matrix.length * 2;
		double[][] newMatrix = allocateMatrix(newSize);
		for(int i = 0; i < matrix.length; i++){
			System.arraycopy(matrix[i], 0, newMatrix[i], 0, matrix[i].length);
		}
		return newMatrix;
	}
	
	/**
	 * Metodo che controlla se la grandezza della matrice dei valori è 
	 * abbastanza grande, se non lo è ne aumenta la grandezza.
	 */
	protected void checkSize(){
		if(terms.size() > values.length)
			values = growSize(values);
	}
	
	
	/**
	 * Metodo che restituisce il valore associato al termine indicato
	 * @param term Il termine di cui si vuole conoscere il valore associato
	 * @return Il valore associato al termine term
	 */
	protected double getValue(E term){
		int i;
		if(!terms.contains(term))
			return -1;
		i = terms.indexOf(term);
		
		return values[i][i];
	}
	
	/**
	 * Metodo che restituisce il valore associato ai termini indicati
	 * @param first Primo termine
	 * @param second Secondo termine
	 * @return Il valore associato a first e second 
	 */
	protected double getValue(E first, E second){
		int i,j;
		if(!(terms.contains(first) && terms.contains(second)))
			return -1;
		i = terms.indexOf(first);
		j = terms.indexOf(second);
		
		if (i < j)
			return values[j][i];
		else
			return values[i][j];
		
	}
	
	/**
	 * Metodo che imposta un valore per un dato termine
	 * @param term il valore di cui si vuole associare il valore
	 * @param value il valore da associare a term
	 */
	protected void setValue(E term, double value){
		int i;
		
		if(!terms.contains(term)){
			terms.add(term);
			checkSize();
		}
		
		i = terms.indexOf(term);
		values[i][i] = value;
	}
	
	/**
	 * Metodo che associa un valore ad una coppia di termini
	 * @param first il primo termine della coppia
	 * @param second il secondo termine della coppia
	 * @param value il valore da associare alla coppia
	 */
	protected void setValue(E first, E second, double value){
		int i,j;
		if(!terms.contains(first)){
			terms.add(first);
			checkSize();
		}
		
		if(!terms.contains(second)){
			terms.add(second);
			checkSize();
		}
		
		i = terms.indexOf(first);
		j = terms.indexOf(second);
		
		if (i < j) 
			values[j][i] = value;
		else
			values[i][j] = value;
	}
	
	/**
	 * Metodo che incrementa il valore associato ad un termine
	 * @param term termine il cui valore va incrementato
	 * @param increment incremento
	 */
	protected void add(E term, double increment){
		double value = 0;
		if(terms.contains(term))
			value = getValue(term);
		
		value += increment;
		
		setValue(term, value);
	}
	
	/**
	 * Metodo che incrementa il valore associato ad una coppia di termini
	 * @param first primo termine
	 * @param second secondo termine
	 * @param increment incremento
	 */
	protected void add(E first, E second, double increment){
		double value = 0;
		if(terms.contains(first) && terms.contains(second))
			value = getValue(first, second);
		
		value += increment;
		
		setValue(first, second, value);
	}
	
	protected void normalizeBy(double value){
		int i,j;
		for(i = 0; i < values.length; i++)
			for(j = 0; j < values[i].length; j++){
				values[i][j] /= value;
			}
	}
	
	protected double values[][];
	protected ArrayList<E> terms;
	private static final int minSize = 100;
}
