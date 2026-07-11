/**
 * 
 */
package org.tedros.ai.domain;

/**
 * @author Davis Gordon
 *
 */
public interface DomainApp {

	static final String SEPARATOR = "_";
	static final String VIEW = "VIEW";
	static final String MODULE = "MODULE";
	static final String FORM = "FORM";
	
	static final String TEROS_AGENT = "TEROS_AGENT";
	
	static final String SEP = SEPARATOR;
	
	static final String MNEMONIC = "AI";
	
	static final String TEROS_AGENT_MODULE_ID = MNEMONIC + SEP + TEROS_AGENT + SEP + MODULE;
	static final String TEROS_AGENT_FORM_ID = MNEMONIC + SEP + TEROS_AGENT + SEP + FORM;
	static final String TEROS_AGENT_VIEW_ID = MNEMONIC + SEP + TEROS_AGENT + SEP + VIEW;
	

}
