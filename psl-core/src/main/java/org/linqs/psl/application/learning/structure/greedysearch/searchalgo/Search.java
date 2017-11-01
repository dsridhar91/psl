package org.linqs.psl.application.learning.structure.greedysearch.searchalgo;

import org.linqs.psl.application.ModelApplication;
import org.linqs.psl.application.learning.structure.greedysearch.clauseconstruction.ClauseConstructor;
import org.linqs.psl.application.learning.structure.greedysearch.scorer.WeightedPseudoLogLikelihood;
import org.linqs.psl.application.learning.weight.maxlikelihood.MaxPseudoLikelihood;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.config.Factory;
import org.linqs.psl.database.Database;
import org.linqs.psl.model.Model;
import org.linqs.psl.model.atom.ObservedAtom;
import org.linqs.psl.model.atom.RandomVariableAtom;
import org.linqs.psl.model.rule.WeightedRule;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Search extends Observable implements ModelApplication
{

	/**
	 * Prefix of property keys used by this class.
	 * 
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "search";

	protected Model model;
	protected Database rvDB, observedDB;
	protected ConfigBundle config;
	protected ClauseConstructor clConstr;
	protected double startingScore;
	protected MaxPseudoLikelihood mpll;
	protected WeightedPseudoLogLikelihood wpll;

	public Search(Model model, Database rvDB, Database observedDB, ConfigBundle config, double startingScore) {
		this.model = model;
		this.rvDB = rvDB;
		this.observedDB = observedDB;
		this.config = config;
		this.startingScore = startingScore;

		mpll = new MaxPseudoLikelihood(model, rvDB, observedDB, config);
		wpll = new WeightedPseudoLogLikelihood(model, rvDB, observedDB, config);

	}

	public abstract Set<WeightedRule> doSearch();


	@Override
	public void close() {
		model = null;
		rvDB = null;
		config = null;
	}


}
