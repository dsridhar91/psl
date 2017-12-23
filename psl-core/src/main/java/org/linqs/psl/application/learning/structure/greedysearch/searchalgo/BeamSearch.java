package org.linqs.psl.application.learning.structure.greedysearch.searchalgo;

import org.linqs.psl.application.ModelApplication;
import org.linqs.psl.application.learning.structure.greedysearch.clauseconstruction.ClauseConstructor;
import org.linqs.psl.application.learning.structure.greedysearch.scoring.WeightedPseudoLogLikelihood;
import org.linqs.psl.application.learning.weight.maxlikelihood.MaxPseudoLikelihood;
import org.linqs.psl.application.util.Grounding;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.config.Factory;
import org.linqs.psl.database.Database;
import org.linqs.psl.database.DataStore;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.application.learning.weight.TrainingMap;
import org.linqs.psl.application.groundrulestore.GroundRuleStore;
import org.linqs.psl.application.groundrulestore.MemoryGroundRuleStore;
import org.linqs.psl.model.rule.Rule;
import org.linqs.psl.model.rule.WeightedRule;
import org.linqs.psl.model.rule.logical.WeightedLogicalRule;
import org.linqs.psl.model.formula.Conjunction;
import org.linqs.psl.model.predicate.StandardPredicate;
import org.linqs.psl.model.predicate.Predicate;
import org.linqs.psl.model.formula.Formula;


import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Observable;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeamSearch extends Search{
	/**
	 * Prefix of property keys used by this class.
	 * 
	 * @see ConfigManager
	 */
	private static final Logger log = LoggerFactory.getLogger(BeamSearch.class);

	public static final String CONFIG_PREFIX = "structurelearning";

	public static final String BEAM_SIZE_KEY = CONFIG_PREFIX + ".beamsearch.beamsize";
	public static final int BEAM_SIZE_DEFAULT = 4;

	public static final String MAX_ITERATIONS_KEY = CONFIG_PREFIX + ".beamsearch.maxiter";
	public static final int MAX_ITERATIONS_DEFAULT = 3;

	public static final String INIT_RULE_WEIGHT_KEY = CONFIG_PREFIX + ".initweight";
	public static final double INIT_RULE_WEIGHT_DEFAULT = 5.0;

	public static final String SQUARED_POTENTIALS_KEY = CONFIG_PREFIX + ".squared";
	public static final boolean SQUARED_POTENTIALS_DEFAULT = true;

	protected int beamSize;
	protected int maxSearchIterations;
	protected double initRuleWeight;
	protected boolean useSquaredPotentials;

	public BeamSearch(Model model, Database rvDB, Database observedDB, ConfigBundle config, Set<Formula> unitClauses, Set<Predicate> targetPredicates, Set<Predicate> observedPredicates, Map<Predicate,Map<Integer,Set<String>>> predicateTypeMap, GroundRuleStore groundRuleStore) {
		super(model, rvDB, observedDB, config, unitClauses, targetPredicates, observedPredicates, predicateTypeMap, groundRuleStore);

		beamSize = config.getInt(BEAM_SIZE_KEY, BEAM_SIZE_DEFAULT);
		initRuleWeight = config.getDouble(INIT_RULE_WEIGHT_KEY, INIT_RULE_WEIGHT_DEFAULT);
		useSquaredPotentials = config.getBoolean(SQUARED_POTENTIALS_KEY, SQUARED_POTENTIALS_DEFAULT);
		maxSearchIterations = config.getInt(MAX_ITERATIONS_KEY,MAX_ITERATIONS_DEFAULT);

	}

	@Override
	protected Set<WeightedRule> doSearch(double startingScore){

		double previousBestGain = 0.0;
		double bestGain = 0.0;
		Formula bestClause = null;
		boolean reachedStoppingCondition = false;

		Set<Rule> modelRules = new HashSet<Rule>();
		for(Rule r : model.getRules()){
			modelRules.add(r);
		}

		Set<WeightedRule> bestRules = new HashSet<WeightedRule>();

		Set<Formula> beam = new HashSet<Formula>();
		for (Formula c : unitClauses){
			beam.add(c);
		}

		int iteration = 0;
		while((!reachedStoppingCondition) && (iteration < maxSearchIterations)){

			clConstr.createCandidateClauses(beam);
			Map<Formula,Double> currentClauseGains = new HashMap<Formula,Double>();
		
			while(clConstr.hasNext()) {
				WeightedRule candidateRule = clConstr.next();

				if(modelRules.contains(candidateRule)){
					continue;
				}

				log.warn("Current candidate rule:" + candidateRule);

				Map<WeightedRule,Double> currentModelWeightsMap = this.getRuleWeights();
				double currentModelScore = -100.00;

				this.model.addRule(candidateRule);

				try{
					this.mpll.learn();
					currentModelScore = this.wpll.scoreModel();
				}
				catch(Exception ex){
					System.out.println(ex);
					ex.printStackTrace();
				}

				double currentGain = currentModelScore - startingScore;

				if (currentGain > 0){
					Formula currentFormula = ((WeightedLogicalRule)candidateRule).getFormula();
					currentClauseGains.put(currentFormula, currentGain);	
				}

				model.removeRule(candidateRule);
				this.resetRuleWeights(currentModelWeightsMap);

				Grounding.removeRule(candidateRule, groundRuleStore);
			}

			if(currentClauseGains.size() == 0){
				break;
			}

			List<Formula> topClauses = getTopEntries(currentClauseGains);
			beam = new HashSet<Formula>();
			for (Formula f : topClauses){
				beam.add(f);
			}

			Formula currentBeamBestClause = topClauses.get(0);
			double currentBeamBestGain = currentClauseGains.get(currentBeamBestClause);

			previousBestGain = bestGain;

			if (currentBeamBestGain > bestGain){
				bestClause = currentBeamBestClause;
				bestGain = currentBeamBestGain;
				log.warn("Best Clause:" + bestClause);
				log.warn("Best Gain:" + bestGain);
			}
			if(Math.abs(previousBestGain - bestGain) <= 0.1){
				reachedStoppingCondition = true;
			}
			iteration++;
		}

		if(bestClause != null){
			WeightedRule bestRule = new WeightedLogicalRule(bestClause, initRuleWeight, useSquaredPotentials);
			bestRules.add(bestRule);
			Grounding.groundRule(bestRule, trainingMap, groundRuleStore);

		}
		
		return bestRules;

	}

	private List<Formula> getTopEntries(Map<Formula,Double> map) {
	    List<Map.Entry<Formula,Double>> list = new LinkedList<>(map.entrySet());
	    Collections.sort(list, new Comparator<Map.Entry<Formula,Double>>() {
	        @Override
	        public int compare(Map.Entry<Formula,Double> o1, Map.Entry<Formula,Double> o2) {
	            return (o1.getValue()).compareTo(o2.getValue());
	        }
	    });

	    List<Formula> rankedResults = new ArrayList<Formula>();

	    int startIndex = list.size() - 1;
	    int endIndex = Math.max(list.size() - beamSize, 0);

	    for(int i = startIndex; i >= endIndex; i--){
	    	rankedResults.add(list.get(i).getKey());
	    }
	    return rankedResults;
	}

	
}
