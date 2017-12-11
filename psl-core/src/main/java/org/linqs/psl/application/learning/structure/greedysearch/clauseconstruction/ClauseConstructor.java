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
package org.linqs.psl.application.learning.structure.greedysearch.clauseconstruction;

import org.linqs.psl.application.groundrulestore.GroundRuleStore;
import org.linqs.psl.application.groundrulestore.MemoryGroundRuleStore;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.EmptyBundle;
import org.linqs.psl.database.DataStore;
import org.linqs.psl.database.Database;
import org.linqs.psl.database.Partition;
import org.linqs.psl.database.loading.Inserter;
import org.linqs.psl.database.rdbms.RDBMSDataStore;
import org.linqs.psl.database.rdbms.driver.H2DatabaseDriver;
import org.linqs.psl.database.rdbms.driver.H2DatabaseDriver.Type;
import org.linqs.psl.model.atom.Atom;
import org.linqs.psl.model.atom.AtomCache;
import org.linqs.psl.model.atom.AtomManager;
import org.linqs.psl.model.atom.GroundAtom;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.QueryAtom;
import org.linqs.psl.model.atom.SimpleAtomManager;
import org.linqs.psl.model.formula.Conjunction;
import org.linqs.psl.model.formula.Disjunction;
import org.linqs.psl.model.formula.Negation;
import org.linqs.psl.model.formula.Formula;
import org.linqs.psl.model.formula.Implication;
import org.linqs.psl.model.predicate.PredicateFactory;
import org.linqs.psl.model.predicate.StandardPredicate;
import org.linqs.psl.model.predicate.Predicate;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.Rule;
import org.linqs.psl.model.rule.WeightedRule;
import org.linqs.psl.model.rule.arithmetic.UnweightedArithmeticRule;
import org.linqs.psl.model.rule.arithmetic.WeightedArithmeticRule;
import org.linqs.psl.model.rule.arithmetic.expression.ArithmeticRuleExpression;
import org.linqs.psl.model.rule.arithmetic.expression.SummationAtomOrAtom;
import org.linqs.psl.model.rule.arithmetic.expression.SummationVariable;
import org.linqs.psl.model.rule.arithmetic.expression.coefficient.Coefficient;
import org.linqs.psl.model.rule.arithmetic.expression.coefficient.ConstantNumber;
import org.linqs.psl.model.rule.logical.UnweightedLogicalRule;
import org.linqs.psl.model.rule.logical.WeightedLogicalRule;
import org.linqs.psl.model.term.Constant;
import org.linqs.psl.model.term.ConstantType;
import org.linqs.psl.model.term.Term;
import org.linqs.psl.model.term.UniqueStringID;
import org.linqs.psl.model.term.Variable;
import org.linqs.psl.reasoner.function.FunctionComparator;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClauseConstructor {

	private Set<Predicate> targetPredicates;
	private Set<Predicate> observedPredicates;


	public ClauseConstructor(Set<Predicate> targetPredicates, Set<Predicate> observedPredicates) {
		this.targetPredicates = targetPredicates;
		this.observedPredicates = observedPredicates;
	}

	private Set<Term> getClauseVariables(Formula c, int arity) {

		Set<Term> vars = new HashSet<Term>();
		Set<Atom> atoms = new HashSet<Atom>();
		
		atoms = c.getAtoms(atoms);
		for(Atom a: atoms) {
			for(Term v : a.getArguments()) {
				vars.add(v);
			}
		}
		
		char ch = 'A';
		for(int i = 0; i < arity - 1; i++) {
			vars.add(new Variable(String.valueOf((char) (ch + i + vars.size()))));
		}

		return vars;
	}

	private Set<Formula> pruneClauses(Set<Formula> clauses) {

		Set<Formula> prunedClauses = new HashSet<Formula>();

		for(Formula c: clauses) {
			try {
				WeightedRule rule = new WeightedLogicalRule(c, 1.0, true);
				prunedClauses.add(c);
			}
			catch (IllegalArgumentException ex){
				//System.out.println("Removing Clause :" + c);
			}
		}
		
		return prunedClauses;
	}

	public Set<Formula> createCandidateClauses(Set<Formula> clauses) {

		Set<Formula> candidateClauses = new HashSet<Formula>();

		for(Formula c: clauses) {
			for(Predicate p : observedPredicates) {
				int arity = p.getArity();
				Set<Term> vars = getClauseVariables(c, arity);

				int numRules = (int)Math.pow(vars.size(), arity);
				Term[][] args = new Term[numRules][arity];

				for(int i = 0; i < arity; i++) {
					int repeat = (int)Math.pow(vars.size(), (arity - i - 1));
					int count = 0;
					while(count < numRules) {
						for(Term v : vars) {
							for(int j = 0; j < repeat; j++) {
								args[count++][i] = v;
							}
						}
					}
				}
				
				/*for(int i = 0; i < numRules; i++) {
					System.out.println("");
					for(int j = 0; j < arity; j++) {
						System.out.println(args[i][j] + " ");
					}
				}

				char ch = 'A';
				for(int i = 0; i < arity; i++) {
					args[i] = new Variable(String.valueOf((char) (ch + i)));
				}*/

				if (c instanceof Disjunction){
					for(int i = 0; i < numRules; i++) {
						candidateClauses.add( new Disjunction(((Disjunction) c).flatten(), new Negation(new QueryAtom(p, args[i]))));					
						candidateClauses.add( new Disjunction(((Disjunction) c).flatten(), new QueryAtom(p, args[i])));					
					}
				}
				else{
					Disjunction newClause = null;
					for(int i = 0; i < numRules; i++) {
						newClause = new Disjunction(c, new Negation(new QueryAtom(p, args[i])));
						candidateClauses.add(newClause);	
						newClause = new Disjunction(c, new QueryAtom(p, args[i]));
						candidateClauses.add(newClause);	
					}
				}
			}

		}

		candidateClauses = pruneClauses(candidateClauses);

		/*System.out.println("Clauses");
		for(Formula c: candidateClauses) {
			System.out.println(c);
		}*/

		return candidateClauses;
	}
}
