import numpy as np

data=np.genfromtxt("/home/stefano/git/NetworkSimulator/Models/Monolithic/PESOS/MaxScore/time_energy.txt")
power=[]
freq=[0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0, 2.1, 2.3, 2.5, 2.7, 2.9, 3.1, 3.3, 3.5]
    
print "LEN"
print len( data )

for x in range(0,15):
    for e, t in zip(data[:,(x*2)+2], data[:,(x*2)+1]):
        #TODO trovare il modo di inserire questi valori all'interno del vettore.
        
        print e, t

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
