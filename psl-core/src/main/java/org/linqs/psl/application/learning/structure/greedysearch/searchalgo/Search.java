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
import org.linqs.psl.model.rule.Rule;
import org.linqs.psl.model.predicate.StandardPredicate;
import org.linqs.psl.model.formula.Conjunction;
import org.linqs.psl.model.formula.Formula;
import org.linqs.psl.model.weight.Weight;
import org.linqs.psl.model.weight.PositiveWeight;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.application.learning.weight.TrainingMap;
import org.linqs.psl.application.groundrulestore.GroundRuleStore;


import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
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
	protected TrainingMap trainingMap;
	protected GroundRuleStore groundRuleStore;
	protected Database rvDB, observedDB;
	protected ConfigBundle config;
	
	protected ClauseConstructor clConstr;
	protected MaxPseudoLikelihood mpll;
	protected WeightedPseudoLogLikelihood wpll;
	
	protected Set<Formula> unitClauses;
	protected Set<Predicate> targetPredicates;
	protected Set<Predicate> observedPredicates;
	protected Map<Predicate,Map<Integer,Set<String>>> predicateTypeMap;

	public Search(Model model, Database rvDB, Database observedDB, ConfigBundle config, Set<Formula> unitClauses, Set<Predicate> targetPredicates, Set<Predicate> observedPredicates, Map<Predicate,Map<Integer,Set<String>>> predicateTypeMap, GroundRuleStore groundRuleStore) {
		this.model = model;
		this.rvDB = rvDB;
		this.observedDB = observedDB;
		this.config = config;
		this.unitClauses = unitClauses;
		this.targetPredicates = targetPredicates;
		this.observedPredicates = observedPredicates;
		this.predicateTypeMap = predicateTypeMap;
		this.groundRuleStore = groundRuleStore;

		mpll = new MaxPseudoLikelihood(model, rvDB, observedDB, config, groundRuleStore);
		wpll = new WeightedPseudoLogLikelihood(model, rvDB, observedDB, config, groundRuleStore);

	}

	public Set<WeightedRule> search(double startingScore){

		init();
		Set<WeightedRule> rules = doSearch(startingScore);
		return rules;

	}


	protected void init(){

		trainingMap = new TrainingMap(rvDB, observedDB);
		if (trainingMap.getLatentVariables().size() > 0) {
			throw new IllegalArgumentException("All RandomVariableAtoms must have " +
					"corresponding ObservedAtoms. Latent variables are not supported " +
					"by this WeightLearningApplication. " +
					"Example latent variable: " + trainingMap.getLatentVariables().iterator().next());
		}

		clConstr = new ClauseConstructor(config, targetPredicates, observedPredicates, predicateTypeMap, groundRuleStore, trainingMap);
	}


	protected Map<WeightedRule,Double> getRuleWeights(){
		Map<WeightedRule,Double> ruleWeightMap = new HashMap<WeightedRule,Double>();
		for(Rule r : model.getRules()){
			if(r instanceof WeightedRule){
				double ruleWeight = ((WeightedRule)r).getWeight().getWeight();
				ruleWeightMap.put(((WeightedRule)r), ruleWeight);
			}
			
		}
		return ruleWeightMap;
	}

	protected void resetRuleWeights(Map<WeightedRule,Double> ruleWeightMap){
		for(Rule r : model.getRules()){
			if(r instanceof WeightedRule){
				double ruleWeight = ruleWeightMap.get(r);
				Weight w = new PositiveWeight(ruleWeight);
				((WeightedRule)r).setWeight(w);
			}
		}
	}

	/**
	 * Sets RandomVariableAtoms with training labels to their observed values.
	 */
	protected void setLabeledRandomVariables() {
		for (Map.Entry<RandomVariableAtom, ObservedAtom> e : trainingMap.getTrainingMap().entrySet()) {
			e.getKey().setValue(e.getValue().getValue());
		}
	}



	protected abstract Set<WeightedRule> doSearch(double startingScore);


	@Override
	public void close() {
		model = null;
		rvDB = null;
		config = null;
		mpll.close();
		wpll.close();
	}


}
