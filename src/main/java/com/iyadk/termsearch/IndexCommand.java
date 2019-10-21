package com.iyadk.termsearch;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "index")
public class IndexCommand implements Callable<Integer> {
	@Option(names={"-f", "--corpus"}, description="Path to the Corpus text to index", required=true)
	private String corpusFile;
	
	@Option(names={"-i", "--index", "--index-path"}, description="Path to create the index in")
	private String indexDir;

	public Integer call() throws Exception {
		System.out.println("Creating index");
		
		IndexCreator indexCreator = new IndexCreator(corpusFile);
		
		indexCreator.create();
		
		return 0;
	}

}
