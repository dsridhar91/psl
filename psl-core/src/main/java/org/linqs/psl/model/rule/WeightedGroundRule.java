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
package org.linqs.psl.model.rule;

import org.linqs.psl.model.atom.GroundAtom;
import org.linqs.psl.model.weight.Weight;
import org.linqs.psl.reasoner.function.FunctionTerm;

public interface WeightedGroundRule extends GroundRule {
	
	@Override
	public WeightedRule getRule();
	
	/**
	 * Returns the Weight of this WeightedGroundRule.
	 * <p>
	 * Until {@link #setWeight(Weight)} is called, this GroundRule's weight
	 * is the current weight of its parent Rule. After it is called, it remains
	 * the most recent Weight set by {@link #setWeight(Weight)}.
	 * 
	 * @return this GroundRule's Weight
	 * @see WeightedRule#getWeight()
	 */
	public Weight getWeight();
	
	/**
	 * Sets a weight for this WeightedGroundRule.
	 * 
	 * @param w  new weight
	 */
	public void setWeight(Weight w);
	
	public FunctionTerm getFunctionDefinition();

	/**
	 * Returns the incompatibility of the truth values of this GroundRule's
	 * {@link GroundAtom GroundAtoms}.
	 * <p>
	 * Incompatibility is always non-negative.
	 * 
	 * @return the incompatibility of the current truth values
	 */
	public double getIncompatibility();
	
}
