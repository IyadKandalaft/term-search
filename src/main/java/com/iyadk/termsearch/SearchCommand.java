package com.iyadk.termsearch;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(name = "search")
public class SearchCommand implements Callable<Integer> {

	public Integer call() throws Exception {
		System.out.println("Search command");
		return 0;
	}

}
