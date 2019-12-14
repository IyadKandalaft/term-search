[![CircleCI](https://circleci.com/gh/IyadKandalaft/term-search/tree/dev.svg?style=svg)](https://circleci.com/gh/IyadKandalaft/term-search/tree/dev)
# Term Search

A simple CLI that searches for millions of terms in a large corpus using a minimal set of technologies in an efficient manner.

This command-line tool is simple to use and leverages lucene with multi-threading to index the corpus and permits searching for terms to output the results in a tab delimeted file.  

## Getting Started

This section will outline how to quickly get started using this tool.  

### Pre-requisites

* Java Runtime Environment 8 or 11
* A compiled release of Term Search
    * download the pre-compiled [release](https://github.com/IyadKandalaft/term-search/releases) and extract it.
* A large corpus of text that will be indexed
    * This must contain one document per line where the title and the content are separated by a delimeter such as *:*
    * e.g. Document Title 1*:This is the document content here
* A list of terms to search for in the corpus
    * The terms can be multi-word or hyphen separated
    * Avoid special characters in the terms 

### Indexing the Corpus

The first step is to create an index of the corpus.  Simply run the following: 

```
java -Xmx8G -jar term-search-*.jar index --corpus mycorpus.txt
```

This will create a *lucene-index* folder that houses the index on disk.
The output from this command will provide progress after every 100K of documents indexed.
  
*Note that the maximum java heap size must be increased if the corpus is highly variable and very large.*

### Searching for Terms

The second step is to search the index for terms.  Simply run the following:

```
java -Xmx8G -jar term-search-*.jar search --terms myterms.txt
```

This will begin searching the index and print out general information about how many results were found for each term.

The search results are written to a tab delimited file named *output.txt* by default that contains the search term and the matching document excerpt

### Getting Help

To see all available options for indexing or searching, use *--help* on each respective command.

```
java -jar term-search-*.jar --help
java -jar term-search-*.jar index --help
java -jar term-search-*.jar search --help
```

## Performance Tips

To obtain the optimal performance and shortest indexing and searching times, consider the following suggestions:

* Provide the JRE with a higher maximum heap size using -Xmx 
* Place the corpus on a high throughput drive but latency is not that important (e.g. NFS is okay)
* Place the index on high throughput and low latency drive (e.g. PCIe solid state drives are best)
* Use more threads for indexing and searching by passing the *-t #* option (performance will taper off at some point)


## Development

TODO

### Running Tests

To run the included tests, use the following command

```
mvn test
```

### Components

* [Lucene](https://lucene.apache.org/) - The indexing and searching framework
* [PicoCLI](https://picocli.info/) - The command line interface framework
* [Maven](https://maven.apache.org/) - Dependency Management

### Versioning

Versioning follows the prescribed pattern in [SemVer](http://semver.org/).

See available version using the [tags on this repository](https://github.com/IyadKandalaft/term-search/tags). 

## Authors

* **Michael Douma**
* **Iyad Kandalaft**


## License

This project is licensed under the GPL v3 License - see the [LICENSE.md](LICENSE.md) file for details
