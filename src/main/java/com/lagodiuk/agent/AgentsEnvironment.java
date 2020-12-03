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
package com.lagodiuk.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.lagodiuk.agent.evolution.NeuralNetworkDrivenAgent;
import com.lagodiuk.nn.genetic.OptimizableNeuralNetwork;

public class AgentsEnvironment {
	public static final int ENDANGERED_SPECIES = 10;
	public static final boolean FOOD_CELL_DIVISION = true;

	private int width;
	private int height;
	private double time;
	private int energyReserve;
	private int countEatenFood;

	private Random random = new Random();

	private List<AbstractAgent> agents = new ArrayList<AbstractAgent>();

	private List<AgentsEnvironmentObserver> listeners = new ArrayList<AgentsEnvironmentObserver>();

	public AgentsEnvironment(int width, int height) {
		this.width = width;
		this.height = height;
		this.time = 0;
		this.energyReserve = 0;
	}

	public void addListener(AgentsEnvironmentObserver listener) {
		this.listeners.add(listener);
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public double getTime() {
		return this.time;
	}

	public List<FertileAgent> getFishes() {
		List<FertileAgent> filtered = new ArrayList<FertileAgent>();
		for (IAgent agent : this.agents) {
			if (agent instanceof FertileAgent) {
				filtered.add((FertileAgent) agent);
			}
		}
		return filtered;
	}

	public List<IFood> getFood() {
		List<IFood> filtered = new ArrayList<IFood>();
		for (IAgent agent : this.agents) {
			if (agent instanceof IFood) {
				filtered.add((IFood) agent);
			}
		}
		return filtered;
	}

	private IFood createRandomFood(int width, int height) {
		boolean staticFood = false;
		int x = random.nextInt(width);
		int y = random.nextInt(height);

		IFood food = null;
		if (staticFood) {
			food = new StaticFood(x, y);
		} else {
			double speed = random.nextDouble() * 2;
			double direction = random.nextDouble() * 2 * Math.PI;

			food = new MovingFood(x, y, direction, speed);
		}
		return food;
	}

	private IFood reproduceFood(IFood food) {
		double x = food.getX();
		double y = food.getY();

		double speed = random.nextDouble() * 2;
		double direction = random.nextDouble() * 2 * Math.PI;
		IFood newFood = new MovingFood(x, y, direction, speed);

		return newFood;
	}

	private void feedAgents() {
		countEatenFood = 0;
		for (IFood food : getFood()) {
			for (FertileAgent fish : getFishes()) {
				double fishRadius = fish.getRadius();
				double deltaY = Math.abs(food.getY() - fish.getY());
				if (deltaY < fishRadius) {
					double deltaX = Math.abs(food.getX() - fish.getX());
					if (deltaX < fishRadius) {
						double distanceToFood = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
						if (distanceToFood < fishRadius) {
							countEatenFood++;
							removeFood(food);
							fish.feed(food);
							if (fish.isPregnant()) {
								FertileAgent newFish = fish.reproduce();
								addAgent(newFish);
							}
							break;
						}
					}
				}
			}
		}
	}

	public synchronized void timeStep() {
		// Move agents
		for (IAgent agent : this.getAgents()) {
			agent.interact(this);
			this.avoidMovingOutsideOfBounds(agent);
		}

		// Feed Agents
		this.feedAgents();
		growAgentsOlder();
		if (getFishes().size() <= ENDANGERED_SPECIES) {
			// Do not allow life go extinct in this simulation
			addNewRandomFish();
		}
		if (getFishes().size() > ENDANGERED_SPECIES) {
			addNewFood();
		}

		for (AgentsEnvironmentObserver l : this.listeners) {
			// keeps score for tournaments
			l.notify(this);
		}

		this.time++;
	}

	private void addNewFood() {
		while (energyReserve > 0) {
			int foodEnergy = 1;
			List<IFood> allFood = this.getFood();
			IFood food;
			if (FOOD_CELL_DIVISION && allFood.size() > 0) {
				int index = random.nextInt(allFood.size());
				food = reproduceFood(allFood.get(index));
				this.addFood(food);
			} else {
				food = createRandomFood(this.getWidth(), this.getHeight());
				this.addFood(food);
			}
			energyReserve -= foodEnergy;
		}
	}

	private void addNewRandomFish() {
		if (energyReserve >= FertileAgent.NEWBORN_ENERGY_DEFAULT) {
			// Add new random agent
			int x = random.nextInt(this.getWidth());
			int y = random.nextInt(this.getHeight());
			double direction = random.nextDouble() * 2 * Math.PI;
			double speed = random.nextDouble() * MovingAgent.MAX_SPEED;
			NeuralNetworkDrivenAgent newAgent = new NeuralNetworkDrivenAgent(x, y, direction, speed);
			OptimizableNeuralNetwork newBrain = NeuralNetworkDrivenAgent.randomNeuralNetworkBrain().mutate();
			newAgent.setBrain(newBrain);
			this.addAgent(newAgent);
			energyReserve -= newAgent.getEnergy();
		}
	}

	private void growAgentsOlder() {
		for (FertileAgent agent : getFishes()) {
			int upkeepEnergy = agent.grow();
			if (agent.getEnergy() <= 0) {
				this.removeAgent(agent);
			}
			energyReserve += upkeepEnergy;
		}
	}

	/**
	 * avoid moving outside of environment
	 */
	private void avoidMovingOutsideOfBounds(IAgent agent) {
		double newX = agent.getX();
		double newY = agent.getY();
		if (newX < 0) {
			newX = this.width - 1;
		}
		if (newY < 0) {
			newY = this.height - 1;
		}
		if (newX > this.width) {
			newX = 1;
		}
		if (newY > this.height) {
			newY = 1;
		}

		agent.setX(newX);
		agent.setY(newY);
	}

	public List<IAgent> getAgents() {
		// to avoid concurrent modification exception
		return new ArrayList<IAgent>(this.agents);
	}

	public synchronized void addAgent(AbstractAgent agent) {
		this.agents.add(agent);
	}

	public synchronized void removeAgent(AbstractAgent agent) {
		this.agents.remove(agent);
	}

	public void addFood(IFood food) {
		addAgent((AbstractAgent) food);
	}

	public void removeFood(IFood food) {
		removeAgent((AbstractAgent) food);
	}

	public int getLongestGeneration() {
		int maxGeneration = -1;
		for (FertileAgent agent : this.getFishes()) {
			if (agent instanceof NeuralNetworkDrivenAgent) {
				maxGeneration = Math.max(((NeuralNetworkDrivenAgent) agent).getGeneration(), maxGeneration);
			}
		}
		return maxGeneration;
	}

	public int getEnergyReserve() {
		return energyReserve;
	}

	public void setEnergyReserve(int energy) {
		energyReserve = energy;
	}

	public int countEatenFood() {
		return countEatenFood;
	}
}
