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
import org.linqs.psl.application.util.Grounding;
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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class ClauseConstructor implements Iterator<Formula> {

	private Set<Predicate> targetPredicates;
	private Set<Predicate> observedPredicates;
	protected Map<Predicate,Map<Integer,Set<String>>> predicateTypeMap;
	private GroundRuleStore groundRuleStore;
	private AtomManager atomManager;

	private List<Formula> candidateClauses;
	private Formula nextClause;


	public ClauseConstructor(Set<Predicate> targetPredicates, Set<Predicate> observedPredicates, Map<Predicate,Map<Integer,Set<String>>> predicateTypeMap, GroundRuleStore groundRuleStore, AtomManager atomManager) {
		this.targetPredicates = targetPredicates;
		this.observedPredicates = observedPredicates;
		this.predicateTypeMap = predicateTypeMap;
		this.groundRuleStore = groundRuleStore;
		this.atomManager = atomManager;

		this.candidateClauses = null;
		this.nextClause = null;
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

	public boolean hasNext() {
		if (nextClause == null) {
			return false;
		}
		else {
			return true;
		}
	}

	public Formula next() {

		Formula clause = nextClause;

		nextClause = null;
		while(!this.candidateClauses.isEmpty()) {
			int listIndex = candidateClauses.size() - 1; 
			Formula c = candidateClauses.get(listIndex);
			candidateClauses.remove(listIndex);
			if(this.isValidClause(c)) {
				nextClause = clause;
				break;
			}
		}

		return clause;
	}

	public void remove() {
		throw new UnsupportedOperationException("Remove is not supported");
	}

	private boolean isValidClause(Formula c) {

		//Remove clauses where a variable does not occur in any non-negated predicate
		try {
			WeightedRule rule = new WeightedLogicalRule(c, 1.0, true);
		}
		catch (IllegalArgumentException ex){
			return false;
		}
			
		//Remove clauses that are type inconsistent
		Map<Term, Set<String>> varXtype = new HashMap<Term, Set<String>>();
		Set<Atom> atoms = new HashSet<Atom>();
		atoms = c.getAtoms(atoms);
		for(Atom a: atoms) {
			Map<Integer, Set<String>> predicateTypes = predicateTypeMap.get(a.getPredicate());
			Term[] vars = a.getArguments();
			for(int i = 0; i < vars.length; i++) {
				Set<String> varTypes = predicateTypes.get(i);
				if(varXtype.containsKey(vars[i])) {
					varTypes.retainAll(varXtype.get(vars[i]));
					if(varTypes.size() == 0) {
						return false;
					}
					varXtype.put(vars[i], varTypes);
				}
			}
		}

		//Remove clauses with zero groundings
		int numGroundings = Grounding.groundRule((Rule)c, atomManager, groundRuleStore);
		if(numGroundings == 0) {
			return false;
		}

		return true;
	}
					

	public void createCandidateClauses(Set<Formula> initialClauses) {

		for(Formula c: initialClauses) {
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

		//candidateClauses = pruneClauses(candidateClauses);
		//return candidateClauses;
	}
}
