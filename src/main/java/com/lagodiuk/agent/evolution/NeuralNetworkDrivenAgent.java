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
package com.lagodiuk.agent.evolution;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.lagodiuk.agent.AgentsEnvironment;
import com.lagodiuk.agent.FertileAgent;
import com.lagodiuk.agent.IAgent;
import com.lagodiuk.agent.IFood;
import com.lagodiuk.agent.MovingFood;
import com.lagodiuk.nn.NeuralNetwork;
import com.lagodiuk.nn.ThresholdFunction;
import com.lagodiuk.nn.genetic.OptimizableNeuralNetwork;

public class NeuralNetworkDrivenAgent extends FertileAgent {
	private static final double RADIUS = 5;
	public static final double MAX_SPEED = 4;
	private static final double MAX_DELTA_ANGLE = 1;
	protected static final double MAX_AGENTS_DISTANCE = 150;
	private static final double MUTATE_FACTOR = 1000;
	private static final int NUMBER_OF_NEURONS = 15;

	private volatile NeuralNetwork brain;

	private static volatile long countMutation = 0;
	private int generation = 0;

	public NeuralNetworkDrivenAgent(double x, double y, double angle, double speed) {
		super(x, y, angle, speed);
		this.generation = 0;
	}

	@Override
	public double getRadius() {
		return RADIUS;
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

	/**
	 * Synchronization prevents from race condition when trying to set new
	 * brain, while method "interact" runs <br/>
	 * <br/>
	 * TODO Maybe consider to use non-blocking technique. But at the moment this
	 * simplest solution doesn't cause any overheads
	 */
	@Override
	public synchronized void interact(AgentsEnvironment env) {
		mutate();

		List<Double> nnInputs = this.createNnInputs(env);

		this.activateNeuralNetwork(nnInputs);

		int neuronsCount = this.brain.getNeuronsCount();
		double deltaAngle = this.brain.getAfterActivationSignal(neuronsCount - 2);
		double deltaSpeed = this.brain.getAfterActivationSignal(neuronsCount - 1);

		deltaSpeed = this.avoidNaNAndInfinity(deltaSpeed);
		deltaAngle = this.avoidNaNAndInfinity(deltaAngle);

		double newSpeed = this.normalizeSpeed(this.getSpeed() + deltaSpeed);
		double newAngle = this.getAngle() + this.normalizeDeltaAngle(deltaAngle);

		this.setAngle(newAngle);
		this.setSpeed(newSpeed);
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

	@SuppressWarnings("unused")
	protected List<Double> createNnInputs(AgentsEnvironment environment) {
		// Find nearest food
		IFood nearestFood_1 = null;
		double nearestFoodDist_1 = Double.MAX_VALUE;
		IFood nearestFood_2 = null;
		double nearestFoodDist_2 = Double.MAX_VALUE;

		for (IFood currFood : environment.getFood()) {
			// agent can see only ahead
			if (this.inSight(currFood)) {
				double currFoodDist = this.distanceTo(currFood);
				if ((nearestFood_1 == null) || (currFoodDist <= nearestFoodDist_1)) {
					nearestFood_2 = nearestFood_1;
					nearestFoodDist_2 = nearestFoodDist_1;
					nearestFood_1 = currFood;
					nearestFoodDist_1 = currFoodDist;
				}
			}
		}

		// Find nearest agent
		FertileAgent nearestAgent = null;
		double nearestAgentDist = MAX_AGENTS_DISTANCE;

		for (FertileAgent currAgent : environment.getFishes()) {
			// agent can see only ahead
			if ((this != currAgent) && (this.inSight(currAgent))) {
				double currAgentDist = this.distanceTo(currAgent);
				if (currAgentDist <= nearestAgentDist) {
					nearestAgent = currAgent;
					nearestAgentDist = currAgentDist;
				}
			}
		}

		List<Double> nnInputs = new LinkedList<Double>();

		double rx = this.getRx();
		double ry = this.getRy();

		double x = this.getX();
		double y = this.getY();

		nnInputs.add((double) getEnergy());

		double foodDirectionCosTeta_1 = 0;
		double nearestFoodSpeed_1 = 0;
		double nearestFoodAngleDiff_1 = 0;
		if (nearestFood_1 != null) {
			double foodDirectionVectorX = nearestFood_1.getX() - x;
			double foodDirectionVectorY = nearestFood_1.getY() - y;

			// left/right cos
			foodDirectionCosTeta_1 =
					Math.signum(this.pseudoScalarProduct(rx, ry, foodDirectionVectorX, foodDirectionVectorY))
					* this.cosTeta(rx, ry, foodDirectionVectorX, foodDirectionVectorY);

			if (nearestFood_1 instanceof MovingFood) {
				nearestFoodSpeed_1 = ((MovingFood) nearestFood_1).getSpeed();
				double nearestFoodAngle = ((MovingFood) nearestFood_1).getAngle();
				nearestFoodAngleDiff_1 = this.getAngle() - nearestFoodAngle;
			}
		}

		nnInputs.add(nearestFoodDist_1);
		nnInputs.add(foodDirectionCosTeta_1);
		nnInputs.add(nearestFoodAngleDiff_1);
		nnInputs.add(nearestFoodSpeed_1);

		double foodDirectionCosTeta_2 = 0;
		double nearestFoodSpeed_2 = 0;
		double nearestFoodAngleDiff_2 = 0;
		if (nearestFood_2 != null) {
			double foodDirectionVectorX = nearestFood_2.getX() - x;
			double foodDirectionVectorY = nearestFood_2.getY() - y;

			// left/right cos
			foodDirectionCosTeta_2 =
					Math.signum(this.pseudoScalarProduct(rx, ry, foodDirectionVectorX, foodDirectionVectorY))
					* this.cosTeta(rx, ry, foodDirectionVectorX, foodDirectionVectorY);

			if (nearestFood_2 instanceof MovingFood) {
				nearestFoodSpeed_2 = ((MovingFood) nearestFood_2).getSpeed();
				double nearestFoodAngle = ((MovingFood) nearestFood_2).getAngle();
				nearestFoodAngleDiff_2 = this.getAngle() - nearestFoodAngle;
			}
		}

		nnInputs.add(nearestFoodDist_2);
		nnInputs.add(foodDirectionCosTeta_2);
		nnInputs.add(nearestFoodAngleDiff_2);
		nnInputs.add(nearestFoodSpeed_2);

//		if (nearestAgent != null) {
//			double agentDirectionVectorX = nearestAgent.getX() - x;
//			double agentDirectionVectorY = nearestAgent.getY() - y;
//
//			// left/right cos
//			double agentDirectionCosTeta =
//					Math.signum(this.pseudoScalarProduct(rx, ry, agentDirectionVectorX, agentDirectionVectorY))
//							* this.cosTeta(rx, ry, agentDirectionVectorX, agentDirectionVectorY);
//
//			double nearestAgentSpeed = 0;
//			double nearestAgentAngleDiff = 0;
//			if (nearestAgent instanceof NeuralNetworkDrivenAgent) {
//				nearestAgentSpeed = ((NeuralNetworkDrivenAgent) nearestAgent).getSpeed();
//				double nearestAgentAngle = ((NeuralNetworkDrivenAgent) nearestAgent).getAngle();
//				nearestAgentAngleDiff = this.getAngle() - nearestAgentAngle;
//			}
//
////			nnInputs.add(AGENT);
//			nnInputs.add(nearestAgentDist);
//			nnInputs.add(agentDirectionCosTeta);
//			nnInputs.add(nearestAgentSpeed);
//			nnInputs.add(nearestAgentAngleDiff);
//
//		} else {
////			nnInputs.add(EMPTY);
//			nnInputs.add(0.0);
//			nnInputs.add(0.0);
//			nnInputs.add(0.0);
//			nnInputs.add(0.0);
//		}
		return nnInputs;
	}

	protected boolean inSight(IAgent agent) {
		if (distanceTo(agent) > MAX_AGENTS_DISTANCE) {
			return false;
		}
		double crossProduct = this.cosTeta(this.getRx(), this.getRy(), agent.getX() - this.getX(), agent.getY() - this.getY());
		return (crossProduct > 0);
	}

	protected double distanceTo(IAgent agent) {
		return this.module(agent.getX() - this.getX(), agent.getY() - this.getY());
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

	protected double module(double vx1, double vy1) {
		return Math.sqrt((vx1 * vx1) + (vy1 * vy1));
	}

	protected double pseudoScalarProduct(double vx1, double vy1, double vx2, double vy2) {
		return (vx1 * vy2) - (vy1 * vx2);
	}

	private double normalizeSpeed(double speed) {
		if (speed > MAX_SPEED) {
			speed = MAX_SPEED;
		} else if (speed < 0) {
			speed = 0;
		}
		return speed;
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
		final int MIDDLE_POINT = 6;
		OptimizableNeuralNetwork nn = new OptimizableNeuralNetwork(NUMBER_OF_NEURONS);
		for (int i = 0; i < NUMBER_OF_NEURONS; i++) {
			ThresholdFunction f = ThresholdFunction.getRandomFunction();
			nn.setNeuronFunction(i, f, f.getRandomParams());
		}
//		for (int i = 0; i < MIDDLE_POINT; i++) {
//			nn.setNeuronFunction(i, ThresholdFunction.LINEAR, ThresholdFunction.LINEAR.getDefaultParams());
//		}
		for (int i = 0; i < MIDDLE_POINT; i++) {
			for (int j = MIDDLE_POINT; j < NUMBER_OF_NEURONS; j++) {
				nn.addLink(i, j, Math.random() - 0.5);
			}
		}
		for (int i = MIDDLE_POINT; i < NUMBER_OF_NEURONS; i++) {
			for (int j = MIDDLE_POINT; j < NUMBER_OF_NEURONS; j++) {
				if (i < j) {
					nn.addLink(i, j, Math.random() - 0.5);
				}
			}
		}
		return nn;
	}

	@Override
	public void move(AgentsEnvironment env) {
		double newAngle = getAngle();
		for (FertileAgent otherAgent : env.getFishes()) {
			if (this != otherAgent) {
				double futureDistance = this.module(otherAgent.getX() - (this.getX() + this.getRx() * this.getSpeed()),
						otherAgent.getY() - (this.getY() + this.getRy() * this.getSpeed()));
				if (futureDistance < this.getRadius() * 2 + 3) {
					// Check if the agent moves towards the other one (angle less than 90 degree)
					double ux = otherAgent.getX() - this.getX();
					double uy = otherAgent.getY() - this.getY();
					double vx = this.getRx();
					double vy = this.getRy();
					double d = ux*vx + uy*vy;
					if (d > 0) {
						// if less than 90 degree move the agent in opposite direction
						newAngle = Math.atan2(uy, ux) + Math.PI/2;
						this.setAngle(newAngle);
						// if the other agent not moving move it
						if (otherAgent.getSpeed() == 0) {
							otherAgent.setAngle(newAngle + Math.PI);
							otherAgent.setSpeed(MAX_SPEED);
							otherAgent.move(env);
						}
						if (this.getSpeed() == 0) {
							this.setSpeed(MAX_SPEED);
						}
						break;
					}
				}
			}
		}

		super.move(env);
	}

	private void mutate() {
		Random random = new Random();
		if (this.brain instanceof OptimizableNeuralNetwork && random.nextInt() % MUTATE_FACTOR == 0) {
			generation++;
			countMutation++;
			OptimizableNeuralNetwork newBrain = ((OptimizableNeuralNetwork) this.brain).mutate();
			if (random.nextInt() % MUTATE_FACTOR == 0) {
				// double mutation
				countMutation++;
				newBrain = newBrain.mutate();
			}
			setBrain(newBrain);
		}
	}

	@Override
	public void reproduce(AgentsEnvironment env) {
		if (getEnergy() >= PREGNANCY_ENERGY) {
			Random random = new Random();
			double newAngle = random.nextDouble();
			double newSpeed = 0;
			NeuralNetworkDrivenAgent newAgent = new NeuralNetworkDrivenAgent(this.getX(), this.getY(), newAngle, newSpeed);
			newAgent.generation = this.generation;
			newAgent.setBrain(this.brain.clone());
			setEnergy(getEnergy() - newAgent.getEnergy());
			env.addAgent(newAgent);
		}
	}

	public int getGeneration() {
		return this.generation;
	}

	public void setGeneration(int gen) {
		this.generation = gen;
	}

	static public long getMutationCount() {
		return countMutation;
	}
}
