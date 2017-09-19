Per prima cosa trovi il file con i tempi di interarrivo delle query, che ho chiamato msn.day2.arrivals . Il formato e' simile a questo:

2000
2250
2500
2750
Ogni riga rappresenta l'arrivo di una query. Il valore numerico riporta quando arriva la query rispetto all'istante 0. Quindi la prima query arriva dopo 2000ms dall'istante 0, la seconda dopo 2250ms dall'istante 0, la terza dopo 2500ms dall'istante 0 e cosi' via.

Poi nell'archivio hai due cartelle, una per MaxScore ed una per Wand. Prendo ad esempio quella di MaxScore, Wand e' analoga.

File MaxScore.predictions.txt

1    3    640163
2    3    548680
3    2    1276146
4    2    45135

Ogni riga rappresenta una query. La prima riga ti dice che la query con id 1 (primo campo) ha 3 termini (secondo campo) e STIMIAMO di dover "scorare" 640163 (terzo campo) postings per risolverla. Etc etc. Il file contiene 10'000 query. Le stesse identiche query le ritrovi anche per Wand (ma le stime, ovviamente, saranno diverse). Come noterai, il numero di query e' << del numero di query in arrivo su msn.day2.arrivals. Mi aspetto che ad ogni arrivo in msn.day2.arrivals tu associ randomicamente una di queste 10'000 query (possibilmente con un seed fisso, per riprodurre i risultati).

File regressors_MaxScore.txt

query.classes=6
class.1.rmse=0
class.2.rmse=719975
class.3.rmse=708232
class.4.rmse=867497
class.5.rmse=973621
class.6.rmse=1266133
1000000.1.alpha=0.00021
1000000.1.beta=0.99893
1000000.1.rmse=3.49896
1000000.2.alpha=0.00023
1000000.2.beta=26.65930
1000000.2.rmse=32.24749

Questo file mi dice che ho 6 classi di query (query.classes) e quindi 6 regressori. Regressore 1 e' per le query da 1 termine, regressore 2 per quelle da 2 termini, etc, etc regressore 6 (l'ultimo) e' per le query che hanno 6 o piu' termini.

I regressori, come da journal, sono nella forma:

tempoEsecuzione_classe(query) = alpha_frequenza_classe * numero_posting_stimati(query) + beta_frequenza_classe + RMSE_frequenza_classe

Quindi, ad esempio, per la frequenza 1000000 kHz e per query di 1 classe (1 termine), guardo 1000000.1.alpha, 1000000.1.beta, e 1000000.1.rmse . La formula sara'�:

tempoEsecuzione_1000000_classe1(query) = 0.00021 * numero_posting_stimati(query) + 0.99893 + 3.49896 
Per la frequenza 1000000 kHz e per query di 2 classe (2 termini), guardo 1000000.2.alpha, 1000000.2.beta, e 1000000.2.rmse . La formula sara':

tempoEsecuzione_classe2(query) = 0.00023 * numero_posting_stimati(query) + 26.65930 + 32.24749

etc etc.

Quindi, quando ti arriva la query con id 3, da 2 termini (vedi sopra), mi aspetto che il simulatore stimi che ci vorranno

0.00023 ms/postings * 1276146 postings + 26.65930 ms + 32.24749 ms = 352.42037 ms per processare la query.
Il file regressors_normse_MaxScore.txt e' analogo, ma ha gli RMSE fissi a zero.

File MaxScore_time_energy.txt

0 805.493040667 15.286967 891.662979667 14.3075563333 898.837956333 13.5832726667 963.775423 12.237793 1031.03364433 11.9739583333 1110.39194333 11.2403566667 1210.08968667 10.2438966667 1333.407143 9.836202 1374.37797867 9.34676099999 1536.21985433 8.87058500001 1708.42226033 7.51647966666 1991.25148733 7.50569666666 2285.17033267 7.44161000001 2794.44895 5.27400700001 3448.367294 5.51285833333
1 76.768973 1.582967 103.345199667 2.16117366666 88.389748 1.526896 92.4962916667 1.35506166667 100.827880667 1.41611700001 117.514106667 1.555501 115.198173333 1.20135533334 144.817763 1.54412833333 128.598461333 1.04886866667 141.718593 0.97064200001 176.108553667 1.13035033334 186.884345667 1.21738666668 209.185763667 1.08178766666 247.088691667 0.675720666652 342.668110667 0.644775000013
Ogni riga rappresenta una query. Il primo campo e' l'id della query (le query sono sempre quelle di MaxScore.predictions.txt). Dopo l'id, per ogni frequenza, hai una coppia di valori: il primo e' il tempo effettivo di esecuzione in ms, il secondo sono i joules consumati. Ad esempio, la query con id 0 impiega 805.49 ms per essere eseguita e consuma 15.28 Joules a 3.5Ghz, impiega 891.66 ms e consuma 14.30 J a 3.3 Ghz, etc.
1) Le frequenze sono le stesse gia' riportate in regressors_normse_MaxScore.txt, non le ho riportate anche qui. 
2) I campi si intendono dalla frequenza piu' alta a quella piu' bassa, quindi ogni riga sara'

id_query ms@3.5Ghz j@3.5Ghz ms@3.3Ghz j@3.3Ghz ... ms@800Mhz j@800Mhz

Le frequenze sono 15, quindi in totale questo file ha 31 campi per riga.
N.B. questo file riporta meno query di quelle in MaxScore.predictions.txt!! Perche'? Perche' alcune query ci han messo meno di 1 ms ad essere eseguite, che e' minore del tempo di campionamento per le misure di energia sulla nostra CPU. Quindi SE UNA QUERY NON E' IN QUESTO FILE, NON USARLA NELLA SIMULAZIONE (e.g., escludila MaxScore.predictions.txt)



Ricapitolando, cosa mi aspetto dal simulatore: per ogni evento nel file degli arrivi, prendi una query a caso di quelle valide. Usando il numero di posting stimato, stima il tempo di esecuzione della query alle varie frequenze, e fai scegliere a PESOS la frequenza piu' adeguata. Quando la query e' eseguita a frequenza x, manda avanti il tempo della simulazione usando i tempi effettivi che ti ho dato ed incrementa il consumo di energia col relativo valore.
