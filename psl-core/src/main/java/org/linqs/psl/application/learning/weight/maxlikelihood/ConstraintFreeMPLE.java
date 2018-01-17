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

import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.database.Database;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.atom.GroundAtom;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.WeightedGroundRule;
import org.linqs.psl.model.rule.WeightedRule;
import org.linqs.psl.application.groundrulestore.GroundRuleStore;

/**
 * Learns weights by optimizing the pseudo-log-likelihood of the data using
 * the voted perceptron algorithm.
 * 
 * @author Ben London <blondon@cs.umd.edu>
 */
public class ConstraintFreeMPLE extends VotedPerceptron {

	/**
	 * Prefix of property keys used by this class.
	 * 
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "constraintfreemple";
	
	/**
	 * Boolean property. If true, MaxPseudoLikelihood will treat RandomVariableAtoms
	 * as boolean valued. Note that this restricts the types of contraints supported.
	 */
	public static final String BOOLEAN_KEY = CONFIG_PREFIX + ".bool";
	/** Default value for BOOLEAN_KEY */
	public static final boolean BOOLEAN_DEFAULT = false;
	
	/**
	 * Key for positive integer property.
	 * MaxPseudoLikelihood will sample this many values to approximate
	 * the integrals in the marginal computation.
	 */
	public static final String NUM_SAMPLES_KEY = CONFIG_PREFIX + ".numsamples";
	/** Default value for NUM_SAMPLES_KEY */
	public static final int NUM_SAMPLES_DEFAULT = 20;
	
	/**
	 * Key for constraint violation tolerance
	 */
	public static final String CONSTRAINT_TOLERANCE_KEY = CONFIG_PREFIX + ".constrainttolerance";
	/** Default value for CONSTRAINT_TOLERANCE **/
	public static final double CONSTRAINT_TOLERANCE_DEFAULT = 1e-5;
	
	/**
	 * Key for positive double property.
	 * Used as minimum width for bounds of integration.
	 */
	public static final String MIN_WIDTH_KEY = CONFIG_PREFIX + ".minwidth";
	/** Default value for MIN_WIDTH_KEY */
	public static final double MIN_WIDTH_DEFAULT = 1e-2;
	
	private final boolean bool;
	private final int numSamples;
	private final double minWidth;
	private final double constraintTol;
	
	/**
	 * Constructor
	 * @param model
	 * @param rvDB
	 * @param observedDB
	 * @param config
	 */
	public ConstraintFreeMPLE(Model model, Database rvDB, Database observedDB, ConfigBundle config) {
		super(model, rvDB, observedDB, config);
		bool = config.getBoolean(BOOLEAN_KEY, BOOLEAN_DEFAULT);
		numSamples = config.getInt(NUM_SAMPLES_KEY, NUM_SAMPLES_DEFAULT);
		if (numSamples <= 0)
			throw new IllegalArgumentException("Number of samples must be positive integer.");
		minWidth = config.getDouble(MIN_WIDTH_KEY, MIN_WIDTH_DEFAULT);
		if (minWidth <= 0)
			throw new IllegalArgumentException("Minimum width must be positive double.");
		constraintTol = config.getDouble(CONSTRAINT_TOLERANCE_KEY, CONSTRAINT_TOLERANCE_DEFAULT);
		if (constraintTol <= 0)
			throw new IllegalArgumentException("Minimum width must be positive double.");
	}
	
