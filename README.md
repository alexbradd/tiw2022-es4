# Final project for TIW 2022

This repo contains the code for the final project of the course TIW, held at
Politecnico di Milano during 2022.

The projects consists in the creation of a web application with two front-ends:
one using only static HTML and templating and the other as a single page RIA.

Tools used:

1. Tomcat and Java EE
2. Thymeleaf
3. MySQL
4. Intellij IDEA (IDE)

## Full specification

### Static site

Un’applicazione web consente la gestione di trasferimenti di denaro online da un
conto a un altro. L’applicazione supporta registrazione e login mediante una
pagina pubblica con opportune form. La registrazione controlla la validità
sintattica dell’indirizzo di email e l’uguaglianza tra i campi “password” e
“ripeti password”. La registrazione controlla l’unicità dello username. Un
utente ha un nome, un cognome, uno username e uno o più conti correnti. Un conto
ha un codice, un saldo, e i trasferimenti fatti (in uscita) e ricevuti (in
ingresso) dal conto. Un trasferimento ha una data, un importo, un conto di
origine e un conto di destinazione. Quando l’utente accede all’applicazione
appare una pagina LOGIN per la verifica delle credenziali. In seguito
all’autenticazione dell’utente appare l’HOME page che mostra l’elenco dei suoi
conti. Quando l’utente seleziona un conto, appare una pagina STATO DEL CONTO che
mostra i dettagli del conto e la lista dei movimenti in entrata e in uscita,
ordinati per data discendente. La pagina contiene anche una form per ordinare un
trasferimento. La form contiene i campi: codice utente destinatario, codice
conto destinatario, causale e importo. All’invio della form con il bottone
INVIA, l’applicazione controlla che il conto di destinazione appartenga
all’utente specificato e che il conto origine abbia un saldo superiore o uguale
all’importo del trasferimento. In caso di mancanza di anche solo una condizione,
l’applicazione mostra una pagina con un avviso di fallimento che spiega il
motivo del mancato trasferimento. Nel caso in cui entrambe le condizioni siano
soddisfatte, l’applicazione deduce l’importo dal conto di origine, aggiunge
l’importo al conto di destinazione e mostra una pagina CONFERMA TRASFERIMENTO
che presenta i dati dell’importo trasferito e i dati del conto di origine e di
destinazione con i rispettivi saldi precedenti al trasferimento e aggiornati
dopo il trasferimento. L’applicazione deve garantire l’atomicità del
trasferimento: ogni volta che il conto di destinazione viene addebitato, il
conto di origine deve essere accreditato. Ogni pagina contiene un collegamento
per tornare alla pagina precedente. L’applicazione consente il logout
dell’utente.

### RIA

Si realizzi un’applicazione client server web che modifica le specifiche
precedenti come segue:

- La registrazione controlla la validità sintattica dell’indirizzo di email e
  l’uguaglianza tra i campi “password” e “ripeti password”, anche a lato client.
- Dopo il login, l’intera applicazione è realizzata con un’unica pagina.
- Ogni interazione dell’utente è gestita senza ricaricare completamente la
  pagina, ma produce l’invocazione asincrona del server e l’eventuale modifica
  del contenuto da aggiornare a seguito dell’evento.
- I controlli di validità dei dati di input (ad esempio importo non nullo e
  maggiore di zero) devono essere realizzati anche a lato client.
- L’avviso di fallimento è realizzato mediante un messaggio nella pagina che
  ospita l’applicazione.
- L’applicazione chiede all’utente se vuole inserire nella propria rubrica i
  dati del destinatario di un trasferimento andato a buon fine non ancora
  presente. Se l’utente conferma, i dati sono memorizzati nella base di dati e
  usati per semplificare l’inserimento. Quando l’utente crea un trasferimento,
  l’applicazione propone mediante una funzione di auto-completamento i
  destinatari in rubrica il cui codice corrisponde alle lettere inserite nel
  campo codice utente destinatario.
