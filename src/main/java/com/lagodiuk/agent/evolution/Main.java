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

import java.util.Random;

import com.lagodiuk.agent.AgentsEnvironment;
import com.lagodiuk.agent.FertileAgent;
import com.lagodiuk.agent.MovingAgent;
import com.lagodiuk.nn.NeuralNetwork;
import com.lagodiuk.nn.NeuralNetworkDrivenAgent;

public class Main {
	private static Random random = new Random();
	private static Visualizator visualizator;

	public static void main(String[] args) throws Exception {
		// TODO maybe, add ability to define these parameters as environment
		// constants
		int environmentWidth = 1470;
		int environmentHeight = 850;
		int agentsCount = 50;
		int minNumberOfAgents = 10;
		// Density per million pixels
		int foodDensity = 800;
		int totalEnergy = environmentWidth * environmentHeight * foodDensity / 1000000;
		int foodCount = totalEnergy - agentsCount * FertileAgent.NEWBORN_ENERGY_DEFAULT;

		initializeEnvironment(environmentWidth, environmentHeight, agentsCount, foodCount, minNumberOfAgents);

		visualizator.initialize(environmentWidth, environmentHeight);

		mainEnvironmentLoop();
	}

	private static void mainEnvironmentLoop() throws InterruptedException {
		for (;;) {
			visualizator.timeStep();
		}
	}

	private static void initializeEnvironment(int environmentWidth, int environmentHeight, int agentsCount, int foodCount, int minNumberOfAgents) {
		AgentsEnvironment environment = new AgentsEnvironment(environmentWidth, environmentHeight);
		visualizator = new Visualizator(environment);

		initializeAgents(agentsCount, minNumberOfAgents);
		initializeFood(foodCount);
	}

	private static void initializeAgents(int agentsCount, int minNumberOfAgents) {
		AgentsEnvironment environment = visualizator.getEnvironment();
		int environmentWidth = environment.getWidth();
		int environmentHeight = environment.getHeight();
		environment.setMinNumberOfAgents(minNumberOfAgents);

		for (int i = 0; i < agentsCount; i++) {
			int x = random.nextInt(environmentWidth);
			int y = random.nextInt(environmentHeight);
			double direction = random.nextDouble() * 2*Math.PI;
			double speed = random.nextDouble() * MovingAgent.MAX_SPEED;

			NeuralNetworkDrivenAgent agent = new NeuralNetworkDrivenAgent(x, y, direction, speed);
			NeuralNetwork brain = NeuralNetworkDrivenAgent.randomNeuralNetworkBrain();
			agent.setBrain(brain);

			environment.addAgent(agent);
		}
	}

	private static void initializeFood(int foodCount) {
		AgentsEnvironment environment = visualizator.getEnvironment();
		for (int i = 0; i < foodCount; i++) {
			environment.addNewRandomFood();
		}
	}

}
