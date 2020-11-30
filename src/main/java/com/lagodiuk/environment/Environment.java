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
package com.lagodiuk.environment;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import com.lagodiuk.agent.AbstractAgent;
import com.lagodiuk.agent.FertileAgent;
import com.lagodiuk.agent.IAgent;
import com.lagodiuk.agent.IFood;
import com.lagodiuk.agent.MovingAgent;
import com.lagodiuk.agent.MovingFood;
import com.lagodiuk.agent.StaticFood;
import com.lagodiuk.nn.NeuralNetwork;
import com.lagodiuk.nn.NeuralNetworkDrivenAgent;
import com.lagodiuk.nn.genetic.OptimizableNeuralNetwork;

@XmlSeeAlso({ StaticFood.class, MovingFood.class, OptimizableNeuralNetwork.class, NeuralNetworkDrivenAgent.class })

@XmlRootElement(name="environment")
public class Environment implements IEnvironment {
	public static final boolean FOOD_CELL_DIVISION = true;
	private static final boolean FOOD_STATIC = false;

	@XmlElement
	private int width;
	@XmlElement
	private int height;
	@XmlElement
	private double time;
	@XmlElement
	private int energyReserve;
	@XmlElement
	private int minNumberOfAgents = 10;
	@XmlElement
	private long countMutation;

	@XmlElementWrapper(name = "agents")
	@XmlElement(name = "agent")
	private List<AbstractAgent> agents = new ArrayList<AbstractAgent>();

	@XmlTransient
	private ArrayList<AbstractAgent> seedAgents = new ArrayList<AbstractAgent>();

	@XmlTransient
	private List<IFood> foodAgents = null;

	@XmlTransient
	private Random random = new Random();

	@SuppressWarnings("unused")
	private Environment() {
	}

	public Environment(int width, int height) {
		this.width = width;
		this.height = height;
		this.time = 0;
		this.energyReserve = 0;
	}

	@Override
	public int getWidth() {
		return this.width;
	}

	@Override
	public int getHeight() {
		return this.height;
	}

	@Override
	public double getTime() {
		return this.time;
	}

	@Override
	public List<FertileAgent> getFishes() {
		List<FertileAgent> filtered = new ArrayList<FertileAgent>();
		for (AbstractAgent agent : this.agents) {
			if (agent instanceof FertileAgent && agent.isAlive()) {
				filtered.add((FertileAgent) agent);
			}
		}
		return filtered;
	}

	@Override
	public List<IFood> getFood() {
		return foodAgents;
	}

	private void initializeFish(int agentsCount) {
		for (int i = 0; i < agentsCount; i++) {
			int x = random.nextInt(width);
			int y = random.nextInt(height);
			double direction = random.nextDouble() * 2 * Math.PI;
			double speed = random.nextDouble() * MovingAgent.MAX_SPEED;

			NeuralNetworkDrivenAgent agent = new NeuralNetworkDrivenAgent(x, y, direction, speed);
			NeuralNetwork brain = NeuralNetworkDrivenAgent.randomNeuralNetworkBrain();
			agent.setBrain(brain);

			addAgent(agent);
		}
	}

	private void initializeFood(int foodCount) {
		for (int i = 0; i < foodCount; i++) {
			addNewRandomFood();
		}
	}

	@Override
	public void initialize(int agentsDensity, int foodDensity) {
		int area = width * height;
		// Density per million pixels
		int agentsCount = area * agentsDensity / 1000000;
		int foodCount = area * foodDensity / 1000000;

		initializeFish(agentsCount);
		initializeFood(foodCount);

		evaluate();
	}

	private IFood createRandomFood(int x, int y) {
		IFood food = null;
		if (FOOD_STATIC) {
			food = new StaticFood(x, y);
		} else {
			double speed = random.nextDouble() * MovingFood.MAX_SPEED;
			double direction = random.nextDouble() * 2 * Math.PI;

			food = new MovingFood(x, y, direction, speed);
		}
		return food;
	}

	public void seedFood(int x, int y) {
		IFood food = createRandomFood(x, y);
		this.seedAgents.add((AbstractAgent) food);
	}

