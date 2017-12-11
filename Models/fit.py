import numpy as np

text = "/home/stefano/git/NetworkSimulator/Models/Monolithic/PESOS/MaxScore/time_energy.txt"
data = np.genfromtxt( text )
power = []
freq  = [0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.1, 2.3, 2.5, 2.7, 2.9, 3.1, 3.3, 3.5]
#freq.reverse()

# Add the reverse of the frequency.
for x in range( 0, len( freq ) ):
    freq.append( -freq[x] )

def cpuEnergy( f, a, b, c ):
    energy = (f**2) * a + f * b + c
    return energy

for i in range( 0, len( data ) ):
    energyList = data[i][2::2]
    
    energyList = np.append( energyList[::-1], energyList[::-1], 0 )
    
    coeff = np.polyfit( freq, energyList, 2 )
    #print cpuEnergy( 0.8, *coeff )
    #print cpuEnergy( 1.0, *coeff )
    #print cpuEnergy( 1.2, *coeff )
    #print cpuEnergy( 1.4, *coeff )
    #print cpuEnergy( 1.6, *coeff )
    #print cpuEnergy( 1.8, *coeff )
    #print cpuEnergy( 2.0, *coeff )
    #print cpuEnergy( 2.1, *coeff )
    #print cpuEnergy( 2.3, *coeff )
    #print cpuEnergy( 2.5, *coeff )
    #print cpuEnergy( 2.7, *coeff )
    #print cpuEnergy( 2.9, *coeff )
    #print cpuEnergy( 3.1, *coeff )
    #print cpuEnergy( 3.3, *coeff )
    #print cpuEnergy( 3.5, *coeff )
    
    energyList = data[i][2::2]
    for x in range( 0, len( freq )/2 ):
        energy = cpuEnergy( freq[x], *coeff )
        energyList[len( freq )/2-1-x] = energy

# Save results into an output file.
output = "/home/stefano/test.txt"
np.savetxt( output, data, delimiter=" " ) 