	public ConstraintFreeMPLE(Model model, Database rvDB, Database observedDB, ConfigBundle config, GroundRuleStore groundRuleStore) {
		super(model, rvDB, observedDB, config, groundRuleStore);
		bool = config.getBoolean(BOOLEAN_KEY, BOOLEAN_DEFAULT);
		numSamples = config.getInt(NUM_SAMPLES_KEY, NUM_SAMPLES_DEFAULT);
		if (numSamples <= 0)
			throw new IllegalArgumentException("Number of samples must be positive integer.");
		minWidth = config.getDouble(MIN_WIDTH_KEY, MIN_WIDTH_DEFAULT);
		if (minWidth <= 0)
			throw new IllegalArgumentException("Minimum width must be positive double.");
		constraintTol = config.getDouble(CONSTRAINT_TOLERANCE_KEY, CONSTRAINT_TOLERANCE_DEFAULT);
		if (constraintTol <= 0)
			throw new IllegalArgumentException("Minimum width must be positive double.");
	}	
	/**
	 * Computes the expected incompatibility using the pseudolikelihood.
	 * Uses Monte Carlo integration to approximate definite integrals,
	 * since they do not admit a closed-form antiderivative.
	 */
	@Override
	protected double[] computeExpectedIncomp() {
		
		double[] expInc = new double[rules.size()];

		Random random = new Random();

		Set<RandomVariableAtom> freeRVSet = new HashSet<RandomVariableAtom>();
		for (GroundRule groundRule : groundRuleStore.getGroundRules()) {
			for (GroundAtom atom : groundRule.getAtoms()) {
				if (atom instanceof RandomVariableAtom) {
					freeRVSet.add(((RandomVariableAtom)atom));
				}
			}
		}

		/* Accumulate the expected incompatibility over all atoms */
		for (RandomVariableAtom atom : freeRVSet) {
			
			/* Sample numSamples random numbers in the range of integration */
			double[] s;
			
			s = new double[numSamples];
			for (int iSample = 0; iSample < s.length; iSample++) {
				s[iSample] = random.nextDouble();
			}

			// double width = 1.0/numSamples;
			// for (int iSample = 0; iSample < s.length; iSample++) {
			// 	s[iSample] = width*iSample;
			// }
			
			/* Compute the incompatibility of each sample for each rule */
			HashMap<WeightedRule,double[]> incompatibilities = new HashMap<WeightedRule,double[]>();
			
			/* Saves original state */
			double originalAtomValue = atom.getValue();
			
			/* Computes the probability */
			for (GroundRule groundRule : atom.getRegisteredGroundRules()) {
				if (groundRule instanceof WeightedGroundRule) {
					WeightedRule rule = (WeightedRule) groundRule.getRule();
					if (!incompatibilities.containsKey(rule))
						incompatibilities.put(rule, new double[s.length]);
					double[] inc = incompatibilities.get(rule);
					for (int iSample = 0; iSample < s.length; iSample++) {
						atom.setValue(s[iSample]);
						inc[iSample] += ((WeightedGroundRule) groundRule).getIncompatibility();
					}
				}
			}
			
			atom.setValue(originalAtomValue);

			Arrays.sort(s);

			double width;
			/* Compute the exp incomp and accumulate the partition for the current atom. */
			HashMap<WeightedRule,Double> expIncAtom = new HashMap<WeightedRule,Double>();
			double Z = 0.0;
			for (int j = 0; j < s.length; j++) {
				/* Compute the exponent */
				double sum = 0.0;
				for (Map.Entry<WeightedRule,double[]> e2 : incompatibilities.entrySet()) {
					WeightedRule rule = e2.getKey();
					double[] inc = e2.getValue();
					sum -= rule.getWeight().getWeight() * inc[j];
				}
				double exp = Math.exp(sum);
				/* Add to partition */
				width = 0.0;
				if(j == 0){
					width = s[j]; 
				}
				else if (j == s.length - 1){
					width = 1.0 - s[j];
				}
				else{
					width = s[j+1] - s[j];
				}

				Z += (exp * width);
				// Z+= exp;

				/* Compute the exp incomp for current atom */
				for (Map.Entry<WeightedRule,double[]> e2 : incompatibilities.entrySet()) {
					WeightedRule rule = e2.getKey();
					if (!expIncAtom.containsKey(rule))
						expIncAtom.put(rule, 0.0);
					double val = expIncAtom.get(rule).doubleValue();
					val += exp * incompatibilities.get(rule)[j];
					expIncAtom.put(rule, val);
				}
			}
			/* Finally, we add to the exp incomp for each rule */ 
			for (int i = 0; i < rules.size(); i++) {
				WeightedRule rule = rules.get(i);
				if (expIncAtom.containsKey(rule))
					if (expIncAtom.get(rule) > 0.0) 
						expInc[i] += expIncAtom.get(rule) / (numSamples*Z);
			}
		}
		
		// setLabeledRandomVariables();
		return expInc;
	}

}