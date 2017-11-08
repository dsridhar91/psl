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

	public BeamSearch(Model model, Database rvDB, Database observedDB, ConfigBundle config, Set<Formula> unitClauses, Set<Predicate> targetPredicates, Set<Predicate> observedPredicates) {
		super(model, rvDB, observedDB, config, unitClauses, targetPredicates, observedPredicates);
	}

	@Override
	protected Set<WeightedRule> doSearch(double startingScore){

		Set<WeightedRule> bestRules = new HashSet<WeightedRule>();

		Set<Formula> beam = new HashSet<Formula>();
		for (Formula c : unitClauses){
			beam.add(c);
		}


		Formula bestClause = null;
		Set<Formula> candidateClauses = clConstr.createCandidateClauses(beam);
		for (Formula c: candidateClauses) {
			bestClause = c;
		    break;
		}	       
		WeightedRule bestRule = new WeightedLogicalRule(bestClause, 1.0, true);
		bestRules.add(bestRule);

		return bestRules;


	}
 


}
