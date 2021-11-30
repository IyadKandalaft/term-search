package com.iyadk.termsearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.expressions.SimpleBindings;
import org.apache.lucene.expressions.js.JavascriptCompiler;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

public class SearchIndex {
	private Path termsPath;
	private Path indexPath;
	private Path outputPath;
	private int matchLimit = 20;
	private int resultLimit = 5;
	private int sourceLimit = 125;
	private int highlightMin = 50;
	private int highlightMax = 180;
	private String scoringFormula = "_score / score";
	private DoubleValuesSource scoringMethod;
	private boolean explainScoring = false;
	// private MatchList excludedWords;
	private int numThreads = 4;
	private ExcerptScorer excerptScorer;
	private boolean expandSearch = true;
	private int expandIterations = 3;
	private double expandFactor = 2.0;

	private final Directory dirIndex;
	private final IndexReader reader;
	private final Analyzer analyzer;
	public final IndexSearcher searcher;


	/**
	 * 
	 * @param terms Path to file with each search phrase/term per line
	 */
	public SearchIndex(String terms) throws IOException {
		this(terms, "output.tsv");
	}

	/**
	 *
	 * @param terms Path to file with each search phrase/term per line
	 * @param output Path to output file where results are written
	 */
	public SearchIndex(String terms, String output) throws IOException {
		this(terms, output, "./lucene-index");
	}

	/**
	 * @param matchLimit Maximum number of matches to review
	 */
	public void setMatchLimit(int matchLimit) {
		this.matchLimit = matchLimit;
	}

	/**
	 * Creates a SearchIndex object that is used to search for terms in the lucene index
	 * 
	 * @param terms Path to file with each phrase/term per line
	 * @param output Path to output file where results are written
	 * @param index Path to lucene index directory
	 */
	public SearchIndex(String terms, String output, String index) throws IOException {
		termsPath = Paths.get(terms);
		outputPath = Paths.get(output);
		indexPath = Paths.get(index);

		/* Instantiate searcher using memory mapped directory */
		dirIndex = MMapDirectory.open(indexPath);
		reader = DirectoryReader.open(dirIndex);
		searcher = new IndexSearcher(reader);
		analyzer = UniqueAnalyzer.getInstance().analyzer;

		excerptScorer = new ExcerptScorer();

		//setExcludedWords(new ArrayList<String>());
		setScoring(scoringFormula);
	}

	/**
	 * Set the scoring formula used to rank search results
	 * 
	 * The formula can be any mathematical formula using a combination of operators, functions, and computed variables.
	 * The supported functions include javascript's builtins such as pow, log10, abs
	 * The computed variables include _score (lucene's matching score), score (the document's parsed score), and
	 * docMatchCount (the count that this document has matched previously).
	 * 
	 * @param scoringFormula Mathematical formula to use 
	 */
	public void setScoring(String scoringFormula) {
		HashMap<String,Method> functions = new HashMap<>();

		// docMatchCount is a function that needs to be passed the docid
		scoringFormula = scoringFormula.replaceAll("docMatchCount", "docMatchCount(docid)");
		this.scoringFormula = scoringFormula;

		// Add all javascript functions to the list of usable functions
		functions.putAll(JavascriptCompiler.DEFAULT_FUNCTIONS);

		// Add bindings for the lucene _score, the parsed document score, and the unique docid
		SimpleBindings bindings = new SimpleBindings();
		bindings.add(new SortField("docid", SortField.Type.DOUBLE));
		bindings.add(new SortField("_score", SortField.Type.SCORE));
		bindings.add(new SortField("score", SortField.Type.DOUBLE));

		// Add custom function that permits looking up the number of matches
		// that the current document has had previously
		Method docMatchCountMethod;
		try {
			docMatchCountMethod = SearchDocumentMatches.class.getMethod("getDocMatchCount", double.class);
			functions.put("docMatchCount", docMatchCountMethod);
		} catch (NoSuchMethodException e) {
			System.out.println("The SearchDocumentMatches.incrementDocMatchCount method is missing");
		}

		Method randomMethod;
		try {
			randomMethod = Math.class.getMethod("random");
			functions.put("random", randomMethod);
		} catch (NoSuchMethodException e) {
			System.out.println("The Math.random method is missing");
		}

		try {
			scoringMethod = JavascriptCompiler.compile(scoringFormula, functions, getClass().getClassLoader())
					.getDoubleValuesSource(bindings);		
		} catch (ParseException | SecurityException e) {
			System.out.println("The scoring method could not be parsed or contains an invalid method");
			System.exit(1);
		}
	}

