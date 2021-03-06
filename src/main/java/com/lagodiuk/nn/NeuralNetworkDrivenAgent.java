/*******************************************************************************
 * Copyright 2012 Yuriy Lagodiuk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.lagodiuk.nn;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.lagodiuk.agent.AbstractAgent;
import com.lagodiuk.agent.FertileAgent;
import com.lagodiuk.agent.IAgent;
import com.lagodiuk.agent.IFood;
import com.lagodiuk.environment.IEnvironment;
import com.lagodiuk.nn.genetic.OptimizableNeuralNetwork;

public class NeuralNetworkDrivenAgent extends FertileAgent {
	public static final double EYSIGHT_ANGLE = Math.PI / 4;
	public static final double EYESIGHT_DISTANCE = 100;
	private static final double MAX_DELTA_ANGLE = 1;
	protected static final double MAX_AGENTS_DISTANCE = 5;
	private static final double AGENT = -10;
	private static final double EMPTY = 0;
	private static final double FOOD = 10;
	private static final int MUTATE_CHANCE_NEWBORN = 10;

	@XmlElement
	private volatile NeuralNetwork brain;
	@XmlElement
	private int generation = 0;
	@XmlTransient
	private SortedSet<AbstractAgent> agentsInSight = null;
	@XmlTransient
	private SortedSet<AbstractAgent> foodInSight = null;

	private static volatile long countMutation = 0;

	@XmlTransient
	private Random random = new Random();

	@SuppressWarnings("unused")
	private NeuralNetworkDrivenAgent() {
		// Required by JAXB
	}

	public NeuralNetworkDrivenAgent(double x, double y, double angle, double speed) {
		super(x, y, angle, speed);
		this.generation = 0;
	}

	/**
	 * Animating of agents and evolving best brain - might be in different
	 * threads <br/>
	 * Synchronization prevents from race condition when trying to set new
	 * brain, while method "interact" runs <br/>
	 * <br/>
	 * TODO Maybe consider to use non-blocking technique. But at the moment this
	 * simplest solution doesn't cause any overheads
	 */
	public synchronized void setBrain(NeuralNetwork brain) {
		this.brain = brain;
	}

	public SortedSet<AbstractAgent> getFoodInSight() {
		return foodInSight;
	}

	@Override
	public synchronized void evaluate(IEnvironment env) {
		super.evaluate(env);

		agentsInSight = new TreeSet<AbstractAgent>(sorterByDistance);
		foodInSight = new TreeSet<AbstractAgent>(sorterByDistance);
		for (IAgent iagent : env.getAgents()) {
			AbstractAgent agent = (AbstractAgent) iagent;
			if (agent.isAlive() && inSight(agent, env)) {
				if (agent instanceof IFood) {
					foodInSight.add(agent);
				} else {
					agentsInSight.add(agent);
				}
			}
		}
	}

	/**
	 * Synchronization prevents from race condition when trying to set new
	 * brain, while method "interact" runs <br/>
	 * <br/>
	 * TODO Maybe consider to use non-blocking technique. But at the moment this
	 * simplest solution doesn't cause any overheads
	 */
	@Override
	public synchronized void interact(IEnvironment env) {
		super.interact(env);

		if (isAlive()) {
			List<Double> nnInputs = this.createNnInputs(env);

			this.activateNeuralNetwork(nnInputs);

			int neuronsCount = this.brain.getNeuronsCount();
			double newBornEnergy = this.brain.getAfterActivationSignal(neuronsCount - 4);
			double parentPostBirthEnergy = this.brain.getAfterActivationSignal(neuronsCount - 3);
			double deltaAngle = this.brain.getAfterActivationSignal(neuronsCount - 2);
			double deltaSpeed = this.brain.getAfterActivationSignal(neuronsCount - 1);

			deltaSpeed = this.avoidNaNAndInfinity(deltaSpeed);
			deltaAngle = this.avoidNaNAndInfinity(deltaAngle);

			double newSpeed = this.getSpeed() + deltaSpeed;
			double newAngle = this.getAngle() + this.normalizeDeltaAngle(deltaAngle);

			this.setAngle(newAngle);
			this.setSpeed(newSpeed);
			this.setNewbornEnergy((int) newBornEnergy);
			this.setParentingEnergy((int) parentPostBirthEnergy);
		}
	}

	private double avoidNaNAndInfinity(double x) {
		if ((Double.isNaN(x)) || Double.isInfinite(x)) {
			x = 0;
		}
		return x;
	}

	private void activateNeuralNetwork(List<Double> nnInputs) {
		for (int i = 0; i < nnInputs.size(); i++) {
			this.brain.putSignalToNeuron(i, nnInputs.get(i));
		}
		this.brain.activate();
	}

	protected List<Double> createNnInputs(IEnvironment environment) {
		List<Double> nnInputs = new LinkedList<Double>();
		nnInputs.add((double) getEnergy());

		double rx = this.getRx();
		double ry = this.getRy();

		double x = this.getX();
		double y = this.getY();

		AbstractAgent nearestFood = null;
		if (!foodInSight.isEmpty()) {
			nearestFood = foodInSight.first();
		}
		if (nearestFood != null) {
			double nearestFoodDistanceInput = environment.squareOfDistance(this, nearestFood);

			double foodDirectionVectorX = nearestFood.getX() - x;
			double foodDirectionVectorY = nearestFood.getY() - y;

			// left/right cos
			double foodDirectionCosTeta =
					Math.signum(this.pseudoScalarProduct(rx, ry, foodDirectionVectorX, foodDirectionVectorY))
							* this.cosTeta(rx, ry, foodDirectionVectorX, foodDirectionVectorY);

			nnInputs.add(FOOD);
			nnInputs.add(nearestFoodDistanceInput);
			nnInputs.add(foodDirectionCosTeta);

		} else {
			nnInputs.add(EMPTY);
			nnInputs.add(0.0);
			nnInputs.add(0.0);
		}

		AbstractAgent nearestAgent = null;
		if (!agentsInSight.isEmpty()) {
			nearestAgent = agentsInSight.first();
		}
		if (nearestAgent != null) {
			double nearestAgentDistanceInput = environment.squareOfDistance(this, nearestAgent);
			double agentDirectionVectorX = nearestAgent.getX() - x;
			double agentDirectionVectorY = nearestAgent.getY() - y;

			// left/right cos
			double agentDirectionCosTeta =
					Math.signum(this.pseudoScalarProduct(rx, ry, agentDirectionVectorX, agentDirectionVectorY))
							* this.cosTeta(rx, ry, agentDirectionVectorX, agentDirectionVectorY);

			nnInputs.add(AGENT);
			nnInputs.add(nearestAgentDistanceInput);
			nnInputs.add(agentDirectionCosTeta);

		} else {
			nnInputs.add(EMPTY);
			nnInputs.add(0.0);
			nnInputs.add(0.0);
		}
		return nnInputs;
	}

	private boolean inSightInternal(double x, double y) {
		double vx1 = x - this.getX();
		double vy1 = y - this.getY();
		if (vx1 > EYESIGHT_DISTANCE || vy1 > EYESIGHT_DISTANCE
				|| vx1 * vx1 + vy1 * vy1 > EYESIGHT_DISTANCE * EYESIGHT_DISTANCE) {
			return false;
		}
		double crossProduct = this.cosTeta(this.getRx(), this.getRy(), x - this.getX(), y - this.getY());
		boolean result = crossProduct > Math.cos(EYSIGHT_ANGLE);
		return result;
	}

	public boolean inSight(IAgent agent, IEnvironment env) {
		if (!isAlive()) {
			return false;
		}

		double x = agent.getX();
		double y = agent.getY();
		if (inSightInternal(x, y)) {
			return true;
		}

		// Agents can move across boundaries so should also see
		final double maxY = env.getHeight();
		final double maxX = env.getWidth();
		if (x < EYESIGHT_DISTANCE) {
			x = x + maxX;
		} else if (x > maxX - EYESIGHT_DISTANCE) {
			x = x - maxX;
		}
		if (x != agent.getX()) {
			if (inSightInternal(x, agent.getY())) {
				return true;
			}
		}
		if (y < EYESIGHT_DISTANCE) {
			y = y + maxY;
		} else if (y > maxY - EYESIGHT_DISTANCE) {
			y = y - maxY;
		}
		if (y != agent.getY()) {
			if (inSightInternal(agent.getX(), y)) {
				return true;
			}
		}
		if (x != agent.getX() && y != agent.getY()) {
			if (inSightInternal(x, y)) {
				return true;
			}
		}

		return false;
	}

	protected double cosTeta(double vx1, double vy1, double vx2, double vy2) {
		double v1 = this.module(vx1, vy1);
		double v2 = this.module(vx2, vy2);
		if (v1 == 0) {
			v1 = 1E-5;
		}
		if (v2 == 0) {
			v2 = 1E-5;
		}
		double ret = ((vx1 * vx2) + (vy1 * vy2)) / (v1 * v2);
		return ret;
	}

	protected double pseudoScalarProduct(double vx1, double vy1, double vx2, double vy2) {
		return (vx1 * vy2) - (vy1 * vx2);
	}

	private double normalizeDeltaAngle(double angle) {
		double abs = Math.abs(angle);
		if (abs > MAX_DELTA_ANGLE) {
			double sign = Math.signum(angle);
			angle = sign * MAX_DELTA_ANGLE;
		}
		return angle;
	}

	public static OptimizableNeuralNetwork randomNeuralNetworkBrain() {
		OptimizableNeuralNetwork nn = new OptimizableNeuralNetwork(15);
		for (int i = 0; i < 15; i++) {
			ThresholdFunction f = ThresholdFunction.getRandomFunction();
			nn.setNeuronFunction(i, f, f.getDefaultParams());
		}
		for (int i = 0; i < 6; i++) {
			nn.setNeuronFunction(i, ThresholdFunction.LINEAR, ThresholdFunction.LINEAR.getDefaultParams());
		}
		for (int i = 0; i < 6; i++) {
			for (int j = 6; j < 15; j++) {
				nn.addLink(i, j, Math.random());
			}
		}
		for (int i = 6; i < 15; i++) {
			for (int j = 6; j < 15; j++) {
				if (i < j) {
					nn.addLink(i, j, Math.random());
				}
			}
		}
		return nn;
	}

	private void mutate(int mutateChance) {
		if (brain instanceof OptimizableNeuralNetwork && random.nextInt() % mutateChance == 0) {
			countMutation++;
			brain = ((OptimizableNeuralNetwork) brain).mutate();
			if (random.nextInt() % mutateChance == 0) {
				// double mutation
				countMutation++;
				brain = ((OptimizableNeuralNetwork) brain).mutate();
			}
			generation++;
		}
	}

	@Override
	public NeuralNetworkDrivenAgent reproduce(IEnvironment env) {
		NeuralNetworkDrivenAgent newAgent = null;
		int parentEnergy = getEnergy();
		int childEnergy = getNewbornEnergy();
		double newAngle = random.nextDouble();
		double newSpeed = 0;
		newAgent = new NeuralNetworkDrivenAgent(this.getX(), this.getY(), newAngle, newSpeed);
		newAgent.generation = this.generation;
		newAgent.setBrain(brain);
		newAgent.mutate(MUTATE_CHANCE_NEWBORN);
		newAgent.setEnergy(childEnergy);
		setEnergy(parentEnergy - newAgent.getEnergy());
		env.addAgent(newAgent);
		return newAgent;
	}

	@XmlTransient
	public int getGeneration() {
		return this.generation;
	}

	public void setGeneration(int gen) {
		this.generation = gen;
	}

	static public long getMutationCount() {
		return countMutation;
	}

	static public void setMutationCount(long newCountMutation) {
		countMutation = newCountMutation;
	}
}
