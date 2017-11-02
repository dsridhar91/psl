package org.linqs.psl.application.learning.structure.greedysearch;

import org.linqs.psl.application.learning.structure.StructureLearningApplication;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.model.Model;
import org.linqs.psl.database.Database;


/**
 * Abstract class for learning the structure of
 * {@link WeightedRule CompatibilityRules} in a {@link Model}
 * from data.
 *
 * @author Golnoosh Farnadi <gfarnadi@ucsc.edu>
 */


public class TopDownStructureLearning  extends StructureLearningApplication {
	
	
	/**
	 * Prefix of property keys used by this class.
	 *
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "topdownstructurelearning";
	
	
	public static final String REASONER_KEY = CONFIG_PREFIX + ".reasoner";
	public static final String WEIGHT_LEARNING_KEY = CONFIG_PREFIX + ".weightlearning";
	public static final String SEARCH_ALGO_KEY = CONFIG_PREFIX + ".searchalgo";
	public static final String SCORING_KEY = CONFIG_PREFIX + ".scoring";
	public static final String CLAUSE_CONSTRUCTOR_KEY = CONFIG_PREFIX + ".clausecunstructor";
	
	public TopDownStructureLearning(Model model, Database rvDB, Database observedDB, ConfigBundle config) {
		super(model, rvDB, observedDB, config);
		//TODO: implement!

	}
	
	
	@Override
	protected void doStructureLearn() {
		//TODO: implement!
	}
	
	
	
	
}