	public ExcerptScorer getExcerptScorer() {
		return excerptScorer;
	}

	public void setExcerptScorer(ExcerptScorer excerptScorer) {
		this.excerptScorer = excerptScorer;
	}

	public boolean isExpandSearch() {
		return expandSearch;
	}

	public void setExpandSearch(boolean expandSearch) {
		this.expandSearch = expandSearch;
		if (! expandSearch)
			setExpandIterations(0);
	}

	public int getExpandIterations() {
		return expandIterations;
	}

	public void setExpandIterations(int expandIterations) {
		if (! this.isExpandSearch())
			this.expandIterations = 0;
		else
			this.expandIterations = expandIterations;
	}

	public double getExpandFactor() {
		return expandFactor;
	}

	public void setExpandFactor(double expandFactor) {
		this.expandFactor = expandFactor;
	}

	/**
	 * 
	 * @return True if a scoring explanation will be provided with the output. 
	 */
	public boolean isExplainScoring() {
		return explainScoring;
	}

	/**
	 * Toggle scoring explanation output
	 * @param explainScoring Set to true to provide a scoring explanation in the output
	 */
	public void setExplainScoring(boolean explainScoring) {
		this.explainScoring = explainScoring;
	}


	/**
	 * Set a list of words to exclude from the results
	 * @param excludedWords A list of words to exclude from the results

	public void setExcludedWords(List<String> excludedWords) {
		this.excludedWords = new MatchList(excludedWords);
		System.out.printf("Added %d words to the exclusion list" + System.lineSeparator(), this.excludedWords.count());
	}

	/**
	 * Set a list of words to exclude from the results
	 * @param excludedWordsFile A file with words to exclude from the results

	public void setExcludedWords(Path excludedWordsFile) {
		try {
			setExcludedWords(Files.readAllLines(excludedWordsFile));
		} catch (IOException e) {
			System.out.println("Cannot read Excluded Words file " + excludedWordsFile);
		}
	}
	

	/**
	 * 
	 * @return A MatchList object containing words to exclude 
	
	public MatchList getExcludedWords() {
		return excludedWords;
	}
	*/

	/**
	 * @return Maximum number of matches to review
	 */
	public int getMatchLimit() {
		return matchLimit;
	}

	/**
	 * Get the maximum results to generate per term
	 * @return Maximum number of results
	 */
	public int getResultLimit() {
		return resultLimit;
	}

	/**
	 * Set the maximum results to generate per term
	 * @param resultLimit Maximum number of results  
	 */
	public void setResultLimit(int resultLimit) {
		this.resultLimit = resultLimit;
	}

	/**
	 * Get the maximum results to generate per source
	 * @return Maximum number of results per source
	 */
	public int getSourceLimit() {
		return sourceLimit + 1;
	}

	/**
	 * Set the maximum results to generate per source
	 * @param sourceLimit Maximum number of results per source
	 */
	public void setSourceLimit(int sourceLimit) {
		this.sourceLimit = sourceLimit - 1;
	}

	public int[] getHighlightLimit() {
		int highlightLimit[] = new int[]{highlightMin, highlightMax};
		return highlightLimit;
	}

	public void setHighlightLimit(int highlightMin, int highlightMax) {
		this.highlightMin = highlightMin;
		this.highlightMax = highlightMax;
	}

