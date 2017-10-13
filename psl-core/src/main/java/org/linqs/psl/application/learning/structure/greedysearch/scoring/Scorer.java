package org.linqs.psl.application.learning.structure.greedysearch.scoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.linqs.psl.application.ModelApplication;
import org.linqs.psl.database.Database;
import org.linqs.psl.model.Model;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.GroundRule;
import org.linqs.psl.model.rule.WeightedGroundRule;
import org.linqs.psl.model.rule.WeightedRule;
import org.linqs.psl.model.weight.NegativeWeight;
import org.linqs.psl.model.weight.PositiveWeight;
import org.linqs.psl.model.weight.Weight;
import org.linqs.psl.application.util.Grounding;
import org.linqs.psl.reasoner.Reasoner;
import org.linqs.psl.reasoner.ReasonerFactory;
import org.linqs.psl.reasoner.admm.ADMMReasonerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

public abstract class Scorer extends Observable implements ModelApplication
{

	/**
	 * Prefix of property keys used by this class.
	 * 
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "scoring";
	
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
	protected TrainingMap trainingMap;
	protected Reasoner reasoner;
	

	public Scorer(Model model, Database rvDB, Database observedDB, ConfigBundle config) {
		this.model = model;
		this.rvDB = rvDB;
		this.observedDB = observedDB;
		this.config = config;

		kernels = new ArrayList<WeightedRule>();
		immutableKernels = new ArrayList<WeightedRule>();



	public double scoreModel() 
		throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		/* Gathers the CompatibilityKernels */
		for (WeightedRule k : Iterables.filter(model.getRules(), WeightedRule.class))
			if (k.isWeightMutable())
				kernels.add(k);
			else
				immutableKernels.add(k);
		
		initGroundModel();
		
		/* Score the current model */
		Double currentModelScore = doScoring();
		
		kernels.clear();
		cleanUpGroundModel();

		return currentModelScore;
	}


	protected abstract double doScoring();



	protected void initGroundModel()
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		trainingMap = new TrainingMap(rvDB, observedDB);
		reasoner = ((ReasonerFactory) config.getFactory(REASONER_KEY, REASONER_DEFAULT)).getReasoner(config);
		if (trainingMap.getLatentVariables().size() > 0)
			throw new IllegalArgumentException("All RandomVariableAtoms must have " +
					"corresponding ObservedAtoms. Latent variables are not supported " +
					"by this WeightLearningApplication. " +
					"Example latent variable: " + trainingMap.getLatentVariables().iterator().next());
		Grounding.groundAll(model, trainingMap, reasoner);
	}
	

	protected void cleanUpGroundModel() {
		trainingMap = null;
		reasoner.close();
		reasoner = null;
	}

	@Override
	public void close() {
		model = null;
		rvDB = null;
		config = null;
	}
	
	/**
	 * Sets RandomVariableAtoms with training labels to their observed values.
	 */
	protected void setLabeledRandomVariables() {
		for (Map.Entry<RandomVariableAtom, ObservedAtom> e : trainingMap.getTrainingMap().entrySet()) {
			e.getKey().setValue(e.getValue().getValue());
		}
	}


}
