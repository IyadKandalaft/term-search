package com.iyadk.termsearch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/*
 * search subcommand that is used to trigger searches of a lucene index based on a set of terms 
 */
@Command(name = "search")
public class SearchCommand implements Callable<Integer> {
	@Option(names={"-i", "--index", "--index-path"}, 
			description="Path to read the index from", 
			defaultValue = "./lucene-index",
			paramLabel="INDEXPATH")
	private String indexDir;
	
	@Option(names={"-t", "--terms", "--search-terms-path"}, 
			description="Path to the terms file to query for", 
			defaultValue = "./terms.txt",
			paramLabel="FILE")
	private String termsFile;

	@Option(names={"-o", "--output", "--output-file"},
			description="Path to the file where the output will be written",
			defaultValue="./output/results.tsv",
			paramLabel="FILE")
	private String outputFile;
	
	@Option(names={"-m", "--match-limit"},
			description="Maximum number of matches to return",
			defaultValue="100",
			type=Integer.class, 
			paramLabel="NUM")
	private Integer matchLimit;
	
	@Option(names={"-h", "--highlight-limit"},
			description="Maximum number of characters to return surrounding a matched term",
			defaultValue="500",
			type=Integer.class, 
			paramLabel="NUM")
	private Integer highlightLimit;
	
	@Option(names={"--threads"},
			description="Number of threads/cores to use for searching",
			defaultValue="4",
			type=Integer.class, 
			paramLabel="NUM")
	private Integer threads;
	
	public Integer call() throws Exception {
		System.out.println("Executing search command");
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
		System.out.println("Start time: " + dateFormatter.format(new Date()));
		System.out.println("-------------------------------------------------------");
		
		SearchIndex indexSearcher = new SearchIndex(termsFile, outputFile);
		indexSearcher.searchAll("content");
		indexSearcher.setSearchLimit(matchLimit);
		indexSearcher.setHighlightLimit(highlightLimit);
		indexSearcher.setNumThreads(threads);

		indexSearcher.close();

		System.out.println("End time: " + dateFormatter.format(new Date()));
		
		return 0;
	}

}
