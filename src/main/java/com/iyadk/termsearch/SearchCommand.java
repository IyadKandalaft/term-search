package com.iyadk.termsearch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "search")
public class SearchCommand implements Callable<Integer> {
	@Option(names={"-i", "--index", "--index-path"}, 
			description="Path to read the index from", 
			defaultValue = "./lucene-index")
	private String indexDir;
	
	@Option(names={"-t", "--terms", "--search-terms-path"}, 
			description="Path to the file of search times", 
			defaultValue = "./terms.txt")
	private String termsFile;
	
	
	public Integer call() throws Exception {
		System.out.println("Executing search command");
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		
		System.out.println("Start time: " + dateFormatter.format(new Date()));
		System.out.println("-".repeat(80));
		
		
		SearchIndex indexSearcher = new SearchIndex(termsFile);
		indexSearcher.searchAll();
		
		System.out.println("End time: " + dateFormatter.format(new Date()));
		
		
		return 0;
	}

}
