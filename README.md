<p align="center">
    <img src="https://i.ibb.co/cQ2bYHT/12.jpg" alt="Logo" width=400 height=200>
  </a>

  <h3 align="center">Recoreon</h3>

  <p align="center">
    <b>A collection of Information Retrieval Systems</b> <br>
  </p>
</p>

&nbsp;

# 1. Description

This repository contains different **information retrieval systems** proposed for a small information retrieval competition, 
called **MINITREC**. These systems have been developed in order to satisfy a set of information needs which where given in
advance. The corpus on which the information retrieval systems have been tested has also been provided in advance, specifically, 
it is a collection of **academic papers** and theses available through the **Digital Repository of the University of Zaragoza**. 
This digital repository is accessible through a Web Portal called Zaguán and can be accessed at http://zaguan.unizar.es/.

Below are the information needs that have been worked on written in Spanish:

* Me gustaría saber qué estudios y avances se depositaron, preferentemente del 2010 al 2015, en la campo de la medicina 
  referentes
  a las enfermedades oculares.
* Información acerca de la evolución de las corrientes ideológicas reflejadas en el cine en la segunda mitad del siglo XX.
* Busco documentos sobre desarrollo de videojuegos o diseño de personajes que incluyan técnicas de inteligencia artificial en 
  los últimos 8 años.
* ¿Qué tesis existen que hablen de contaminación en España, Aragón o Zaragoza?
* Busco Trabajos de Fin de Grado dirigidos por un profesor conocido. Sé que se llama Javier, que los trabajos son sobre 
  informática y que se han publicado a partir de 2012.
  
The following information retrieval systems have been implemented:

* A traditional recovery system.
* A semantic recovery system.

Boths systems, the traditional and the semantic, are stores in different folders. The traditional information system is 
located in the directory named **MiniTrecTraditional**,  and the semantic information system is located in the directory 
**MiniTrecSemantic**. These folders contains the eclipse projects that can be imported without problems.

&nbsp;

# 2. Traditional information retrieval system

The traditional information retrieval system designed has the following architecture:

* **Searchfiles**: class that is responsible for carrying out various tasks such as the parsing of information needs, the
  transformation of these needs into queries that can be interpreted by the retrieval system, the execution of such queries 
  and the return to users of the corpus documents obtained after executing each one of them. 
* **IndexFiles**: class that does the indexing of the documents on which the queries are made.
* **CustomSpanishAnalyzer**: class that serves as a tool for the analysis of queries and documents by the query processor 
   and the document indexer, respectively. 
   
The following image shows the class diagram 

<p align="center">
    <img src="https://i.ibb.co/LgV6GFh/Sin-t-tulo.png">
</p>

In order to test the traditional information retrieval system, the information needs were transformed into queries. In order to
show to the users the different documents retrieved, a ranking has been done. The **Okapi BM25 probabilistic model** has been
used to calculate this result ranking.

To see more details about the operation of this system you can have a look to the report owhere all the wotks has been explanied
with detail. This files is **Informe_Sistema_Recuperación_Tradicional_Grupo04.pdf**.

&nbsp;

# 2. Semantic information retrieval system

In order to implement the semantic information retrieval system, the following steps have been realised:

* the downloading of the Zaguán document collection took place. The download was performed by a **crawler**, developed on the
  **Apache Nutch platform**. After that, the results where integrated with the **Solr tool**, which facilitated the search in 
  the collection that had been downloaded from the network. 
* Subsequently, the semantic retrieval system was built. This system required a transformation of the document collection into 
  a **set of semantic resource descriptions in RDF**. These descriptions were dumped on an RDF triplet store, before providing
  the search services that had access and store using the SPARQL query language.

The construction of the RDFS model that describes the structure of the records, their properties and relationships. This schema
was to be defined in order to, Subsequently, convert the original collection to an RDF that complies with the RDFS model. 
Later this model was used to generate the rdfs network that represents the Zaguan document collection. This graph is stored
in the file named **graph04.xml**. This graph is represented in XML format.

After that, in order to take advantage of the semantic search, a terminological model that describe the terms used to describe 
the resources was created. This terminological model was designed following the **skos shcema**. This model provided a hierarchy
of the terms used in the queries, synonyms, and other terms also used in the metadata of the collection that could be considered
as specializations of the terms of the queries and allow to improve the exhaustiveness of the search. The skos model can be 
found in the file **skos04.xml**. 

Subsequently, using the RDF graph as a basis, an OWL enriched model that includes elements that can be used to improve the
quality of the information retrieval system was done by using the skos model.

Finally, the info needs where translated to SPARQL languaje and after getting the results for each of them, both systems were 
compared in order to verify which of them was better.

&nbsp;

# 3. Evaluation

In order to evaluate the information retrieval systems an evaluation system based on the following metrics has been implemented:

* **Precision**: which allows to know number of relevant documents with respect to the total number of documents retrieved 
  for each information need. 
* **Recall**: which allows to know number of relevant documents with respect to the total number of relevant documents 
  for each information need. 
* **F1 score**: which allows to know the quality of the information retrieval systems using the two previous metrics.
* **Prec@10**: the precision to 10.
* **Average_precision**: which averages the value of accuracy for the documents for each time a relevant document is retrieved.
* **Recall_precision**: which allows the generation of the precision-exhaustiveness curve; and the interpolated precision-
  exhaustiveness points.
* **Interpolated_recall_precision**: which allows to generate the precision-exhaustiveness curve interpolated on 11 points
  of exhaustiveness (from 0.0 to 1.0).

Aditionally, the following global measures have be displayed: 

* Global precision (accuracy).
* Global exhaustiveness (recall).
* Global F1 measure (calculated on the basis of the global precision and global exhaustiveness).
* Global precision at 10 (prec@10).
* MAP measure.
* the average interpolated exhaustiveness-precision points (inter-polated_recall_precision) which would allow the generation of
  the precision-exhaustiveness curve interpolated over 11 exhaustiveness points (from 0.0 to 1.0).

&nbsp;

# 4. Authors

* [Victor Peñasco](https://github.com/vpec) - 741294
* [Rubén Rodríguez](https://github.com/ZgzInfinity) - 737215






  






