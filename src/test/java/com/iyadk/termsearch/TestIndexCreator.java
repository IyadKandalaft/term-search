/**
 * 
 */
package com.iyadk.termsearch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * @author iyad
 *
 */
public class TestIndexCreator extends TestCase {
	private String indexDir;
	private IndexCreator indexCreator;
	
	@BeforeClass
	public void setUp() throws IOException {
		indexDir = Files.createTempDirectory("lucene-index-test").toString();
		String corpusFile = TestSearchIndex.class.getClassLoader().getResource("test-corpus.txt").getFile().toString();
		/*
		 * Create a known index from the test-courpus.txt resource
		 */
		indexCreator = new IndexCreator(corpusFile, indexDir);
	}

	@Test
	public void testCreate() {
		assert true;
	}
	
	@AfterClass
	public void teadDown() throws IOException {
		System.out.println("Deleting index");
		Files.walk(Paths.get(indexDir)).map(Path::toFile).forEach(File::delete);
	}
}
