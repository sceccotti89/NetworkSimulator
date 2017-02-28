/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import simulator.utils.SimulatorUtils.Size;

public class CPU
{
    private String _model;
    private double _frequency;
    
    /**
     * Create a new CPU object.</br>
     * 
     * @param freq  the CPU frequency, expressed in MHz
    */
    public CPU( final String model, final double freq )
    {
        _model = model;
        _frequency = freq;
    }
    
    public long computeEnergyConsumption( final long input, final Size size )
    {
        // TODO il costo dovrebbe dipendere anche dalla quantia' di carico su quella CPU.
        // TODO Mettere qui tutti i dati raccolti e realizzare una funzione che ne faccia uso.
        // TODO ovviamente si dovrebbe aggiungere anche l'energia della CPU in idle.
        /*
         * Tcpu = (Nistr / CPI) * frequency
         * 
         * CPI = Nck / Nistr
         * dove Nck e' il numero di cicli di clock necessari ad eseguire il programma
         * composto da Nistr istruzioni.
         * 
         * Power = Energy/Time  ==>  1W = 1J/s
        */
        
        // TODO implement!!
        return 42;
    }
    
    public double getFrequency() {
        return _frequency;
    }
    
    @Override
    public String toString()
    {
        return "Name: " + _model + ", FREQUENCY: " + _frequency;
    }
}