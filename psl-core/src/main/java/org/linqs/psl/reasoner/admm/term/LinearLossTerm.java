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
package org.linqs.psl.reasoner.admm.term;

import org.linqs.psl.reasoner.term.WeightedTerm;

import java.util.List;

/**
 * {@link ADMMReasoner} objective term of the form <br />
 * weight * coeffs^T * x
 * 
 * @author Stephen Bach <bach@cs.umd.edu>
 */
public class LinearLossTerm extends ADMMObjectiveTerm implements WeightedTerm {
	
	private final List<Double> coeffs;
	private double weight;

	/**
	 * Caller releases control of |zIndices| and |coeffs|.
	 */
	LinearLossTerm(List<LocalVariable> variables, List<Double> coeffs, double weight) {
		super(variables);

		assert(variables.size() == coeffs.size());

		this.coeffs = coeffs;
		setWeight(weight);
	}

	@Override
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	@Override
	public void minimize(double stepSize, double[] consensusValues) {
		for (int i = 0; i < variables.size(); i++) {
			LocalVariable variable = variables.get(i);

			double value = consensusValues[variable.getGlobalId()] - variable.getLagrange() / stepSize;
			value -= (weight * coeffs.get(i).doubleValue() / stepSize);

			variable.setValue(value);
		}
	}
}
