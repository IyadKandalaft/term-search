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
	
	@Option(names={"-ml", "--match-limit"},
			description="Maximum number of matches to review (default: ${DEFAULT-VALUE})",
			defaultValue="20",
			type=Integer.class,
			paramLabel="NUM")
	private Integer matchLimit;

	@Option(names={"-rl", "--result-limit"},
			description="Maximum number of matches to return (default: ${DEFAULT-VALUE})",
			defaultValue="5",
			type=Integer.class,
			paramLabel="NUM")
	private Integer resultLimit;

	@Option(names={"-sl", "--source-limit"},
			description="Maximum number of matches to return per source (default: ${DEFAULT-VALUE})",
			defaultValue="130",
			type=Integer.class,
			paramLabel="NUM")
	private Integer sourceLimit;

	@Option(names={"-hs", "--highlight-min"},
			description="Minimum number of characters to return surrounding a matched term (default: ${DEFAULT-VALUE})",
			defaultValue="70",
			type=Integer.class,
			paramLabel="NUM")
	private Integer highlightMin;

	@Option(names={"-hx", "--highlight-max"},
			description="Maximum number of characters to return surrounding a matched term (default: ${DEFAULT-VALUE})",
			defaultValue="180",
			type=Integer.class,
			paramLabel="NUM")
	private Integer highlightMax;

	@Option(names={"-s", "--scoring", "--scoring-formula"},
			description="Result scoring formula. (default: ${DEFAULT-VALUE})\n"
					+ "Computed vars: _score (lucene search score), score (parsed doc score), & docMatchCount (# of times doc has matched)\n"
					+ "Functions: abs, cbrt, ceil, cos, exp, floor, log, max, min, pow, random, round, sin, sqrt, tan, trunc",
			defaultValue="_score/score",
			type=String.class
			)
	private String scoring;
	
	@Option(names={"-e", "--explain", "--explain-scoring"},
			description="Provides an explanation of results by providing the considered excerpts and their score\n",
			defaultValue="false",
			type=Boolean.class
			)
	private Boolean explainScoring;

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

		// TODO: Check if index exists and exit gracefully
		SearchIndex indexSearcher = new SearchIndex(termsFile, outputFile, indexDir);

		indexSearcher.setMatchLimit(matchLimit);
		indexSearcher.setResultLimit(resultLimit);
		indexSearcher.setSourceLimit(sourceLimit);
		indexSearcher.setHighlightLimit(highlightMin, highlightMax);
		indexSearcher.setNumThreads(threads);
		indexSearcher.setScoring(scoring);
		indexSearcher.setExplainScoring(explainScoring);
		indexSearcher.searchAll("content");

		indexSearcher.close();

		System.out.println("End time: " + dateFormatter.format(new Date()));
		
		return 0;
	}

}
