import numpy as np

text = "/home/stefano/time_energy.txt"
data = np.genfromtxt( text )
power = []
freq  = [0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.1, 2.3, 2.5, 2.7, 2.9, 3.1, 3.3, 3.5]
#freq.reverse()

# Add the reverse of the frequency.
#for x in range( 0, len( freq ) ):
#    freq.append( -freq[x] )

def cpuEvaluation( f, a, b, c ):
    value = (f**2) * a + f * b + c
    return value

for i in range( 0, len( data ) ):
    queryID    = data[i][0]
    data[i][0] = queryID+1
    
    energyList = data[i][2::2]
    timeList   = data[i][1::2]
    
    #print "ENERGY1", energyList
    #energyList = np.append( energyList[::-1], energyList[::-1], 0 )
    #timeList   = np.append( timeList[::-1], timeList[::-1], 0 )
    
    #print "ENERGY2", energyList
    
    energyCoeff = np.polyfit( freq, energyList, 2 )
    #print cpuEvaluation( 0.8, *energyCoeff )
    #print cpuEvaluation( 1.0, *energyCoeff )
    #print cpuEvaluation( 1.2, *energyCoeff )
    #print cpuEvaluation( 1.4, *energyCoeff )
    #print cpuEvaluation( 1.6, *energyCoeff )
    #print cpuEvaluation( 1.8, *energyCoeff )
    #print cpuEvaluation( 2.0, *energyCoeff )
    #print cpuEvaluation( 2.1, *energyCoeff )
    #print cpuEvaluation( 2.3, *energyCoeff )
    #print cpuEvaluation( 2.5, *energyCoeff )
    #print cpuEvaluation( 2.7, *energyCoeff )
    #print cpuEvaluation( 2.9, *energyCoeff )
    #print cpuEvaluation( 3.1, *energyCoeff )
    #print cpuEvaluation( 3.3, *energyCoeff )
    #print cpuEvaluation( 3.5, *energyCoeff )
    
    #energyList = data[i][2::2]
    for x in range( 0, len( freq ) ):
        energy = cpuEvaluation( freq[x], *energyCoeff )
        energyList[x] = energy
        #energyList[len( freq )/2-1-x] = energy
        if energy >= 3000:
            print "ID:", (queryID+1), ", FREQ:", freq[x], ", ENERGY:", energy
    
    #timeCoeff = np.polyfit( freq, timeList, 2 )
    timeCoeff = np.polyfit( freq, timeList, 2 )
    #print "TIME: ", cpuEvaluation( 0.8, *timeCoeff )
    #timeList = data[i][1::2]
    for x in range( 0, len( freq ) ):
        time = cpuEvaluation( freq[x], *timeCoeff )
        timeList[x] = time
        #timeList[len( freq )/2-1-x] = time

#quit()

# Save results into an output file.
output = "/home/stefano/test.txt"
np.savetxt( output, data, delimiter=" " ) 

quit()

for x in range(0,15):
    power.append(np.mean( (data[:,(x*2)+2]/(data[:,(x*2)+1]/1000.0))))
power.reverse()

for x in range(0,15):
    freq.append(-freq[x])
    power.append(power[x])

print(freq)
print(power)

coeff=np.polyfit(freq, power, 2)

print(coeff)

def cpupower(cores):
    power=coeff[2]-0.1
    for x in cores:
        power+=coeff[0]*(x**2)+0.1
    return power

print(cpupower([0.8]))
print(cpupower([0.8,0.8]))
print(cpupower([0.8,3.5]))
print(cpupower([3.5,3.5,3.5,3.5]))
print(cpupower([]))
