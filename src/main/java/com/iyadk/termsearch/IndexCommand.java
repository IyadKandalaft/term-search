package com.iyadk.termsearch;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "index")
public class IndexCommand implements Callable<Integer> {
	@Option(names={"-f", "--corpus"}, 
			description="Path to the Corpus text to index", 
			required=true)
	private String corpusFile;
	
	@Option(names={"-i", "--index", "--index-path"}, 
			description="Path to create the index in", 
			defaultValue="./lucene-index")
	private String indexDir;
	
	@Option(names={"-d", "--delimeter"},
			description="Delimeter (regex) to use to split corpus lines into document titles and content",
			defaultValue=".txt:")
	private String delimeter;

	public Integer call() throws Exception {
		System.out.println("Creating index");

		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		System.out.println("Start time: " + dateFormatter.format(new Date()));
		System.out.println("-------------------------------------------------------");

		IndexCreator indexCreator = new IndexCreator(corpusFile, indexDir);
		
		indexCreator.setDelimeter(delimeter);

		try {
			indexCreator.create();
		} catch (FileNotFoundException e) {
			System.out.printf("The corpus file %s does not exist.", corpusFile);
		}

		System.out.println("End time: " + dateFormatter.format(new Date()));

		return 0;
	}

}
