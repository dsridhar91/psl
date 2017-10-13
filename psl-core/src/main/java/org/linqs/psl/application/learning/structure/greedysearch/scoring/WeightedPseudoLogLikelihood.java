package org.linqs.psl.application.learning.structure.greedysearch.scoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.linqs.psl.model.ConstraintBlocker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

public class WeightedPseudoLogLikelihood extends Scorer{

	private static final Logger log = LoggerFactory.getLogger(WeightedPseudoLogLikelihood.class);
	
	/**
	 * Prefix of property keys used by this class.
	 * 
	 * @see ConfigManager
	 */
	public static final String CONFIG_PREFIX = "wpll";
	
	/**
	 * Key for positive double property scaling the L2 regularization
	 * (\lambda / 2) * ||w||^2
	 */
	public static final String L2_REGULARIZATION_KEY = CONFIG_PREFIX + ".l2regularization";
	/** Default value for L2_REGULARIZATION_KEY */
	public static final double L2_REGULARIZATION_DEFAULT = 0.0;
	
	/**
	 * Key for positive double property scaling the L1 regularization
	 * \gamma * |w|
	 */
	public static final String L1_REGULARIZATION_KEY = CONFIG_PREFIX + ".l1regularization";
	/** Default value for L1_REGULARIZATION_KEY */
	public static final double L1_REGULARIZATION_DEFAULT = 0.0;

	/**
	 * Key for Boolean property that indicates whether to scale pseudolikelihood by number of groundings per predicate
	 */
	public static final String SCALE_PLL_KEY = CONFIG_PREFIX + ".scalepll";
	/** Default value for SCALE_GRADIENT_KEY */
	public static final boolean SCALE_PLL_DEFAULT = true;
		
	protected final double l2Regularization;
	protected final double l1Regularization;
	protected final boolean scalePLL;
	protected double[] truthIncompatibility;
	protected double[] expectedIncompatibility;

	public WeightedPseudoLogLikelihood(Model model, Database rvDB, Database observedDB, ConfigBundle config) {
		super(model, rvDB, observedDB, config);
		
		l2Regularization = config.getDouble(L2_REGULARIZATION_KEY, L2_REGULARIZATION_DEFAULT);
		if (l2Regularization < 0)
			throw new IllegalArgumentException("L2 regularization parameter must be non-negative.");
		l1Regularization = config.getDouble(L1_REGULARIZATION_KEY, L1_REGULARIZATION_DEFAULT);
		if (l1Regularization < 0)
			throw new IllegalArgumentException("L1 regularization parameter must be non-negative.");

		scaleGradient = config.getBoolean(SCALE_PLL_KEY, SCALE_PLL_DEFAULT);
	}

		@Override
	protected double doScoring() {

		double[] avgWeights = new double[kernels.size()];

		/*Stub: loop over the learned weights for current kernels and add to avgWeights*/
		/* if scaling flag is true, compute num groundings for each target predicate and store in hashmap*/

		/*Pseudocode to implement:
		*	for atom in random variable atoms:
		*		compute log P_pseudo(atom | MB(atom)) = w * observed distance to satisfaction
		*		compute log Z(P_pseudo)

		*		return log P_pseudo - log Z(P_pseudo)
		*/

	}

	protected double[] computeObservedIncomp() {
		numGroundings = new double[kernels.size()];
		double[] truthIncompatibility = new double[kernels.size()];
		setLabeledRandomVariables();
		
		/* Computes the observed incompatibilities and numbers of groundings */
		for (int i = 0; i < kernels.size(); i++) {
			for (GroundRule gk : reasoner.getGroundKernels(kernels.get(i))) {
				truthIncompatibility[i] += ((WeightedGroundRule) gk).getIncompatibility();
				numGroundings[i]++;
			}
		}
		
		return truthIncompatibility;
	}
	
	/**
	 * Computes the expected (unweighted) total incompatibility of the
	 * {@link WeightedGroundRule GroundCompatibilityKernels} in reasoner
	 * for each {@link WeightedRule}.
	 * 
	 * @return expected incompatibilities, ordered according to kernels
	 */
	protected double[] computeExpectedIncomp(){

		/*TODO: implement this following MaxPseudoLikelihood*/

	}
	
	protected double computeRegularizer() {
		double l2 = 0;
		double l1 = 0;
		for (int i = 0; i < kernels.size(); i++) {
			l2 += Math.pow(kernels.get(i).getWeight().getWeight(), 2);
			l1 += Math.abs(kernels.get(i).getWeight().getWeight());
		}
		return 0.5 * l2Regularization * l2 + l1Regularization * l1;
	}

	


}