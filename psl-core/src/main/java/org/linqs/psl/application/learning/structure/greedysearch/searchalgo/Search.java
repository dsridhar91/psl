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
import org.linqs.psl.model.predicate.Predicate;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.WeightedRule;
import org.linqs.psl.model.predicate.StandardPredicate;
import org.linqs.psl.model.formula.Conjunction;
import org.linqs.psl.model.formula.Formula;
import org.linqs.psl.model.weight.Weight;


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


public abstract class Search extends Observable implements ModelApplication
{

	/**
	 * Prefix of property keys used by this class.
	 * 
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "search";

	protected Model model;
	protected Database rvDB, observedDB;
	protected ConfigBundle config;
	protected ClauseConstructor clConstr;
	protected MaxPseudoLikelihood mpll;
	protected WeightedPseudoLogLikelihood wpll;
	protected Set<Formula> unitClauses;
	protected Set<Predicate> targetPredicates;
	protected Set<Predicate> observedPredicates;

	public Search(Model model, Database rvDB, Database observedDB, ConfigBundle config, Set<Formula> unitClauses, Set<Predicate> targetPredicates, Set<Predicate> observedPredicates) {
		this.model = model;
		this.rvDB = rvDB;
		this.observedDB = observedDB;
		this.config = config;
		this.unitClauses = unitClauses;
		this.targetPredicates = targetPredicates;
		this.observedPredicates = observedPredicates;

		mpll = new MaxPseudoLikelihood(model, rvDB, observedDB, config);
		wpll = new WeightedPseudoLogLikelihood(model, rvDB, observedDB, config);

	}

	public Set<WeightedRule> search(double startingScore){

		initClauseConstruction();
		Set<WeightedRule> rules = doSearch(startingScore);
		return rules;

	}


	protected void initClauseConstruction(){

		clConstr = new ClauseConstructor(targetPredicates, observedPredicates);
	}


	private Map<WeightedRule,Double> getRuleWeights(){
		Map<WeightedRule,Double> ruleWeightMap = HashMap<WeightedRule,Double>();
		for(Rule r : model.getRules()){
			double ruleWeight = r.getWeight().getWeight();
			ruleWeightMap.put(r, ruleWeight);
		}

	}

	private void resetRuleWeights(Map<WeightedRule,Double> ruleWeightMap){
		for(Rule r : model.getRules()){
			double ruleWeight = ruleWeightMap.get(r);
			Weight w = new PositiveWeight(ruleWeight);
			r.setWeight(w);
		}
	}



	protected abstract Set<WeightedRule> doSearch(double startingScore);


	@Override
	public void close() {
		model = null;
		rvDB = null;
		config = null;
	}


}
