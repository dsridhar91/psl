package org.linqs.psl.application.learning.structure.greedysearch.searchalgo;

import org.linqs.psl.application.ModelApplication;
import org.linqs.psl.application.learning.structure.greedysearch.clauseconstruction.ClauseConstructor;
import org.linqs.psl.application.learning.structure.greedysearch.scoring.WeightedPseudoLogLikelihood;
import org.linqs.psl.application.learning.weight.maxlikelihood.MaxPseudoLikelihood;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.config.Factory;
import org.linqs.psl.database.Database;
import org.linqs.psl.database.DataStore;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.WeightedRule;
import org.linqs.psl.model.rule.logical.WeightedLogicalRule;
import org.linqs.psl.model.formula.Conjunction;
import org.linqs.psl.model.predicate.StandardPredicate;
import org.linqs.psl.model.predicate.Predicate;
import org.linqs.psl.model.formula.Formula;


import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Observable;
import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeamSearch extends Search{
	/**
	 * Prefix of property keys used by this class.
	 * 
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "beamsearch";
	public static final String BEAM_SIZE_KEY = CONFIG_PREFIX + ".beamsize";
	public static final int BEAM_SIZE_DEFAULT = 10;

	protected int beamSize;

	public BeamSearch(Model model, Database rvDB, Database observedDB, ConfigBundle config, Set<Formula> unitClauses, Set<Predicate> targetPredicates, Set<Predicate> observedPredicates) {
		super(model, rvDB, observedDB, config, unitClauses, targetPredicates, observedPredicates);
		beamSize = config.getInt(BEAM_SIZE_KEY, BEAM_SIZE_DEFAULT);
	}

	@Override
	protected Set<WeightedRule> doSearch(double startingScore){

		double previousBestGain = 0.0;
		double bestGain = 0.0;
		Formula bestClause = null;
		boolean reachedStoppingCondition = false;

		Set<Formula> beam = new HashSet<Formula>();
		for (Formula c : unitClauses){
			beam.add(c);
		}

		while(!reachedStoppingCondition){

			Set<Formula> candidateClauses = clConstr.createCandidateClauses(beam);
			Map<Formula,Double> currentClauseGains = new HashMap<Formula,Double>();
		
			for(Formula cc : candidateClauses){

				WeightedRule candidateRule = new WeightedLogicalRule(cc);
				Map<WeightedRule,Double> currentModelWeightsMap = this.getRuleWeights();

				this.model.addRule(candidateRule);
				this.mpll.learn();

				double currentModelScore = this.wpll.scoreModel();
				double currentGain = currentModelScore - startingScore;

				if (currentGain > 0){
					currentClauseGains.put(cc, currentGain);	
				}
				
				model.removeRule(candidateRule);
				this.resetRuleWeights(currentModelWeightsMap);
			}

			List<Formula> topClauses = getTopEntries(currentClauseGains);
			beam = new HashSet<Formula>();
			for (Formula f : topClauses){
				beam.add(f);
			}

			Formula currentBeamBestClause = topClauses.get(0);
			double currentBeamBestGain = currentClauseGains.get(currentBeamBestClause);

			if (currentBeamBestGain > bestGain){
				bestClause = currentBeamBestClause;
				previousBestGain = bestGain;
				bestGain = currentBeamBestGain;
			}

			if(previousBestGain == bestGain || beam.size() == 0){
				reachedStoppingCondition = true;
			}
		}

		Set<WeightedRule> bestRules = new HashSet<WeightedRule>();
		bestRules.add(new WeightedLogicalRule(bestClause));
		return bestRules;

	}

	private List<Formula> getTopEntries(Map<Formula,Double> map) {
	    List<Map.Entry<K,V>> list = new LinkedList<>(map.entrySet());
	    Collections.sort( list, new Comparator<Map.Entry<K, V>>() {
	        @Override
	        public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
	            return (o1.getValue()).compareTo(o2.getValue());
	        }
	    });

	    List<Formula> topKResult = new ArrayList<Formula>();
	    for(int i = k; i >= 0; i--){
	    	topKResult.add(list.get(i).getKey());
	    }
	    return topKresult;
	}
}
