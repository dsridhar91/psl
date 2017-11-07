package org.linqs.psl.application.learning.structure.greedysearch;

import org.linqs.psl.application.learning.structure.StructureLearningApplication;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.model.Model;
import org.linqs.psl.database.Database;
import org.linqs.psl.model.formula.Conjunction;
import org.linqs.psl.model.formula.Formula;
import org.linqs.psl.model.formula.Implication;
import org.linqs.psl.model.atom.QueryAtom;
import org.linqs.psl.model.term.Variable;
import org.linqs.psl.model.predicate.Predicate;
import org.linqs.psl.application.learning.structure.greedysearch.searchalgo.BeamSearch;
import org.linqs.psl.application.learning.structure.greedysearch.searchalgo.Search;
import org.linqs.psl.application.learning.structure.greedysearch.scoring.Scorer;
import org.linqs.psl.application.learning.structure.greedysearch.scoring.WeightedPseudoLogLikelihood;
import org.linqs.psl.model.rule.WeightedRule;
import org.linqs.psl.model.rule.logical.WeightedLogicalRule;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Observable;


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
	public static final String CLAUSE_CONSTRUCTOR_KEY = CONFIG_PREFIX + ".clauseconstructor";

	public static final String MAX_ITERATIONS_KEY = CONFIG_PREFIX + ".maxiter";
	public static final int MAX_ITERATIONS_DEFAULT = 10;

	protected int maxIterations;
	
	public TopDownStructureLearning(Model model, Database rvDB, Database observedDB, ConfigBundle config, Set<Predicate> targetPredicates, Set<Predicate> observedPredicates) {
		super(model, rvDB, observedDB, config, targetPredicates, observedPredicates);
		//TODO: implement!
		maxIterations = config.getInteger(MAX_ITERATIONS_KEY, MAX_ITERATIONS_DEFAULT);

	}
	
	
	@Override
	protected void doStructureLearn() {

		double initScore = 0.0;
		Set<Formula> unitClauses = getUnitClauses();
		Search searchAlgorithm = new BeamSearch(model, rvDB, observedDB, config, unitClauses, targetPredicates, observedPredicates);
		Scorer scorer = new WeightedPseudoLogLikelihood(model, rvDB, observedDB, config);

		int iter = 0;
		while(iter < maxIterations){

			Set<WeightedRule> clauses = searchAlgorithm.doSearch(initScore);

			if(clauses.isEmpty()){
				break;
			}

			for (WeightedRule r : clauses){
				model.addRule(r);	
			}

			//TODO: Do weight learning
			try{
				initScore = scorer.scoreModel();	
			}
			catch(Exception ex){
				System.out.println(ex);
				ex.printStackTrace();
			}
			
			iter++;
		}

		
	}

	private Set<Formula> getUnitClauses(){

		Set<Formula> unitClauses = new HashSet<Formula>();

		for (Predicate p : targetPredicates){
			int arity = p.getArity();
			Variable[] arguments = new Variable[arity];
			for(int i = 0; i < arity; i++){
				System.out.println(String.valueOf((char)(i+65)));
				arguments[i] = new Variable(String.valueOf((char)(i+65)));
			}

			Formula unitClause = new QueryAtom(p, arguments);
			unitClauses.add(unitClause);

		}
		return unitClauses;
	}

	
	
	
	
}
