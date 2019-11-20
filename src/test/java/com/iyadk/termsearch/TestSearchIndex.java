package com.iyadk.termsearch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestSearchIndex {

	String indexDir;
	String termsFile;
	SearchIndex indexSearcher;

	// Field to search for most tests
	String field = "content";
	
	@BeforeEach
	void setUp() throws Exception {
		indexDir = Files.createTempDirectory("lucene-index-test").toString();

		String corpusFile = getClass().getClassLoader().getResource("test-corpus.txt").getFile().toString();
		termsFile = getClass().getClassLoader().getResource("test-terms.txt").getFile().toString();

		/*
		 * Create a known index from the test-courpus.txt resource
		 */
		IndexCreator indexCreator = new IndexCreator(corpusFile, indexDir);
		indexCreator.create();

		File outputFile = Paths.get("./output.tsv").toFile();

		try {
			indexSearcher = new SearchIndex(termsFile, outputFile.toString(), indexDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@AfterEach
	void tearDown() throws Exception {

	}

	@Test
	void testSearchTermLCase() throws IOException {
		String phrase = "cookie";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(0, results.scoreDocs.length,
				String.format("No results found for search phrase/term: %s", phrase));
		assertEquals(1, results.scoreDocs.length,
				String.format("Incorrect number of results were found for search phrase: %s", phrase));

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			// Lowecase should match
			assertTrue(String.format("Lowecase result was not matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text that will match the term: cookie"));
			// Camelcase should not match
			assertFalse(String.format("CamelCase result matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text that will match the term: Cookie"));
			// Uppercase should not match
			assertFalse(String.format("Uppercase result matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text that will match the term: COOKIE"));
		}
	}

	@Test
	void testSearchTermCCase() throws IOException {
		String phrase = "Cookie";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(0, results.scoreDocs.length,
				String.format("No results found for search phrase/term: %s", phrase));
		assertEquals(1, results.scoreDocs.length,
				String.format("Incorrect number of results were found for search phrase: %s", phrase));

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			// Camelcase should match
			assertTrue(String.format("CamelCase results was not matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text that will match the term: Cookie"));
			// Lowecase should not match
			assertFalse(String.format("Lowercase result matched found for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text that will match the term: cookie"));
			// Uppercase should match
			assertFalse(String.format("Uppercase result matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text that will match the term: COOKIE"));
		}
	}

	@Test
	void testSearchTermUCase() throws IOException {
		String phrase = "COOKIE";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(0, results.scoreDocs.length,
				String.format("No results found for search phrase/term: %s", phrase));
		assertEquals(1, results.scoreDocs.length,
				String.format("Incorrect number of results were found for search phrase: %s", phrase));

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			// Uppercase should match
			assertTrue(String.format("Uppercase result was not matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text that will match the term: COOKIE"));
			// Lowecase should not match
			assertFalse(String.format("Lowercase result matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text that will match the term: cookie"));
			// Camelcase should not match
			assertFalse(String.format("CamelCase result matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text that will match the term: Cookie"));
		}
	}

	@Test
	void testSearchPhraseLCase() throws IOException {
		String phrase = "two words";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(0, results.scoreDocs.length,
				String.format("No results found for search phrase/term: %s", phrase));
		assertEquals(1, results.scoreDocs.length,
				String.format("Incorrect number of results were found for search phrase: %s", phrase));

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("Lower case result was not matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text where a term is composed of two words"));
			assertFalse(String.format("Uppercase phrase was matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text where a term is composed of Two Words with capitals"));
		}
	}

	@Test
	void testSearchPhraseUCase() throws IOException {
		String phrase = "Two Words";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(0, results.scoreDocs.length,
				String.format("No results found for search phrase/term: %s", phrase));
		assertEquals(1, results.scoreDocs.length,
				String.format("Incorrect number of results were found for search phrase: %s", phrase));

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertFalse(String.format("Lower case result was matched for phrase/term: %s", phrase),
					d.get(field).contentEquals("This is sample text where a term is composed of two words"));
			assertTrue(String.format("Uppercase phrase was matched for phrase/term: %s", phrase), d.get(field)
					.contentEquals("This is sample text where a term is composed of Two Words with capitals"));
		}
	}

	@Test
	void testSearchInsideQuotes() throws IOException {
		String phrase = "quote";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(0, results.scoreDocs.length,
				String.format("No results found for search phrase/term: %s", phrase));

		assertEquals(3, results.scoreDocs.length,
				String.format("Incorrect number of results were found for search phrase: %s", phrase));

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
	void testSearchPhraseHyphen() throws IOException {
		String phrase = "hyphen";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(0, results.scoreDocs.length,
				String.format("No results found for search phrase/term: %s", phrase));
		assertTrue(String.format("More than two results were found for search phrase: %s", phrase),
				(results.scoreDocs.length < 3));

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("Result without a hyphen was matched for phrase/term: %s", phrase),
					d.get(field).equals("This is sample text where a term is suffixed by a hyphen-term")
							|| d.get(field).equals("This is sample text where a term is prefixed by a term-hyphen"));
		}
	}
	
	@Test
	void testSearchPhraseIgnoreFirstWord() throws IOException {
		String phrase = "My";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(0, results.scoreDocs.length,
				String.format("No results found for search phrase/term: %s", phrase));
		assertEquals(1, results.scoreDocs.length,
				String.format("Incorrect number of results were found for search phrase: %s", phrase));

		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("Result without a hyphen was matched for phrase/term: %s", phrase),
					d.get(field).equals("My sample text where a the first term My is not matched but the second one is"));
		}
	}
	
	/*
	 * Ensure that documents are returned in order based on their score
	 */
	@Test
	void testSearchPhraseDocumentPriority() throws IOException {
		String phrase = "priority";

		TopDocs results = indexSearcher.searchPhrase(phrase, field);

		assertNotEquals(0, results.scoreDocs.length,
				String.format("No results found for search phrase/term: %s", phrase));
		assertEquals(5, results.scoreDocs.length,
				String.format("Incorrect number of results were found for search phrase: %s", phrase));

		int i = 1;
		for (ScoreDoc sd : results.scoreDocs) {
			Document d = indexSearcher.searcher.doc(sd.doc);

			assertTrue(String.format("Result without a hyphen was matched for phrase/term: %s", phrase),
					d.get("title").startsWith("Test Doc 5 - " + i));
			
			i++;
		}
	}

}
