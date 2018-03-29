/*
 * This file is part of the PSL software.
 * Copyright 2011-2015 University of Maryland
 * Copyright 2013-2017 The Regents of the University of California
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
package org.linqs.psl.application.learning.weight.maxlikelihood;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.database.Database;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.atom.GroundAtom;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.Rule;
import org.linqs.psl.model.rule.WeightedGroundRule;
import org.linqs.psl.model.rule.WeightedRule;
import org.linqs.psl.application.groundrulestore.GroundRuleStore;

/**
 * Learns weights by optimizing the piecewise-pseudo-log-likelihood of the data using
 * the voted perceptron algorithm.
 * 
 * @author Varun Embar <vembar@ucsc.edu>
 */
public class MaxPiecewisePseudoLikelihood extends VotedPerceptron {

	/**
	 * Prefix of property keys used by this class.
	 * 
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "maxpiecewisepseudolikelihood";
	
	/**
	 * Key for positive integer property.
	 * MaxPiecewisePseudoLikelihood will sample this many values to approximate
	 * the expectations.
	 */
	public static final String NUM_SAMPLES_KEY = CONFIG_PREFIX + ".numsamples";
	/** Default value for NUM_SAMPLES_KEY */
	public static final int NUM_SAMPLES_DEFAULT = 5000;
	
	private final int numSamples;
	protected ArrayList<HashMap<RandomVariableAtom, ArrayList<GroundRule>>> ruleRandomVariableMap;
	
	/**
	 * Constructor
	 * @param model
	 * @param rvDB
	 * @param observedDB
	 * @param config
	 */
	public MaxPiecewisePseudoLikelihood(Model model, Database rvDB, Database observedDB, ConfigBundle config) {
		super(model, rvDB, observedDB, config);
		numSamples = config.getInt(NUM_SAMPLES_KEY, NUM_SAMPLES_DEFAULT);
		if (numSamples <= 0)
			throw new IllegalArgumentException("Number of samples must be positive integer.");
	}
	
	public MaxPiecewisePseudoLikelihood(Model model, Database rvDB, Database observedDB, ConfigBundle config, GroundRuleStore groundRuleStore) {
		super(model, rvDB, observedDB, config, groundRuleStore);
		numSamples = config.getInt(NUM_SAMPLES_KEY, NUM_SAMPLES_DEFAULT);
		if (numSamples <= 0)
			throw new IllegalArgumentException("Number of samples must be positive integer.");
	}	

	/**
	 * Creates a dictonary for each unground rule. The dictonary takes a
	 * random variable atom and returns the set of all groundrules of the 
	 * rule that the atom participates in
	 * */
	protected void populateRandomVariableMap() {
		System.out.println("populate");
		ruleRandomVariableMap = new ArrayList<HashMap<RandomVariableAtom, ArrayList<GroundRule>>>();
		for (Rule rule : rules) { 
			HashMap<RandomVariableAtom, ArrayList<GroundRule>> groundRuleMap = new HashMap<RandomVariableAtom, ArrayList<GroundRule>>();
			for (GroundRule r : groundRuleStore.getGroundRules(rule)) {
				for (GroundAtom atom : r.getAtoms()) {
					if (atom instanceof RandomVariableAtom) {
						if (!groundRuleMap.containsKey(atom)) {
							groundRuleMap.put((RandomVariableAtom)atom, new ArrayList<GroundRule>());
						}
						groundRuleMap.get(atom).add(r);
					}
				}
			}
			ruleRandomVariableMap.add(groundRuleMap);
		}
	}

	/**
	 * Computes the expected incompatibility using the piecewisepseudolikelihood.
	 * Uses Monte Carlo integration to approximate epectations.
	 */
	@Override
	protected double[] computeExpectedIncomp() {

		setLabeledRandomVariables();
		if(ruleRandomVariableMap == null) {
			populateRandomVariableMap();
		}

		Random random = new Random();
		double[] expInc = new double[rules.size()];

		for (int i = 0; i < expInc.length; i++) {
			Rule rule = rules.get(i);
			HashMap<RandomVariableAtom, ArrayList<GroundRule>> groundRuleMap = ruleRandomVariableMap.get(i);
			double accumulateIncompatibility = 0;
			double weight = ((WeightedRule) rule).getWeight().getWeight();

			for(RandomVariableAtom atom: groundRuleMap.keySet()) {
				double oldValue = atom.getValue();
				double num = 0;
				double den = 0;
				for (int iSample = 0; iSample < numSamples; iSample++) {
					double sample = random.nextDouble();
					atom.setValue(sample);

					double energy = 0;
					for(GroundRule r: groundRuleMap.get(atom)) {
						energy -=  ((WeightedGroundRule) r).getIncompatibility();
					}

					den += Math.exp(weight*energy);
					num += Math.exp(weight*energy) * energy;
				}
				atom.setValue(oldValue);
				accumulateIncompatibility += num / den;
			}
			expInc[i] = accumulateIncompatibility;
		}
		System.out.println(Arrays.toString(expInc));
		return expInc;
	}

	@Override
	protected double computeLoss() {
		Random random = new Random();
		setLabeledRandomVariables();

		if(ruleRandomVariableMap == null) {
			populateRandomVariableMap();
		}

		double loss = 0.0;
		int numRules = rules.size();

		for (int i = 0; i < numRules; i++) {
			HashMap<RandomVariableAtom, ArrayList<GroundRule>> groundRuleMap = ruleRandomVariableMap.get(i);
			Rule rule = rules.get(i);
			double weight = ((WeightedRule) rule).getWeight().getWeight();

			for(RandomVariableAtom atom: groundRuleMap.keySet()) {

				double oldValue = atom.getValue();
				double expInc = 0;
				for (int iSample = 0; iSample < numSamples; iSample++) {
					double sample = random.nextDouble();
					atom.setValue(sample);

					double energy = 0;
					for(GroundRule r: groundRuleMap.get(atom)) {
						energy -=  ((WeightedGroundRule) r).getIncompatibility();
					}
					expInc += Math.exp(weight*energy);
				}
				atom.setValue(oldValue);

				double obsInc = 0;
				for(GroundRule r: groundRuleMap.get(atom)) {
					obsInc = -1 * weight * ((WeightedGroundRule) r).getIncompatibility();
				}
				expInc = -1 * Math.log( expInc / numSamples);
				loss += obsInc + expInc;  
			}
		}
		return loss;
	}

	@Override
	protected double[] computeObservedIncomp() {

		double[] truthIncompatibility = new double[rules.size()];
		numGroundings = new double[rules.size()];

		setLabeledRandomVariables();
		if(ruleRandomVariableMap == null) {
			populateRandomVariableMap();
		}
		
		/* Computes the observed incompatibilities and numbers of groundings */
		for (int i = 0; i < rules.size(); i++) {
			Rule rule = rules.get(i);
			HashMap<RandomVariableAtom, ArrayList<GroundRule>> groundRuleMap = ruleRandomVariableMap.get(i);

			double weight = ((WeightedRule) rule).getWeight().getWeight();
			double obsInc = 0;
			for(RandomVariableAtom atom: groundRuleMap.keySet()) {
				for(GroundRule r: groundRuleMap.get(atom)) {
					obsInc += weight * ((WeightedGroundRule) r).getIncompatibility();
				}
			}
			truthIncompatibility[i] = obsInc;
		}
		System.out.println("true" + Arrays.toString(truthIncompatibility));
		return truthIncompatibility;
	}
}