package com.iyadk.termsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;

public class SearchIndex {
	private Path termsPath;
	private Path indexPath;
	private Directory dirIndex;
	
	public SearchIndex(String terms) throws IOException {
		this(terms, "./lucene-index");
	}
	
	public SearchIndex(String terms, String index) throws IOException {
		termsPath = Paths.get(terms);
		indexPath = Paths.get(index);
		dirIndex = MMapDirectory.open(indexPath);
	}
	
	public void searchAll() throws IOException, ParseException {
		IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
		IndexSearcher searcher = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer();
		
	    try ( BufferedReader in = Files.newBufferedReader(termsPath, StandardCharsets.UTF_8) ){
		    QueryParser parser = new QueryParser("content", analyzer);
		    String line;

		    while ( (line = in.readLine()) != null ) {
		    	if ( line.trim().length() == 0)
		    		continue;
		    	
		    	Query query = parser.parse(line);
		    	
				System.out.println("-".repeat(80));
		    	System.out.println("Searching for: " + query.toString());
				System.out.println("-".repeat(80));

		    	TopDocs searchResults = searcher.search(query, 100);
		    	
				for (ScoreDoc sd : searchResults.scoreDocs)
			    {
			        Document d = searcher.doc(sd.doc);
			        System.out.println("Document Name : " + d.get("title")
			                    + "  :: Content : " + d.get("content")
			                    + "  :: Score : " + sd.score);
			    }

		    	
		        System.out.println("Total Results :: " + searchResults.totalHits);
		    	
		    }
	    }
	    reader.close();		
	}
}
