package com.iyadk.termsearch;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.regex.PatternSyntaxException;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "index")
public class IndexCommand implements Callable<Integer> {
	@Option(names={"-f", "--corpus"}, 
			description="Path to the Corpus text to index ", 
			required=true)
	private String corpusFile;
	
	@Option(names={"-i", "--index", "--index-path"}, 
			description="Path to create the index in (default: ${DEFAULT-VALUE})", 
			defaultValue="./lucene-index")
	private String indexDir;
	
	@Option(names={"-d", "--delimeter"},
			description="Delimeter (regex) used to split corpus lines into document titles and content (default: ${DEFAULT-VALUE})",
			defaultValue=".txt:")
	private String delimeter;
	
	@Option(names={"-s", "--score-offset"},
			description="TSV file w/ 2 columns containing a regex and a multiplying factor that offsets document scores that match the regex")
	private File offsetLookupFile;
	
	@Option(names={"--threads"},
			description="Number of threads/cores to use for indexing (default: ${DEFAULT-VALUE})",
			defaultValue="4")
	private int threads;

    @Option(names = { "-h", "--help" },
    		usageHelp = true,
    		description = "Displays this message")
    private boolean helpRequested = false;

	public Integer call() throws Exception {
		System.out.println("Creating index");

		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		if (Files.exists(Paths.get(indexDir))) {
			System.out.printf("Removing existing lucene index:%s\n", indexDir);
			Files.walk(Paths.get(indexDir)).map(Path::toFile).forEach(File::delete);
		}

		IndexCreator indexCreator = new IndexCreator(corpusFile, indexDir);

		indexCreator.setDelimeter(delimeter);
		indexCreator.setNumThreads(threads);
		try {
			if (offsetLookupFile != null ) {
				System.out.println("Parsing score offset file.");
				indexCreator.setOffsetLookup(offsetLookupFile);
			}
		} catch (FileNotFoundException e) {
			System.out.printf("The offset lookup file %s does not exist.", offsetLookupFile);
			return 1;
		} catch (PatternSyntaxException e) {
			System.out.printf("The offset lookup file contains an invalid regex.\n %s", e);
			return 1;
		} catch (NumberFormatException e) {
			System.out.printf("The offset lookup file contains an invalid score offset.\n %s", e);
			return 1;
		}

		System.out.println("Start time: " + dateFormatter.format(new Date()));
		System.out.println("-------------------------------------------------------");
		
		try {
			indexCreator.create();
		} catch (FileNotFoundException e) {
			System.out.printf("The corpus file %s does not exist.", corpusFile);
			return 1;
		}

		System.out.println("End time: " + dateFormatter.format(new Date()));

		return 0;
	}

}