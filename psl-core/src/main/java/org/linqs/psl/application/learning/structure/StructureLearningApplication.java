package org.linqs.psl.application.learning.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import org.linqs.psl.application.ModelApplication;
import org.linqs.psl.application.util.Grounding;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.config.Factory;
import org.linqs.psl.database.Database;
import org.linqs.psl.database.DatabasePopulator;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.WeightedRule;
import org.linqs.psl.reasoner.Reasoner;
import org.linqs.psl.reasoner.ReasonerFactory;
import org.linqs.psl.reasoner.admm.ADMMReasonerFactory;

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
	
	protected Model model;
	protected Database rvDB, observedDB;
	protected ConfigBundle config;
	
	protected final List<WeightedRule> kernels;
	protected final List<WeightedRule> immutableKernels;
	
	public StructureLearningApplication(Model model, Database rvDB, Database observedDB, ConfigBundle config) {
		this.model = model;
		this.rvDB = rvDB;
		this.observedDB = observedDB;
		this.config = config;

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
		for (WeightedRule k : Iterables.filter(model.getRules(), WeightedRule.class))
			if (k.isWeightMutable())
				kernels.add(k);
			else
				immutableKernels.add(k);
				/* Learns new weights */
		doStructLearn();
		
		kernels.clear();
		cleanUpGroundModel();
	}
	
	protected abstract void doStructureLearn();
	
	
	@Override
	public void close() {
		model = null;
		rvDB = null;
		config = null;
	}
	
}