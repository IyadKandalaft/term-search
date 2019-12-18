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
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.expressions.Expression;
import org.apache.lucene.expressions.SimpleBindings;
import org.apache.lucene.expressions.js.JavascriptCompiler;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

public class SearchIndex {
	private Path termsPath;
	private Path indexPath;
	private Path outputPath;
	private int searchLimit = 100;
	private int highlightLimit = 500;
	DoubleValuesSource scoringMethod;
	private int numThreads = 4;
	private boolean demoteDocs = false;

	private Directory dirIndex;
	private IndexReader reader;
	private SimpleHTMLFormatter formatter;
	private Analyzer analyzer;
	public IndexSearcher searcher;

	/*
	 * @param terms Path to file with each search phrase/term per line
	 */
	public SearchIndex(String terms) throws IOException, NoSuchMethodException {
		this(terms, "output.tsv");
	}

	/*
	 * @param terms Path to file with each search phrase/term per line
	 *
	 * @param output Path to output file where results are written
	 */
	public SearchIndex(String terms, String output) throws IOException, NoSuchMethodException {
		this(terms, output, "./lucene-index");
	}

	/*
	 * @param terms Path to file with each phrase/term per line
	 *
	 * @param output Path to output file where results are written
	 *
	 * @param index Path to lucene index directory
	 */
	public SearchIndex(String terms, String output, String index) throws IOException, NoSuchMethodException, SecurityException {
		termsPath = Paths.get(terms);
		outputPath = Paths.get(output);
		indexPath = Paths.get(index);

		/* Instantiate searcher using memory mapped directory */
		dirIndex = MMapDirectory.open(indexPath);
		reader = DirectoryReader.open(dirIndex);
		searcher = new IndexSearcher(reader);
		formatter = new SimpleHTMLFormatter("[ ", " ]");
		analyzer = UniqueAnalyzer.getInstance().analyzer;

		// Configure scoring of matched documents by dividing the computed "_score"
		// by the value of the "score" field associated with the indexed document
		try {
			HashMap<String,Method> functions = new HashMap<>();
			functions.putAll(JavascriptCompiler.DEFAULT_FUNCTIONS);
			functions.put("incrementDocMatchCount", SearchDocumentMatches.class.getMethod("incrementDocMatchCount", double.class));

			Expression expr = JavascriptCompiler.compile("_score / score", functions, getClass().getClassLoader());
			if (demoteDocs) {
				expr = JavascriptCompiler.compile("_score / (score + incrementDocMatchCount(docid))", functions, getClass().getClassLoader());
			}

			SimpleBindings bindings = new SimpleBindings();
			bindings.add(new SortField("docid", SortField.Type.DOUBLE));
			bindings.add(new SortField("_score", SortField.Type.SCORE));
			bindings.add(new SortField("score", SortField.Type.DOUBLE));

			scoringMethod = expr.getDoubleValuesSource(bindings);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public int getSearchLimit() {
		return searchLimit;
	}

	public void setSearchLimit(int searchLimit) {
		this.searchLimit = searchLimit;
	}

	public int getHighlightLimit() {
		return highlightLimit;
	}

	public void setHighlightLimit(int highlightLimit) {
		this.highlightLimit = highlightLimit;
	}

	public int getNumThreads() {
		return numThreads;
	}

	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}

	public boolean isDemoteDocs() {
		return demoteDocs;
	}

	/**
	 * Toggles demoting a document's score after each term match
	 * This effectively reduces the chances that subsequent searches match the same document 
	 *
	 * @param demoteDocs Set to True enable document demotion for each match 
	 */
	public void setDemoteDocs(boolean demoteDocs) {
		this.demoteDocs = demoteDocs;
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

        ExecutorService threadPool = Executors.newFixedThreadPool(6);

		// Read the file using a memory buffer to improve performance
		try (BufferedReader in = Files.newBufferedReader(termsPath, StandardCharsets.UTF_8)) {
			String phrase;
			while ((phrase = in.readLine()) != null) {
				// Skip empty lines
				if (phrase.trim().length() == 0) {
					continue;
				}

				class subSearch implements Runnable {
					private String searchString;

					subSearch(String s) {
						searchString = s;
					}

					@Override
					public void run() {
						Query query = getQuery(searchString, field);

						TopDocs searchResults;

						try {
							searchResults = searchPhrase(searchString, field);

							for (ScoreDoc sd : searchResults.scoreDocs) {
								Document d = searcher.doc(sd.doc);

								String fragment = getHighlightedField(query, analyzer, "content", d.get("content"));
								// TODO: Occasionally, fragment is null and throws an exception
								if (fragment == null) {
									continue;
								}

								bufferedWriter
										.write(searchString + "\t" + fragment + "\t" + d.get("title") + lnSeperator);
							}

							System.out.print("Searching for: " + searchString + lnSeperator + "Total Results: "
									+ searchResults.totalHits + lnSeperator);
						} catch (IOException | InvalidTokenOffsetsException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
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

	/*
	 * Search for specific phrase or term in the loaded index
	 *
	 * @param phrase Phrase or term to search for
	 * @param field Name of the field to search
	 *
	 * @return TopDocs results
	 */
	public TopDocs searchPhrase(String phrase, String field) throws IOException {
		Query query = getQuery(phrase, field);
		return searchPhrase(query);
	}

	/*
	 * Search for a query in the loaded index
	 *
	 * @param query Query to search the index with
	 *
	 * @return TopDocs results
	 */
	public TopDocs searchPhrase(Query query) throws IOException {
		return searcher.search(query, searchLimit);
	}

	private Query getQuery(String phrase, String field) {
		PhraseQuery phraseQuery = new PhraseQuery(field, phrase.split(" "));
		return new FunctionScoreQuery(phraseQuery, scoringMethod);
	}

	private String getHighlightedField(Query query, Analyzer analyzer, String fieldName, String fieldValue)
			throws IOException, InvalidTokenOffsetsException {
		QueryScorer queryScorer = new QueryScorer(query);
		Highlighter highlighter = new Highlighter(formatter, queryScorer);
		highlighter.setTextFragmenter(new SimpleSpanFragmenter(queryScorer, highlightLimit));
		highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
		return highlighter.getBestFragment(analyzer, fieldName, fieldValue);
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
