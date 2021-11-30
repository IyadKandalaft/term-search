package com.iyadk.termsearch;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "termsearch", subcommands = {SearchCommand.class, IndexCommand.class})
public class TermSearchCommand implements Callable<Integer> {
	@Option(names={"-v", "--verbose"}, description="Verbose output")
	private boolean verbose;
	
	@Option(names={"-h", "--help"}, description="Display help/usage.", usageHelp=true)
	boolean help;
	
	public static void main(String[] args) {
		CommandLine cmd = new CommandLine(new TermSearchCommand());
		System.exit(cmd.execute(args));
	}

	public Integer call() throws Exception {
		return 0;
	}
}