/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import simulator.utils.SimulatorUtils.Size;

public class CPU
{
    private String _model;
    private int _frequency;
    
    /**
     * Create a new CPU object.</br>
     * 
     * @param model    the CPU model
     * @param freq     the CPU frequency, expressed in MHz
    */
    public CPU( final String model, final int freq )
    {
        _model = model;
        _frequency = freq;
    }
    
    /**
     * Computes the CPU energy consumption, required to compute the input data.
     * 
     * @param elapsedTime    time elapsed from the previous received packet
     * @param input          the input size
     * @param size           type of the input size
    */
    public long computeEnergyConsumption( final long elapsedTime, final long input, final Size size )
    {
        // TODO il costo dovrebbe dipendere anche dalla quantita' di carico su quella CPU.
        // TODO Mettere qui tutti i dati raccolti e realizzare una funzione che ne faccia uso.
        /*
         * Tcpu = (Nistr / CPI) * frequency
         * 
         * CPI = Nck / Nistr
         * dove Nck e' il numero di cicli di clock necessari ad eseguire il programma
         * composto da Nistr istruzioni.
         * 
         * Power = Energy/Time  ==>  1W = 1J/s
        */
        // TODO aggiungere anche l'energia della CPU in idle.
        // TODO In tal caso eseguire la seguente formula:
        long cpuTime = timeToCompute( input, size );
        long idle = Math.max( 0, elapsedTime - cpuTime );
        long idle_energy = idle * _frequency;
        
        System.out.println( "ELAPSED_TIME: " + elapsedTime );
        
        return idle_energy;
    }
    
    private long timeToCompute( final long input, final Size size )
    {
        // TODO to implement..
        return 22;
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