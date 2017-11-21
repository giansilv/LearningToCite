# Learning to Cite (JASIST 2016)

This is the Java code implementing the Learning to Cite framework for XML data described in the following paper:

Gianmaria Silvello, "Learning to Cite Framework: How to Automatically Construct Citations for Hierarchical Data". Journal of the Association for Information Science and Technology (JASIST), Volume 68 issue 6, pp. 1505-1524, June 2017.

The paper pre-print is publicly available here: http://www.dei.unipd.it/~silvello/papers/2016-DataCitation-JASIST-Silvello.pdf
The editor's version of the paper is available here: http://onlinelibrary.wiley.com/doi/10.1002/asi.23774/full
A presentation of this work is available here: http://www.dei.unipd.it/~silvello/presentations/2016-02-12_DataCitation_Seminar

We adopted the open-source Java-based XML DBMS BaseX 8.3.18 for realizing the “Pathfinder” and the “XML Retrieval System” components. BaseX is a state-of-the-art Java-based native XML database, which offers both in-memory and secondary-memory storage. BaseX uses compact memory structures and performs compression based on dynamic recognition of data types which, for instance, allows it to determine if a text node is a string or an integer to enable compact storage of the element. Moreover, BaseX provides effective full-text search capabilities which are exploited to perform exact and best match retrieval.

We chose to implement the system in Java in order to make it portable and platform independent; moreover, we employed Apache Maven to simplify the build process and to provide a uniform build system.

More information are available at the URL: http://www.dei.unipd.it/~silvello/datacitation/

A critical review about Theory and Practice of Data Citation is available here: http://www.dei.unipd.it/~silvello/papers/2017-JASIST_preprint_Silvello.pdf
