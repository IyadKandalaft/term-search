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
			description="Path to read the index from (default: ${DEFAULT-VALUE})", 
			defaultValue = "./lucene-index",
			paramLabel="INDEXPATH")
	private String indexDir;
	
	@Option(names={"-t", "--terms", "--search-terms-path"}, 
			description="Path to file containing terms to query (default: ${DEFAULT-VALUE})", 
			defaultValue = "./terms.txt",
			paramLabel="FILE")
	private String termsFile;

	@Option(names={"-o", "--output", "--output-file"},
			description="Path to the file where the output will be written (default: ${DEFAULT-VALUE})",
			defaultValue="./output/results.tsv",
			paramLabel="FILE")
	private String outputFile;
	
	@Option(names={"-m", "--match-limit"},
			description="Maximum number of matches to return (default: ${DEFAULT-VALUE})",
			defaultValue="100",
			type=Integer.class, 
			paramLabel="NUM")
	private Integer matchLimit;
	
	@Option(names={"-l", "--highlight-limit"},
			description="Maximum number of characters to return surrounding a matched term (default: ${DEFAULT-VALUE})",
			defaultValue="500",
			type=Integer.class,
			paramLabel="NUM")
	private Integer highlightLimit;
	
	@Option(names={"--threads"},
			description="Number of threads/cores to use for searching (default: ${DEFAULT-VALUE})",
			defaultValue="4",
			type=Integer.class,
			paramLabel="NUM")
	private Integer threads;
	
    @Option(names = { "-h", "--help" },
    		usageHelp = true,
    		description = "Displays this message")
    private boolean helpRequested = false;
	
	public Integer call() throws Exception {
		System.out.println("Executing search command");
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
		System.out.println("Start time: " + dateFormatter.format(new Date()));
		System.out.println("-------------------------------------------------------");
		
		SearchIndex indexSearcher = new SearchIndex(termsFile, outputFile);
		
		indexSearcher.setSearchLimit(matchLimit);
		indexSearcher.setHighlightLimit(highlightLimit);
		indexSearcher.setNumThreads(threads);
		
		indexSearcher.searchAll("content");

		indexSearcher.close();

		System.out.println("End time: " + dateFormatter.format(new Date()));
		
		return 0;
	}

}
