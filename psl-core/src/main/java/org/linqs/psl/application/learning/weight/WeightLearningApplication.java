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
package org.linqs.psl.application.learning.weight;

import org.linqs.psl.application.ModelApplication;
import org.linqs.psl.application.groundrulestore.GroundRuleStore;
import org.linqs.psl.application.util.Grounding;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.config.Factory;
import org.linqs.psl.database.Database;
import org.linqs.psl.database.atom.TrainingMapAtomManager;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.WeightedRule;
import org.linqs.psl.reasoner.Reasoner;
import org.linqs.psl.reasoner.ReasonerFactory;
import org.linqs.psl.reasoner.admm.ADMMReasonerFactory;
import org.linqs.psl.reasoner.term.TermGenerator;
import org.linqs.psl.reasoner.term.TermStore;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for learning the weights of weighted mutableRules from data for a model.
 */
public abstract class WeightLearningApplication implements ModelApplication {
	/**
	 * Prefix of property keys used by this class.
	 *
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "weightlearning";

	/**
	 * The class to use for inference.
	 */
	public static final String REASONER_KEY = CONFIG_PREFIX + ".reasoner";
	public static final String REASONER_DEFAULT = "org.linqs.psl.reasoner.admm.ADMMReasoner";

	/**
	 * The class to use for ground rule storage.
	 */
	public static final String GROUND_RULE_STORE_KEY = CONFIG_PREFIX + ".groundrulestore";
	public static final String GROUND_RULE_STORE_DEFAULT = "org.linqs.psl.application.groundrulestore.MemoryGroundRuleStore";

	/**
	 * The class to use for term storage.
	 * Should be compatible with REASONER_KEY.
	 */
	public static final String TERM_STORE_KEY = CONFIG_PREFIX + ".termstore";
	public static final String TERM_STORE_DEFAULT = "org.linqs.psl.reasoner.admm.term.ADMMTermStore";

	/**
	 * The class to use for term generator.
	 * Should be compatible with REASONER_KEY and TERM_STORE_KEY.
	 */
	public static final String TERM_GENERATOR_KEY = CONFIG_PREFIX + ".termgenerator";
	public static final String TERM_GENERATOR_DEFAULT = "org.linqs.psl.reasoner.admm.term.ADMMTermGenerator";

	protected Model model;
	protected Database rvDB;
	protected Database observedDB;
	protected ConfigBundle config;

	protected List<WeightedRule> mutableRules;
	protected List<WeightedRule> immutableRules;
	protected TrainingMapAtomManager trainingMap;

	/**
	 * Indicates that the rule weights have been changed and should be updated before optimization.
	 * This should always be checked before optimization.
	 */
	// TODO(eriq): This is suspect. Feels like an indication of a hack.
	protected boolean changedRuleWeights;
	protected boolean passedGroundRuleStore;

	protected Reasoner reasoner;
	protected GroundRuleStore groundRuleStore;
	protected TermStore termStore;
	protected TermGenerator termGenerator;

	public WeightLearningApplication(Model model, Database rvDB, Database observedDB, ConfigBundle config) {
		this(model, rvDB, observedDB, config, null);
	}

	public WeightLearningApplication(Model model, Database rvDB, Database observedDB, ConfigBundle config, GroundRuleStore groundRuleStore) {
		this.model = model;
		this.rvDB = rvDB;
		this.observedDB = observedDB;
		this.config = config;

		if (groundRuleStore != null) {
			this.groundRuleStore = groundRuleStore;
			changedRuleWeights = true;
			passedGroundRuleStore = true;
		}

		// TODO(eriq): Why not fill these now? We have the model.
		mutableRules = new ArrayList<WeightedRule>();
		immutableRules = new ArrayList<WeightedRule>();
	}

	/**
	 * Learns new weights.
	 * <p>
	 * The {@link RandomVariableAtom RandomVariableAtoms} in the distribution are those
	 * persisted in the random variable Database when this method is called. All
	 * RandomVariableAtoms which the Model might access must be persisted in the Database.
	 * <p>
	 * Each such RandomVariableAtom should have a corresponding {@link ObservedAtom}
	 * in the observed Database, unless the subclass implementation supports latent
	 * variables.
	 */
	public void learn() {
		// Gathers the CompatibilityRules.
		for (WeightedRule rule : Iterables.filter(model.getRules(), WeightedRule.class)) {
			if (rule.isWeightMutable()) {
				mutableRules.add(rule);
			} else {
				immutableRules.add(rule);
			}
		}

		// Sets up the ground model.
		initGroundModel();

		// Learns new weights.
		doLearn();

		// TODO(eriq): Why clear? And why not clear immutable?
		mutableRules.clear();
	}

	protected abstract void doLearn();

	/**
	 * Constructs a ground model using model and trainingMap, and stores the
	 * resulting GroundRules in reasoner.
	 */
	protected void initGroundModel() {
		reasoner = (Reasoner)config.getNewObject(REASONER_KEY, REASONER_DEFAULT);
		termStore = (TermStore)config.getNewObject(TERM_STORE_KEY, TERM_STORE_DEFAULT);

		if(!passedGroundRuleStore){
			groundRuleStore = (GroundRuleStore)config.getNewObject(GROUND_RULE_STORE_KEY, GROUND_RULE_STORE_DEFAULT);
		}

		termGenerator = (TermGenerator)config.getNewObject(TERM_GENERATOR_KEY, TERM_GENERATOR_DEFAULT);

		trainingMap = new TrainingMapAtomManager(rvDB, observedDB);
		if (trainingMap.getLatentVariables().size() > 0) {
			throw new IllegalArgumentException("All RandomVariableAtoms must have " +
					"corresponding ObservedAtoms. Latent variables are not supported " +
					"by this WeightLearningApplication. " +
					"Example latent variable: " + trainingMap.getLatentVariables().iterator().next());
		}

		if(!passedGroundRuleStore){
			Grounding.groundAll(model, trainingMap, groundRuleStore);
		}

		termGenerator.generateTerms(groundRuleStore, termStore);
	}

	@Override
	public void close() {
		trainingMap = null;

		termStore.close();
		termStore = null;

		if(!passedGroundRuleStore){
			groundRuleStore.close();
			groundRuleStore = null;
		}

		reasoner.close();
		reasoner = null;

		model = null;
		rvDB = null;
		config = null;
	}

	/**
	 * Sets RandomVariableAtoms with training labels to their observed values.
	 */
	protected void setLabeledRandomVariables() {
		for (Map.Entry<RandomVariableAtom, ObservedAtom> e : trainingMap.getTrainingMap().entrySet()) {
			e.getKey().setValue(e.getValue().getValue());
		}
	}
}
