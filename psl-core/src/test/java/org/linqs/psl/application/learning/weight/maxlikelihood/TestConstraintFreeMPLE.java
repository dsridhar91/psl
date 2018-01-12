package org.linqs.psl.application.learning.weight.maxlikelihood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.linqs.psl.PSLTest;
import org.linqs.psl.application.groundrulestore.GroundRuleStore;
import org.linqs.psl.application.groundrulestore.MemoryGroundRuleStore;
import org.linqs.psl.application.learning.weight.maxlikelihood.ConstraintFreeMPLE;
import org.linqs.psl.application.learning.weight.maxlikelihood.MaxLikelihoodMPE;
import org.linqs.psl.application.learning.structure.StructureLearningApplication;
import org.linqs.psl.application.learning.structure.greedysearch.TopDownStructureLearning;
import org.linqs.psl.application.learning.structure.greedysearch.scoring.WeightedPseudoLogLikelihood;
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
import org.linqs.psl.model.Model;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.formula.Negation;
import org.linqs.psl.model.atom.QueryAtom;
import org.linqs.psl.model.atom.SimpleAtomManager;
import org.linqs.psl.model.formula.Conjunction;
import org.linqs.psl.model.formula.Formula;
import org.linqs.psl.model.formula.Implication;
import org.linqs.psl.model.predicate.PredicateFactory;
import org.linqs.psl.model.predicate.StandardPredicate;
import org.linqs.psl.model.predicate.Predicate;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.Rule;
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
import org.linqs.psl.model.term.UniqueStringID;
import org.linqs.psl.model.term.Variable;
import org.linqs.psl.reasoner.function.FunctionComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestConstraintFreeMPLE {
	private DataStore dataStore;
	private Database rvDB;
	private Database truthDB;
	private ConfigBundle config;
	private Model model;
	private Partition obsPartition;
	private Partition targetPartition;
	private Partition truthPartition;

	private StandardPredicate singlePredicate;
	private StandardPredicate doublePredicateObs;
	private StandardPredicate doublePredicateTar;

	private Set<StandardPredicate> rvClose;
	private Set<StandardPredicate> truthClose;

	@Before
	public void setup() {
		config = new EmptyBundle();
		dataStore = new RDBMSDataStore(new H2DatabaseDriver(Type.Memory, this.getClass().getName(), true), config);

		// Predicates
		PredicateFactory factory = PredicateFactory.getFactory();

		doublePredicateObs = factory.createStandardPredicate("DoublePredicateObs", ConstantType.UniqueStringID, ConstantType.UniqueStringID);
		dataStore.registerPredicate(doublePredicateObs);

		doublePredicateTar = factory.createStandardPredicate("DoublePredicateTar", ConstantType.UniqueStringID, ConstantType.UniqueStringID);
		dataStore.registerPredicate(doublePredicateTar);
		// Data
		obsPartition = dataStore.getPartition("obs");
		targetPartition = dataStore.getPartition("target");

		
		Inserter inserter = dataStore.getInserter(doublePredicateObs, obsPartition);
		inserter.insertValue(0.9, new UniqueStringID("Alice"),new UniqueStringID("Bob"));
		inserter.insertValue(0.9, new UniqueStringID("Bob"),new UniqueStringID("Alice"));
		inserter.insertValue(0.1, new UniqueStringID("Bob"),new UniqueStringID("Bob"));
		inserter.insertValue(0.1, new UniqueStringID("Alice"),new UniqueStringID("Alice"));

		inserter = dataStore.getInserter(doublePredicateTar, targetPartition);
		inserter.insertValue(1.0, new UniqueStringID("Alice"),new UniqueStringID("Bob"));
		inserter.insertValue(1.0, new UniqueStringID("Bob"),new UniqueStringID("Alice"));
		inserter.insertValue(0.0, new UniqueStringID("Bob"),new UniqueStringID("Bob"));
		inserter.insertValue(0.0, new UniqueStringID("Alice"),new UniqueStringID("Alice"));

		rvClose = new HashSet<StandardPredicate>();
		rvClose.add(doublePredicateObs);
		rvDB = dataStore.getDatabase(targetPartition, rvClose, obsPartition);


		truthPartition = dataStore.getPartition("truth");
		inserter = dataStore.getInserter(doublePredicateTar, truthPartition);
		inserter.insert(new UniqueStringID("Alice"),new UniqueStringID("Bob"));
		inserter.insert(new UniqueStringID("Bob"),new UniqueStringID("Alice"));
		
		truthClose = new HashSet<StandardPredicate>();
		truthClose.add(doublePredicateTar);
		truthDB = dataStore.getDatabase(truthPartition, truthClose);

		//Model
		model = new Model();

		Rule goodRule = new WeightedLogicalRule(
			new Implication(
				new QueryAtom(doublePredicateObs, new Variable("A"), new Variable("B")),
				new QueryAtom(doublePredicateTar, new Variable("A"), new Variable("B"))
			),
			1.0,
			true
		);
		model.addRule(goodRule);

		Rule badRule = new WeightedLogicalRule(
			new Implication(
				new QueryAtom(doublePredicateObs, new Variable("A"), new Variable("B")),
				new Negation(new QueryAtom(doublePredicateTar, new Variable("A"), new Variable("B")))
			),
			1.0,
			true
		);
		model.addRule(badRule);
	}

	@Test
	public void testWeights() {
		try {
			// Set<Predicate> targetPredicates = new HashSet<Predicate>();
			// targetPredicates.add(doublePredicateTar);

			// Set<Predicate> observedPredicates = new HashSet<Predicate>();
			// observedPredicates.add(singlePredicate);
			// observedPredicates.add(doublePredicateObs);

			ConstraintFreeMPLE cfmple = new ConstraintFreeMPLE(model, rvDB, truthDB, config);
			cfmple.learn();
			System.out.println(model);
			
		} catch(Exception ex) {
			System.out.println(ex);
			ex.printStackTrace();
			fail("Exception thrown");
		}
		
	}

	@After
	public void cleanup() {
		rvDB.close();
		truthDB.close();
		dataStore.close();
	}
}
