package org.linqs.psl.application.learning.structure.greedysearch.searchalgo;

import org.linqs.psl.application.ModelApplication;
import org.linqs.psl.application.learning.structure.greedysearch.clauseconstruction.ClauseConstructor;
import org.linqs.psl.application.learning.structure.greedysearch.scorer.WeightedPseudoLogLikelihood;
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
import org.linqs.psl.model.formula.Conjunction;
import org.linqs.psl.model.predicate.StandardPredicate;



import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeamSearch extends Search{
	/**
	 * Prefix of property keys used by this class.
	 * 
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "beamsearch";

	public BeamSearch(Model model, Database rvDB, Database observedDB, ConfigBundle config, double startingScore, DataStore data, Set<Conjunction> unitClauses) {
		super(model, rvDB, observedDB, config, startingScore, data, unitClauses);
	}

	@Override
	public abstract Set<WeightedRule> doSearch(){

		Set<WeightedRule> bestRules = new HashSet<WeightedRule>();

		Set<Conjunction> beam = new HashSet<Conjunction>();
		for (Conjunction c : unitClauses){
			beam.add(c);
		}


		Set<Conjunction> candidateClauses = this.clConstr.createCandidateClauses(beam);
		Conjunction bestClause = candidateClauses.get(0);
		WeightedRule bestRule = new WeightedRule(bestClause, 1.0, true);
		bestRules.add(bestRule);

		return bestRules;


	}
 


}