	public int getNumThreads() {
		return numThreads;
	}

	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}

	/*
	 * Search for all the terms/phrases in the supplied terms file
	 *
	 * @param field Name of the field to search
	 */
	public void searchAll(String field) throws IOException, InvalidTokenOffsetsException, InterruptedException {
		File outputFile = outputPath.toFile();

		// Create the parent directory directory of the output file
		if (! outputFile.getParentFile().exists())
			outputFile.getParentFile().mkdirs();

		// Create the output file
		if (! outputFile.exists())
			outputFile.createNewFile();

		/*
		 * Write output to a file using buffering.  Large memory buffers reduce IOPS and blocking.
		 */
		FileWriter fileWriter = new FileWriter(outputFile);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter, 2^24);

		// Use the correct line separator based on the operating system
		String lnSeperator = System.lineSeparator();

        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);

		// Read the file using a memory buffer to improve performance
		try (BufferedReader in = Files.newBufferedReader(termsPath, StandardCharsets.UTF_8)) {
			String phrase;
			while ((phrase = in.readLine()) != null) {
				// Skip empty lines
				if (phrase.trim().length() == 0) {
					continue;
				}

				class subSearch implements Runnable {
					private final String searchString;
					subSearch(String s) {
						this.searchString = s;
					}

					@Override
					public void run() {
						Query query = getQuery(searchString, field);
						StringBuilder scoringExplanation = new StringBuilder();

						TopDocs searchResults = null;
						String[] fragments;

						try {
							PriorityQueue<Excerpt> excerptsQueue = new PriorityQueue<>(1, new ExcerptComparator());
							
							int i = 0;
							// TODO: Bug with expansion returning the correct results
							searchIterLoop: for (int searchIteration = 1; searchIteration <= expandIterations + 1; searchIteration++) {
								int effectiveMatchLimit = getEffectiveMatchLimit(searchIteration);

								searchResults = searchPhrase(searchString, field, effectiveMatchLimit);

								if ( searchResults.totalHits.value == 0 )
									break;

								excerptsQueue = new PriorityQueue<>(effectiveMatchLimit, new ExcerptComparator());

								// Configure term highlighting in results
								UnifiedHighlighter highlighter = new UnifiedHighlighter(searcher, analyzer);
								highlighter.setMaxLength(Integer.MAX_VALUE - 1);

								NaturalBreakIterator lengthBreakIterator = new NaturalBreakIterator(highlightMin, highlightMax, searchString.length());
								highlighter.setHandleMultiTermQuery(true);
								highlighter.setHighlightPhrasesStrictly(true);
								highlighter.setScorer(new UniquePassageScorer());
								highlighter.setBreakIterator(() -> lengthBreakIterator);
								highlighter.setFormatter(new DefaultPassageFormatter("", "", "...", false));

								fragments = highlighter.highlight(field, query, searchResults, 1);

								if (explainScoring) {
									for (int z = 0; z < searchResults.scoreDocs.length; z++) {
										scoringExplanation.append("Match #" + String.valueOf(z) + lnSeperator);
										scoringExplanation.append(searcher.explain(query, searchResults.scoreDocs[z].doc));
									}
								}

								for (; i < searchResults.scoreDocs.length; i++) {
									Excerpt fragment = new Excerpt(fragments[i]);

									
									// Skip this match if it doesn't meet our length requirements
									/*
									if (fragment.length() < highlightMin || fragment.length() > highlightMax) {
										if (explainScoring) {
											scoringExplanation.append("Skipping match #");
											scoringExplanation.append(String.valueOf(i)); 
											scoringExplanation.append(" because it doesn't meet excerpt length limits");
											scoringExplanation.append(lnSeperator);
										}
										continue;
									}*/

									/*
									if ( Character.isLowerCase(fragment.charAt(0)) ) {
										if (explainScoring) {
											scoringExplanation.append("Skipping match #");
											scoringExplanation.append(String.valueOf(i)); 
											scoringExplanation.append(" because the first character is lower case");
											scoringExplanation.append(lnSeperator);
										}
										continue;
									}*/

									/*
									if (! excludedWords.phraseMatch(searchString) && excludedWords.phraseMatch(fragment.toString())) {
										if (explainScoring) {
											scoringExplanation.append("Skipping match #");
											scoringExplanation.append(String.valueOf(i)); 
											scoringExplanation.append(" because it contains an excluded word");
											scoringExplanation.append(lnSeperator);
										}
										continue;
									}*/

									Document doc = searcher.doc(searchResults.scoreDocs[i].doc);
									fragment.setDocumentTitle(doc.get("title"));
									fragment.setDocId(doc.getField("docId").numericValue().doubleValue());

									// Score the excerpt and add it to the queue
									// Skip the excerpt of the score function returns false
									if (excerptScorer.score(fragment, searchString))
										excerptsQueue.offer(fragment);
								}
								
								if (excerptsQueue.size() >= matchLimit || searchResults.totalHits.value < matchLimit)
									break searchIterLoop;
								
								if (explainScoring) {
									scoringExplanation.append("Expanding search limit because we exhausted the current top matches.");
									scoringExplanation.append(lnSeperator);
									scoringExplanation.append("New match limit is: ");
									scoringExplanation.append(String.valueOf(getEffectiveMatchLimit(searchIteration + 1)));
									scoringExplanation.append(lnSeperator);
								}
							}

							int resultCount = 0;
							while(! excerptsQueue.isEmpty()) {
								Excerpt excerpt = excerptsQueue.remove();
								
								double docCount = SearchDocumentMatches.getDocMatchCount(excerpt.getDocId());

								if (docCount > sourceLimit) {
									if (explainScoring) {
										scoringExplanation.append("Skipping match ");
										// scoringExplanation.append(String.valueOf(i));
										scoringExplanation.append(" because the source has been used too many times");
										scoringExplanation.append(lnSeperator);
									}
									continue;
								}

								SearchDocumentMatches.incrementDocMatchCount(excerpt.getDocId());

								bufferedWriter.write(searchString + "\t" + excerpt.replaceAll("[\\t\\r\\n]",  " ") +
									"\t" + excerpt.getDocumentTitle().replaceAll("[\\t\\r\\n]", " ") + lnSeperator);
								
								if (++resultCount >= resultLimit)
									break;
							}
						} catch ( IOException e ) {
							// Something is wrong with reading the index
							return;
						}

						System.out.print("Searching for: " + searchString + lnSeperator +
								"Total Results: " + searchResults.totalHits + lnSeperator +
								scoringExplanation + lnSeperator);
					}
				}

				threadPool.execute(new subSearch(phrase));
				
			}
		}

		// Wait for all threads to terminate
		threadPool.shutdown();
		while (!threadPool.isTerminated()) {}
		
		bufferedWriter.close();
	}

	/**
	 * Get the effective match limit depending on whether expandSearch is set to true
	 * and the current search iteration and expand factor
	 * 
	 * @param searchIteration Search iteration number
	 * @return
	 */
	private int getEffectiveMatchLimit(int searchIteration) {
		if (this.isExpandSearch() && searchIteration != 1) {
			return (int) (this.matchLimit * Math.pow(this.expandFactor, searchIteration - 1));
		}
		return this.matchLimit;
	}

	/*
	 * Search for specific phrase or term in the loaded index using the defined matchLimit
	 *
	 * @param phrase Phrase or term to search for
	 * @param field Name of the field to search
	 *
	 * @return TopDocs results
	 */
	public TopDocs searchPhrase(String phrase, String field) throws IOException {
		Query query = getQuery(phrase, field);
		return searchPhrase(query, this.matchLimit);
	}

	/*
	 * Search for specific phrase or term in the loaded index and retrieve and limit the number of results
	 *
	 * @param phrase Phrase or term to search for
	 * @param field Name of the field to search
	 * @param maxResults Maximum number of results to return 
	 *
	 * @return TopDocs results
	 */
	public TopDocs searchPhrase(String phrase, String field, int maxResults) throws IOException {
		Query query = getQuery(phrase, field);
		return searchPhrase(query, maxResults);
	}

	/**
	 * Search for a query in the loaded index
	 *
	 * @param query Query to search the index with
	 * @param maxResults Maximum number of results to return
	 *
	 * @return TopDocs results
	 */
	public TopDocs searchPhrase(Query query, int maxResults) throws IOException {
		return searcher.search(query, maxResults);
	}

	private Query getQuery(String phrase, String field) {
		// Ensure that multiple search terms are matched exactly without any 
		// words in between (no slop)
		int slop = 0;
		// Remove hyphens and periods from search terms to permit various occurrence patterns
		// e.g. "well-groomed" matches "well groomed" in addition to "well-groomed"
		phrase = phrase.replace('-', ' ').replace(".", "");

		PhraseQuery phraseQuery = new PhraseQuery(slop, field, phrase.split(" "));
		return new FunctionScoreQuery(phraseQuery, scoringMethod);
	}

	/*
	 * Release buffers
	 */
	public void close() {
		try {
			reader.close();
			dirIndex.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
