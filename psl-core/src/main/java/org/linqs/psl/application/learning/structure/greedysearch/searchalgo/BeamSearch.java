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

	public BeamSearch(Model model, Database rvDB, Database observedDB, ConfigBundle config, Set<Conjunction> unitClauses, Set<Predicate> targetPredicates, Set<Predicate> observedPredicates) {
		super(model, rvDB, observedDB, config, unitClauses, targetPredicates, observedPredicates);
	}

	@Override
	public Set<WeightedRule> search(double startingScore){

		Set<WeightedRule> bestRules = new HashSet<WeightedRule>();

		Set<Conjunction> beam = new HashSet<Conjunction>();
		for (Conjunction c : unitClauses){
			beam.add(c);
		}


		Conjunction bestClause = null;
		Set<Conjunction> candidateClauses = this.clConstr.createCandidateClauses(beam);
		for (Conjunction c: candidateClauses) {
			bestClause = c;
		      	break;
		}	       
		WeightedRule bestRule = new WeightedLogicalRule(bestClause, 1.0, true);
		bestRules.add(bestRule);

		return bestRules;


	}
 


}
