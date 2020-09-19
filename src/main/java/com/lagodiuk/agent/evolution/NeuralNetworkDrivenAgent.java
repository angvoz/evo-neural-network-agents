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
import com.lagodiuk.nn.NeuralNetwork;
import com.lagodiuk.nn.ThresholdFunction;
import com.lagodiuk.nn.genetic.OptimizableNeuralNetwork;

public class NeuralNetworkDrivenAgent extends FertileAgent {
	private static final double RADIUS = 5;

	public static final double maxSpeed = 4;

	private static final double maxDeltaAngle = 1;

	protected static final double maxAgentsDistance = 5;

	private static final double AGENT = -10;

	private static final double EMPTY = 0;

	private static final double FOOD = 10;

	private static final double MUTATE_FACTOR = 10;

	private volatile NeuralNetwork brain;

	private static volatile long countMutation = 0;

	private int generation;

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
							otherAgent.setSpeed(maxSpeed);
							otherAgent.move();
						}
						if (this.getSpeed() == 0) {
							this.setSpeed(maxSpeed);
						}
						break;
					}
				}
			}
		}

		this.move();
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

	protected List<Double> createNnInputs(AgentsEnvironment environment) {
		// Find nearest food
		IFood nearestFood = null;
		double nearestFoodDist = Double.MAX_VALUE;

		for (IFood currFood : environment.getFood()) {
			// agent can see only ahead
			if (this.inSight(currFood)) {
				double currFoodDist = this.distanceTo(currFood);
				if ((nearestFood == null) || (currFoodDist <= nearestFoodDist)) {
					nearestFood = currFood;
					nearestFoodDist = currFoodDist;
				}
			}
		}

		// Find nearest agent
		FertileAgent nearestAgent = null;
		double nearestAgentDist = maxAgentsDistance;

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

		if (nearestFood != null) {
			double foodDirectionVectorX = nearestFood.getX() - x;
			double foodDirectionVectorY = nearestFood.getY() - y;

			// left/right cos
			double foodDirectionCosTeta =
					Math.signum(this.pseudoScalarProduct(rx, ry, foodDirectionVectorX, foodDirectionVectorY))
							* this.cosTeta(rx, ry, foodDirectionVectorX, foodDirectionVectorY);

			nnInputs.add(FOOD);
			nnInputs.add(nearestFoodDist);
			nnInputs.add(foodDirectionCosTeta);

		} else {
			nnInputs.add(EMPTY);
			nnInputs.add(0.0);
			nnInputs.add(0.0);
		}

		if (nearestAgent != null) {
			double agentDirectionVectorX = nearestAgent.getX() - x;
			double agentDirectionVectorY = nearestAgent.getY() - y;

			// left/right cos
			double agentDirectionCosTeta =
					Math.signum(this.pseudoScalarProduct(rx, ry, agentDirectionVectorX, agentDirectionVectorY))
							* this.cosTeta(rx, ry, agentDirectionVectorX, agentDirectionVectorY);

			nnInputs.add(AGENT);
			nnInputs.add(nearestAgentDist);
			nnInputs.add(agentDirectionCosTeta);

		} else {
			nnInputs.add(EMPTY);
			nnInputs.add(0.0);
			nnInputs.add(0.0);
		}
		return nnInputs;
	}

	protected boolean inSight(IAgent agent) {
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
		if (speed > maxSpeed) {
			speed = maxSpeed;
		} else if (speed < 0) {
			speed = 0;
		}
		return speed;
	}

	private double normalizeDeltaAngle(double angle) {
		double abs = Math.abs(angle);
		if (abs > maxDeltaAngle) {
			double sign = Math.signum(angle);
			angle = sign * maxDeltaAngle;
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

	@Override
	public NeuralNetworkDrivenAgent reproduce() {
		if (getEnergy() >= PREGNANCY_ENERGY) {
			Random random = new Random();
			double newAngle = random.nextDouble();
			double newSpeed = 0;
			NeuralNetworkDrivenAgent newAgent = new NeuralNetworkDrivenAgent(this.getX(), this.getY(), newAngle, newSpeed);
			newAgent.generation = this.generation;
			NeuralNetwork newBrain;
			if (this.brain instanceof OptimizableNeuralNetwork && random.nextInt() % MUTATE_FACTOR == 0) {
				countMutation++;
				newBrain = ((OptimizableNeuralNetwork) this.brain).mutate();
				if (random.nextInt() % MUTATE_FACTOR == 0) {
					// double mutation
					countMutation++;
					newBrain = ((OptimizableNeuralNetwork) newBrain).mutate();
				}
				newAgent.generation++;
			} else {
				newBrain = this.brain;
			}
			newAgent.setBrain(newBrain);
			setEnergy(getEnergy() - newAgent.getEnergy());
			return newAgent;
		}
		return null;
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
