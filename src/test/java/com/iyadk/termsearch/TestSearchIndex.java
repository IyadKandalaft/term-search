package com.iyadk.termsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class TestSearchIndex {
	private static String indexDir;
	private static String termsFile;
	private static SearchIndex indexSearcher;

	// Field to search for most tests
	String field = "content";
	
	@BeforeClass
	public static void setUp() throws Exception {
		indexDir = Files.createTempDirectory("lucene-index-test").toString();

		String corpusFile = TestSearchIndex.class.getClassLoader().getResource("test-corpus.txt").getFile().toString();
		termsFile = TestSearchIndex.class.getClassLoader().getResource("test-terms.txt").getFile().toString();

		/*
		 * Create a known index from the test-courpus.txt resource
		 */
		IndexCreator indexCreator = new IndexCreator(corpusFile, indexDir);
		indexCreator.create();

		File outputFile = Paths.get("./output.tsv").toFile();
		System.out.println("Creating index");
		try {
			indexSearcher = new SearchIndex(termsFile, outputFile.toString(), indexDir);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Running tests...");

	}

	@AfterClass
	public static void tearDown() throws Exception {
		System.out.println("Deleting index");
		Files.walk(Paths.get(indexDir)).map(Path::toFile).forEach(File::delete);
	}

	@Test
	public void testSearchTermLCase() throws IOException {
		String phrase = "cookie";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(String.format("No results found for search phrase/term: %s", phrase),
				0, results.scoreDocs.length);
				
		assertEquals(String.format("Incorrect number of results were found for search phrase: %s", phrase),
				1, results.scoreDocs.length);
				

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			// Lowecase should match
			assertTrue(String.format("Lowecase result was not matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text that will match the term cookie"));
		}
	}

	@Test
	public void testSearchTermCCase() throws IOException {
		String phrase = "Cookie";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(String.format("No results found for search phrase/term: %s", phrase),
				0, results.scoreDocs.length);
				
		assertEquals(String.format("Incorrect number of results were found for search phrase: %s", phrase),
				1, results.scoreDocs.length);
				

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			// Camelcase should match
			assertTrue(String.format("CamelCase results was not matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text that will match the term Cookie"));
		}
	}

	@Test
	public void testSearchTermUCase() throws IOException {
		String phrase = "COOKIE";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(String.format("No results found for search phrase/term: %s", phrase), 
				0, results.scoreDocs.length);
		assertEquals(String.format("Incorrect number of results were found for search phrase: %s", phrase),
				1, results.scoreDocs.length);

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			// Uppercase should match
			assertTrue(String.format("Uppercase result was not matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text that will match the term COOKIE"));
		}
	}

	@Test
	public void testSearchPhraseLCase() throws IOException {
		String phrase = "two words";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(String.format("No results found for search phrase/term: %s", phrase),
				0, results.scoreDocs.length);
		assertEquals(String.format("Incorrect number of results were found for search phrase: %s", phrase),
				1, results.scoreDocs.length);

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("Lower case result was not matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text where a term is composed of two words"));
			assertFalse(String.format("Uppercase phrase was matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text where a term is composed of Two Words with capitals"));
		}
	}

	@Test
	public void testSearchPhraseUCase() throws IOException {
		String phrase = "Two Words";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(String.format("No results found for search phrase/term: %s", phrase),
				0, results.scoreDocs.length);
		assertEquals(String.format("Incorrect number of results were found for search phrase: %s", phrase),
				1, results.scoreDocs.length);

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertFalse(String.format("Lower case result was matched for phrase/term: %s", phrase),
					d.get(field).contentEquals("This is sample text where a term is composed of two words"));
			assertTrue(String.format("Uppercase phrase was matched for phrase/term: %s", phrase), d.get(field)
					.contentEquals("This is sample text where a term is composed of Two Words with capitals"));
		}
	}

	@Test
	public void testSearchInsideQuotes() throws IOException {
		String phrase = "quote";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(String.format("No results found for search phrase/term: %s", phrase),
				0, results.scoreDocs.length);

		assertEquals(String.format("Incorrect number of results were found for search phrase: %s", phrase),
				3, results.scoreDocs.length);

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("Terms found inside quotes for phrase/term: %s", phrase),
					(d.get(field).contentEquals("This is sample text where a term is surrounded by \"quote\"")
							|| d.get(field).contentEquals("This is sample text where a term ends with \"a quote\"")
							|| d.get(field).contentEquals(
									"This is sample text where a term starts with a \"quote only\"")));
		}
	}

	@Test
	public void testSearchTermHyphen() throws IOException {
		String phrase = "traffic";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertEquals(String.format("More than two results were found for search phrase: %s", phrase),
				2, results.scoreDocs.length);

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("Result without a hyphen was matched for phrase/term: %s", phrase),
					d.get(field).endsWith("will match"));
		}
	}
	
	@Test
	public void testSearchTermIgnoreFirstInDocument() throws IOException {
		String phrase = "Pancakes";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertEquals(String.format("The first word in the sentence is being matched for the phrase/term: %s", phrase),
				1, results.scoreDocs.length);

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("The first word in the sentence is being matched for the phrase/term: %s", phrase),
					d.get(field).endsWith("will match"));
		}
	}

	@Test
	public void testSearchTermIgnoreFirstInSentences() throws IOException {
		String phrase = "First";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertEquals(String.format("Incorrect number of results were found for search phrase/term: %s", phrase),
				0, results.scoreDocs.length);
	}

	@Test
	public void testSearchTermIgnoreFirstLongDoc() throws IOException {
		String phrase = "Undoubtedly";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertEquals(String.format("Incorrect number of results were found for search phrase/term: %s", phrase),
				0, results.scoreDocs.length);
	}

	@Test
	public void testSearchTermMatchAfterCommaLCase() throws IOException {
		String phrase = "yet";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertEquals(String.format("Incorrect number of results were found for search phrase/term: %s", phrase),
				1, results.scoreDocs.length);

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("The first word after a comma is not being matched for the phrase/term: %s", phrase),
					d.get(field).equals("First word of this sentence will not be matched, yet the first word of this sentence will be matched."));
		}
	}

	/*
	 * Ensure that multi-term search is matched verbatim without slop
	 * i.e. "my term" matches "this is my term" but not "this is my work term" 
	 */
	@Test
	public void testSearchTermMatchNoSlop() throws IOException {
		String phrase = "slop term";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertEquals(String.format("Incorrect number of results were found for search phrase/term: %s", phrase),
				1, results.scoreDocs.length);

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("Multi-term search is matching with slop for the phrase/term: %s", phrase),
					d.get(field).endsWith("should match"));
		}
	}

	/*
	 * Ensure that multi-term search is matched verbatim without slop even when it contains hyphen
	 * i.e. "my-term" matches "this is my term" and "this is my-term" but not "this is my work term" 
	 */
	@Test
	public void testSearchTermMatchNoSlopHyphen() throws IOException {
		String phrase = "slop-hyphen-term";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertEquals(String.format("Incorrect number of results were found for search phrase/term: %s", phrase),
				2, results.scoreDocs.length);

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("Multi-term search with a hyphen is matching with slop for the phrase/term: %s", phrase),
					d.get(field).endsWith("will match"));
		}
	}

	/*
	 * Ensure that multi-term search is matched verbatim without slop even when it contains abbreviations
	 * i.e. "Dr. Pepper" matches "Dr. Pepper" but not "Dr. My Pepper" 
	 */
	@Test
	public void testSearchTermMatchNoSlopAbbrev() throws IOException {
		String phrase = "Mr. Term";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertEquals(String.format("Incorrect number of results were found for search phrase/term: %s", phrase),
				1, results.scoreDocs.length);

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("A multi-term search with an abbreviation is matching with slop for the phrase/term: %s", phrase),
					d.get(field).endsWith("will match"));
		}
	}

	@Test
	public void testSearchTermMatchWordAfterCommaCCase() throws IOException {
		String phrase = "Yet";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertEquals(String.format("Incorrect number of results were found for search phrase/term: %s", phrase), 
				1, results.scoreDocs.length);

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("The first word after a comma is not being matched for the phrase/term: %s", phrase),
					d.get(field).equals("First word of this sentence will not be matched, Yet the first word of this sentence will be matched."));
		}
	}

	/*
	 * Ensure that documents are returned in order based on their score
	 */
	@Test
	public void testSearchPhraseDocumentPriority() throws IOException {
		String phrase = "priority";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(String.format("No results found for search phrase/term: %s", phrase),
				0, results.scoreDocs.length);
		assertEquals(String.format("Incorrect number of results were found for search phrase: %s", phrase),
				5, results.scoreDocs.length);

		int i = 1;
		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("Results are not returned based on assigned priority for phrase/term: %s", phrase),
					d.get("title").startsWith("Test Doc 5 - " + i));
			
			i++;
		}
	}

}
