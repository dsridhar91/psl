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
package org.linqs.psl.application.learning.structure.greedysearch.scoring;

import org.linqs.psl.application.ModelApplication;
import org.linqs.psl.application.groundrulestore.GroundRuleStore;
import org.linqs.psl.database.atom.TrainingMapAtomManager;
import org.linqs.psl.application.util.Grounding;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.model.predicate.StandardPredicate;
import org.linqs.psl.config.Factory;
import org.linqs.psl.database.Database;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.WeightedRule;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.reasoner.Reasoner;
import org.linqs.psl.reasoner.ReasonerFactory;
import org.linqs.psl.reasoner.admm.ADMMReasonerFactory;
import org.linqs.psl.reasoner.term.TermGenerator;
import org.linqs.psl.reasoner.term.TermStore;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

public abstract class Scorer extends Observable implements ModelApplication {
	/**
	 * Prefix of property keys used by this class.
	 *
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "scoring";

	/**
	 * Key for {@link Factory} or String property.
	 * <p>
	 * Should be set to a {@link ReasonerFactory} or the fully qualified
	 * name of one. Will be used to instantiate a {@link Reasoner}.
	 * <p>
	 * This reasoner will be used when constructing ground models for weight
	 * learning, unless this behavior is overriden by a subclass.
	 */
	public static final String REASONER_KEY = CONFIG_PREFIX + ".reasoner";
	/**
	 * Default value for REASONER_KEY.
	 * <p>
	 * Value is instance of {@link ADMMReasonerFactory}.
	 */
	public static final ReasonerFactory REASONER_DEFAULT = new ADMMReasonerFactory();

	/**
	 * The class to use for ground rule storage.
	 */
	public static final String GROUND_RULE_STORE_KEY = CONFIG_PREFIX + ".groundrulestore";
   // ConstraintBlockers require AtomRegisterGroundRuleStore.
	public static final String GROUND_RULE_STORE_DEFAULT = "org.linqs.psl.application.groundrulestore.AtomRegisterGroundRuleStore";

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
	protected Database rvDB, observedDB;
	protected ConfigBundle config;

	protected final List<WeightedRule> kernels;
	protected final List<WeightedRule> immutableKernels;
	protected TrainingMapAtomManager trainingMap;

	protected Reasoner reasoner;
	protected GroundRuleStore groundRuleStore;
	protected TermStore termStore;
	protected TermGenerator termGenerator;

	protected boolean passedGroundRuleStore;

	public Scorer(Model model, Database rvDB, Database observedDB, ConfigBundle config) {
		this.model = model;
		this.rvDB = rvDB;
		this.observedDB = observedDB;
		this.config = config;

		passedGroundRuleStore = false;

		kernels = new ArrayList<WeightedRule>();
		immutableKernels = new ArrayList<WeightedRule>();
	}


	public Scorer(Model model, Database rvDB, Database observedDB, ConfigBundle config, GroundRuleStore groundRuleStore) {
		this.model = model;
		this.rvDB = rvDB;
		this.observedDB = observedDB;
		this.config = config;
		this.groundRuleStore = groundRuleStore;

		passedGroundRuleStore = true;

		kernels = new ArrayList<WeightedRule>();
		immutableKernels = new ArrayList<WeightedRule>();
	}

	public double scoreModel()
		throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		/* Gathers the CompatibilityKernels */
		for (WeightedRule k : Iterables.filter(model.getRules(), WeightedRule.class))
			if (k.isWeightMutable())
				kernels.add(k);
			else
				immutableKernels.add(k);

		initGroundModel();

		/* Score the current model */
		Double currentModelScore = doScoring();

		kernels.clear();
		cleanUpGroundModel();

		return currentModelScore;
	}

	protected abstract double doScoring();

	protected void initGroundModel() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		reasoner = ((ReasonerFactory) config.getFactory(REASONER_KEY, REASONER_DEFAULT)).getReasoner(config);
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

		setLabeledRandomVariables();

		termGenerator.generateTerms(groundRuleStore, termStore);
	}

	protected void cleanUpGroundModel() {
		trainingMap = null;

		termStore.close();
		termStore = null;

		if(!passedGroundRuleStore){
			groundRuleStore.close();
			groundRuleStore = null;
		}

		reasoner.close();
		reasoner = null;
	}

	@Override
	public void close() {
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
