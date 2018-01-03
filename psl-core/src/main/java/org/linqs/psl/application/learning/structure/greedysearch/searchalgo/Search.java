/*
 * This file is part of the PSL software.
 * Copyright 2011-2015 University of Maryland
 * Copyright 2013-2018 The Regents of the University of California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.linqs.psl.application.learning.structure.greedysearch.searchalgo;

import org.linqs.psl.application.ModelApplication;
import org.linqs.psl.application.learning.structure.greedysearch.clauseconstruction.ClauseConstructor;
import org.linqs.psl.application.learning.structure.greedysearch.scoring.WeightedPseudoLogLikelihood;
import org.linqs.psl.application.learning.weight.maxlikelihood.MaxPseudoLikelihood;
import org.linqs.psl.config.ConfigBundle;
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
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.database.atom.TrainingMapAtomManager;
import org.linqs.psl.application.groundrulestore.GroundRuleStore;

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Observable;
import java.util.Set;
import java.util.HashSet;

public abstract class Search extends Observable implements ModelApplication {
	/**
	 * Prefix of property keys used by this class.
	 */
	public static final String CONFIG_PREFIX = "search";

	protected Model model;
	protected TrainingMapAtomManager trainingMap;
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

	public Set<WeightedRule> search(double startingScore) {
		init();
		Set<WeightedRule> rules = doSearch(startingScore);
		return rules;
	}

	protected void init() {
		trainingMap = new TrainingMapAtomManager(rvDB, observedDB);
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
		for (Rule rule : model.getRules()) {
			if (rule instanceof WeightedRule) {
				double ruleWeight = ((WeightedRule)rule).getWeight();
				ruleWeightMap.put(((WeightedRule)rule), ruleWeight);
			}
		}
		return ruleWeightMap;
	}

	protected void resetRuleWeights(Map<WeightedRule,Double> ruleWeightMap){
		for (Rule rule : model.getRules()) {
			if (rule instanceof WeightedRule) {
				((WeightedRule)rule).setWeight(ruleWeightMap.get(rule).doubleValue());
			}
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
