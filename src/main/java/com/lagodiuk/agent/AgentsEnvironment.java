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

	private List<IAgent> agents = new ArrayList<IAgent>();

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
		List<FertileAgent> filtered = new LinkedList<FertileAgent>();
		for (IAgent agent : this.agents) {
			if (agent instanceof FertileAgent) {
				filtered.add((FertileAgent) agent);
			}
		}
		return filtered;
	}

	public List<IFood> getFood() {
		List<IFood> filtered = new LinkedList<IFood>();
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

	private void addNewFood() {
		while (energyReserve > 0) {
			int foodEnergy = 1;
			List<IFood> allFood = this.getFood();
			if (FOOD_CELL_DIVISION && allFood.size() > 0) {
				int index = random.nextInt(allFood.size());
				IFood food = allFood.get(index);
				food.reproduce(this);
			} else {
				IFood food = createRandomFood(this.getWidth(), this.getHeight());
				this.addAgent(food);
			}
			energyReserve -= foodEnergy;
		}
	}

	private void addNewRandomFish() {
		if (energyReserve >= FertileAgent.STARTING_ENERGY) {
			// Add new random agent
			int x = random.nextInt(this.getWidth());
			int y = random.nextInt(this.getHeight());
			double direction = random.nextDouble() * 2 * Math.PI;
			double speed = random.nextDouble() * NeuralNetworkDrivenAgent.MAX_SPEED;
			NeuralNetworkDrivenAgent newAgent = new NeuralNetworkDrivenAgent(x, y, direction, speed);
			OptimizableNeuralNetwork newBrain = NeuralNetworkDrivenAgent.randomNeuralNetworkBrain().mutate();
			newAgent.setBrain(newBrain);
			this.addAgent(newAgent);
			energyReserve -= newAgent.getEnergy();
		}
	}

	private void addAgents() {
		if (getFishes().size() <= ENDANGERED_SPECIES) {
			// Do not allow life go extinct in this simulation
			addNewRandomFish();
		}
		if (getFishes().size() > ENDANGERED_SPECIES) {
			addNewFood();
		}
	}

	private void interactAgents() {
		for (IAgent agent : this.getAgents()) {
			agent.interact(this);
		}
	}

	private void moveAgents() {
		for (IAgent agent : this.getAgents()) {
			if (agent instanceof MovingAgent) {
				((MovingAgent) agent).move(this);
			}
		}
	}

	private void feedAgents() {
		countEatenFood = 0;
		for (FertileAgent fish : getFishes()) {
			double fishRadius = fish.getRadius();
			for (IFood food : getFood()) {
				double deltaY = Math.abs(food.getY() - fish.getY());
				if (deltaY < fishRadius) {
					double deltaX = Math.abs(food.getX() - fish.getX());
					if (deltaX < fishRadius) {
						double distanceToFood = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
						if (distanceToFood < fishRadius) {
							countEatenFood++;
							removeAgent(food);
							fish.feed(food);
							fish.reproduce(this);
							break;
						}
					}
				}
			}
		}
	}

	private void growAgentsOlder() {
		for (FertileAgent agent : getFishes()) {
			int upkeepEnergy = agent.growOlder();
			if (agent.getEnergy() <= 0) {
				this.removeAgent(agent);
			}
			energyReserve += upkeepEnergy;
		}
	}

	public List<IAgent> getAgents() {
		// to avoid concurrent modification exception
		return new LinkedList<IAgent>(this.agents);
	}

	public synchronized void addAgent(IAgent agent) {
		this.agents.add(agent);
	}

	public synchronized void removeAgent(IAgent agent) {
		this.agents.remove(agent);
	}

	public synchronized void timeStep() {
			interactAgents();
			moveAgents();
			feedAgents();
			growAgentsOlder();

			addAgents();

			for (AgentsEnvironmentObserver l : this.listeners) {
				// keeps score for tournaments
				l.notify(this);
			}

			this.time++;
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
