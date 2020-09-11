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
import java.util.LinkedList;
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

	public List<Agent> getFishes() {
		List<Agent> fishes = new ArrayList<Agent>();
		for (Agent agent : filter(Agent.class)) {
			fishes.add(agent);
		}
		return fishes;
	}

	public List<Food> getFood() {
		List<Food> foods = new ArrayList<Food>();
		for (Food food : filter(Food.class)) {
			foods.add(food);
		}
		return foods;
	}

	private Food createRandomFood(int width, int height) {
		boolean staticFood = false;
		int x = random.nextInt(width);
		int y = random.nextInt(height);

		Food food = null;
		if (staticFood) {
			food = new Food(x, y);
		} else {
			double speed = random.nextDouble() * 2;
			double direction = random.nextDouble() * 2 * Math.PI;

			food = new MovingFood(x, y, direction, speed);
		}
		return food;
	}

	private Food reproduceFood(Food food) {
		double x = food.getX();
		double y = food.getY();

		double speed = random.nextDouble() * 2;
		double direction = random.nextDouble() * 2 * Math.PI;
		Food newFood = new MovingFood(x, y, direction, speed);

		return newFood;
	}

	private void feedAgents() {
		countEatenFood = 0;
		for (Food food : getFood()) {
			for (Agent fish : getFishes()) {
				double fishRadius = fish.getRadius();
				double deltaY = Math.abs(food.getY() - fish.getY());
				if (deltaY < fishRadius) {
					double deltaX = Math.abs(food.getX() - fish.getX());
					if (deltaX < fishRadius) {
						double distanceToFood = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
						if (distanceToFood < fishRadius) {
							countEatenFood++;
							removeAgent(food);
							fish.eatFood(food);
							if (fish.isPregnant()) {
								Agent newFish = fish.reproduce();
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
		for (AbstractAgent agent : this.getAgents()) {
			agent.interact(this);
			this.avoidMovingOutsideOfBounds(agent);
		}

		// Feed Agents
		this.feedAgents();
		growAgentsOlder();
		if (getFishes().size() <= ENDANGERED_SPECIES) {
			addNewRandomAgent();
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
			List<Food> allFood = this.getFood();
			if (FOOD_CELL_DIVISION && allFood.size() > 0) {
				int index = random.nextInt(allFood.size());
				Food food = reproduceFood(allFood.get(index));
				this.addAgent(food);
			} else {
				Food food = createRandomFood(this.getWidth(), this.getHeight());
				this.addAgent(food);
			}
			energyReserve -= foodEnergy;
		}
	}

	private void addNewRandomAgent() {
		// Do not allow life go extinct in this simulation
		if (energyReserve >= Agent.STARTING_ENERGY) {
			// Add new random agent
			int x = random.nextInt(this.getWidth());
			int y = random.nextInt(this.getHeight());
			double direction = random.nextDouble() * 2 * Math.PI;
			NeuralNetworkDrivenAgent newAgent = new NeuralNetworkDrivenAgent(x, y, direction);
			OptimizableNeuralNetwork newBrain = NeuralNetworkDrivenAgent.randomNeuralNetworkBrain().mutate();
			newAgent.setBrain(newBrain);
			this.addAgent(newAgent);
			energyReserve -= newAgent.getEnergy();
		}
	}

	private void growAgentsOlder() {
		for (Agent agent : getFishes()) {
			int upkeepEnergy = agent.growOlder();
			if (agent.getEnergy() <= 0) {
				this.removeAgent(agent);
			}
			energyReserve += upkeepEnergy;
		}
	}

	/**
	 * avoid moving outside of environment
	 */
	private void avoidMovingOutsideOfBounds(AbstractAgent agent) {
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

	public List<AbstractAgent> getAgents() {
		// to avoid concurrent modification exception
		return new LinkedList<AbstractAgent>(this.agents);
	}

	public synchronized void addAgent(AbstractAgent agent) {
		this.agents.add(agent);
	}

	public synchronized void removeAgent(AbstractAgent agent) {
		this.agents.remove(agent);
	}

	@SuppressWarnings("unchecked")
	public <T extends AbstractAgent> Iterable<T> filter(Class<T> clazz) {
		// Guava increases binary size from 70Kb to 2Mb :(
		// return Iterables.filter(this.getAgents(), clazz);
		List<T> filtered = new LinkedList<T>();
		for (AbstractAgent agent : this.getAgents()) {
			if (clazz.isInstance(agent)) {
				filtered.add((T) agent);
			}
		}
		return filtered;
	}

	public int getLongestGeneration() {
		int maxGeneration = -1;
		for (AbstractAgent agent : this.getAgents()) {
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
