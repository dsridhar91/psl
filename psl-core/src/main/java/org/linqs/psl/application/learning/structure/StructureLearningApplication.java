package org.linqs.psl.application.learning.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import org.linqs.psl.application.ModelApplication;
import org.linqs.psl.application.groundrulestore.GroundRuleStore;
import org.linqs.psl.application.util.Grounding;
import org.linqs.psl.application.learning.weight.TrainingMap;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.config.Factory;
import org.linqs.psl.database.Database;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.predicate.Predicate;
import org.linqs.psl.model.predicate.StandardPredicate;
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
import java.util.Set;
import java.util.HashSet;
import java.util.Observable;

import com.google.common.collect.Iterables;

public abstract class StructureLearningApplication extends Observable implements ModelApplication {

	/**
	 * Prefix of property keys used by this class.
	 * 
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "structurelearning";
	
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
	protected Database rvDB, observedDB;
	protected ConfigBundle config;
	protected TrainingMap trainingMap;
	
	protected final List<WeightedRule> kernels;
	protected final List<WeightedRule> immutableKernels;

	protected Set<Predicate> targetPredicates;
	protected Set<Predicate> observedPredicates;

	protected Map<Predicate,Map<Integer,String>> predicateTypeMap;

	
	public StructureLearningApplication(Model model, Database rvDB, Database observedDB, ConfigBundle config, Set<Predicate> targetPredicates, Set<Predicate> observedPredicates, Map<Predicate,Map<Integer,String>> predicateTypeMap) {
		this.model = model;
		this.rvDB = rvDB;
		this.observedDB = observedDB;
		this.config = config;
		this.targetPredicates = targetPredicates;
		this.observedPredicates = observedPredicates;
		this.predicateTypeMap = predicateTypeMap;

		kernels = new ArrayList<WeightedRule>();
		immutableKernels = new ArrayList<WeightedRule>();
	}
	
	/**
	 * Learns model rules, i.e. kernels.
	 * <p>
	 * The {@link RandomVariableAtom RandomVariableAtoms} in the distribution are those
	 * persisted in the random variable Database when this method is called. All
	 * RandomVariableAtoms which the Model might access must be persisted in the Database.
	 * <p>
	 * Each such RandomVariableAtom should have a corresponding {@link ObservedAtom}
	 * in the observed Database, unless the subclass implementation supports latent
	 * variables.
	 * 
	 * @see DatabasePopulator
	 */
	public void structLearn()
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		/* Gathers the CompatibilityKernels */

		// System.out.println(model.getRules());
		for (WeightedRule k : Iterables.filter(model.getRules(), WeightedRule.class))
			if (k.isWeightMutable())
				kernels.add(k);
			else
				immutableKernels.add(k);

		initTrainingMap();
		doStructureLearn();
		
		kernels.clear();
	}

	protected void initTrainingMap(){
		trainingMap = new TrainingMap(rvDB, observedDB);
		if (trainingMap.getLatentVariables().size() > 0) {
			throw new IllegalArgumentException("All RandomVariableAtoms must have " +
					"corresponding ObservedAtoms. Latent variables are not supported " +
					"by this WeightLearningApplication. " +
					"Example latent variable: " + trainingMap.getLatentVariables().iterator().next());
		}
	}
	
	protected abstract void doStructureLearn();

	
	protected void setLabeledRandomVariables() {
		for (Map.Entry<RandomVariableAtom, ObservedAtom> e : trainingMap.getTrainingMap().entrySet()) {
			e.getKey().setValue(e.getValue().getValue());
		}
	}

	
	
	@Override
	public void close() {
		model = null;
		rvDB = null;
		config = null;
	}
	
}