	private IFood addNewRandomFood() {
		int x = random.nextInt(width);
		int y = random.nextInt(height);
		IFood newFood = createRandomFood(x, y);
		addAgent((AbstractAgent) newFood);
		return newFood;
	}

	private void addNewFood() {
		ArrayList<IFood> foodPool = new ArrayList<IFood>(getFood());
		while (energyReserve > 0) {
			if (FOOD_CELL_DIVISION && foodPool.size() > 0) {
				int index = random.nextInt(foodPool.size());
				IFood food = foodPool.get(index);
				IFood newFood = food.reproduce(this);
				foodPool.remove(food);
				energyReserve -= newFood.getEnergy();
			} else {
				IFood newFood = addNewRandomFood();
				energyReserve -= newFood.getEnergy();
			}
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

	private void removeDeadAgents() {
		agents.removeIf(agent -> !agent.isAlive());
	}

	private void addNewbornAgents() {
		ArrayList<AbstractAgent> unbornAgents = new ArrayList<AbstractAgent>();
		for (AbstractAgent agent : seedAgents) {
			if (energyReserve >= agent.getEnergy()) {
				addAgent(agent);
				energyReserve = energyReserve - agent.getEnergy();
			} else {
				unbornAgents.add(agent);
			}
		}
		seedAgents = unbornAgents;

		if (getFishes().size() <= minNumberOfAgents) {
			// Do not allow life go extinct in this simulation
			addNewRandomFish();
		}
		if (getFishes().size() > minNumberOfAgents) {
			addNewFood();
		}
	}

	@Override
	public List<AbstractAgent> getAgents() {
		// to avoid concurrent modification exception
		return new ArrayList<AbstractAgent>(this.agents);
	}

	@Override
	public synchronized void addAgent(AbstractAgent agent) {
		double x = agent.getX();
		double y = agent.getY();
		if (x < 0) {
			x = x + width;
		}
		if (y < 0) {
			y = y + height;
		}
		if (x >= width) {
			x = x - width;
		}
		if (y >= height) {
			y = y - height;
		}

		boolean expected = (x >= 0) && (x < width) && (y >= 0) && (y < height);
		if (!expected) {
			throw new RuntimeException("addAgent: agent in inconsistent state");
		}

		agent.setX(x);
		agent.setY(y);
		this.agents.add(agent);
	}

	@Override
	public synchronized void removeAgent(AbstractAgent agent) {
		this.agents.remove(agent);
	}

	private void evaluate() {
		foodAgents = new ArrayList<IFood>();
		for (AbstractAgent agent : agents) {
			if (agent instanceof IFood && agent.isAlive()) {
				foodAgents.add((IFood) agent);
			}
		}
	}

	@Override
	public synchronized void timeStep() {
		List<AbstractAgent> agents = new ArrayList<AbstractAgent>(this.agents);
		Collections.shuffle(agents);
		for (AbstractAgent agent : agents) {
			if (agent.isAlive()) {
				agent.interact(this);
			}
		}

		removeDeadAgents();
		addNewbornAgents();

		for (AbstractAgent agent : this.agents) {
			agent.evaluate(this);
		}

		evaluate();

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

	@XmlTransient
	public int getEnergyReserve() {
		return energyReserve;
	}

	public void setEnergyReserve(int energy) {
		energyReserve = energy;
	}

	@Override
	public void addEnergyReserve(int energy) {
		energyReserve += energy;
	}

	public int countEnergy() {
		int countEnergy = energyReserve;
		for (IAgent agent : agents) {
			countEnergy += agent.getEnergy();
		}
		return countEnergy;
	}

	public void setMinNumberOfAgents(int minNumberOfAgents) {
		this.minNumberOfAgents = minNumberOfAgents;
	}

	public static void marshall(Environment env, OutputStream out) throws Exception {
		env.countMutation = NeuralNetworkDrivenAgent.getMutationCount();
		JAXBContext context = JAXBContext.newInstance(Environment.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.marshal(env, out);
		out.flush();
	}

	public static Environment unmarshall(InputStream in) throws Exception {
		JAXBContext context = JAXBContext.newInstance(Environment.class);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		Environment env = (Environment) unmarshaller.unmarshal(in);
		NeuralNetworkDrivenAgent.setMutationCount(env.countMutation);
		return env;
	}
}
