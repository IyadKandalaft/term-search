/**
 * Formats output using regex rules
 */
package com.iyadk.termsearch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
/**
 * @author iyad
 *
 */
public class OutputFormatter {

	private HashMap<String, ArrayList<FormattingRule>> ruleGroups;
	/**
	 * 
	 */
	public OutputFormatter() {
		// TODO Auto-generated constructor stub
		this.ruleGroups = new HashMap<>();
	}

	/**
	 * Add a formatti;ng rule to the list for the specified group.
	 * Each group contains a list of formatting rules that will be applied sequentially 
	 * @param group The group that the formatting rule will be added to
	 * @param matchPattern
	 * @param replacement
	 * @param stopAfter
	 */
	
	public void addRule(String group, String matchPattern, String replacement, boolean stopAfter) {
		FormattingRule rule = new FormattingRule(matchPattern, replacement, stopAfter);
		addRule(group, rule);
	}
	
	/**
	 * 	
	 * 
	 * @param group
	 * @param regex
	 * @return
	 */
	public void addRule(String group, FormattingRule rule) {
		ArrayList<FormattingRule> ruleGroup = ruleGroups.get(group);
		
		if (ruleGroup == null) {
			ruleGroup = addGroup(group);
		}
		ruleGroup.add(rule);
	}
	
	/**
	 * Add a group to apply a list of formatting rules to
	 * 
	 * @param group
	 * @return
	 */
	public ArrayList<FormattingRule> addGroup(String group) {
		return ruleGroups.put(group, new ArrayList<FormattingRule>());
	}
	
	public ArrayList<FormattingRule> getGroup(String group){
		return ruleGroups.get(group);
	}
	
	

	
}

