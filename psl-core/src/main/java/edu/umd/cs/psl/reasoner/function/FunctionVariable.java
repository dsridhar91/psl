/*
 * This file is part of the PSL software.
 * Copyright 2011-2013 University of Maryland
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
package edu.umd.cs.psl.reasoner.function;

/**
 * A variable in a numeric function.
 */
public interface FunctionVariable extends FunctionSingleton {

	public boolean isConstant();
	
	/**
	 * Sets the variable's value
	 *
	 * @param val  the value to set
	 */
	public void setValue(double val);
	
	/**
	 * Sets a confidence value that is associated with the variable.
	 *
	 * @param val  the value to set
	 */
	public void setConfidence(double val);
	
	/**
	 * Returns a confidence value associated with the variable.
	 *
	 * @return  the confidence value
	 */
	public double getConfidence();
	
}
