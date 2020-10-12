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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Random;

import com.lagodiuk.agent.AgentsEnvironment;
import com.lagodiuk.agent.FertileAgent;
import com.lagodiuk.agent.IFood;
import com.lagodiuk.agent.MovingFood;
import com.lagodiuk.agent.StaticFood;
import com.lagodiuk.nn.NeuralNetwork;
import com.lagodiuk.nn.NeuralNetworkDrivenAgent;

public class Runner {
	private static final int MAX_ITERATIONS = 1000000;

	private static AgentsEnvironment environment;
	private static volatile boolean staticFood = false;
	private static String filename = null;
	private static boolean justStarted = true;

	private static Random random = new Random();

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

		if (args.length < 1) {
			System.out.println("Usage: Provide file.xml as an argument");
			return;
		}

		filename = args[0];
		if (new File(filename).getParent() == null) {
			filename = System.getProperty("user.dir") + File.separator + filename;
		}

		if (new File(filename).exists()) {
			loadWorld(filename);
			justStarted = false;
		} else {
			justStarted = true;
		}

		mainEnvironmentLoop();
	}

	private static void mainEnvironmentLoop() throws Exception {
		for (;;) {
			environment.timeStep();
			int time = (int) environment.getTime();
			if (time % 1000 == 0) {
				if (!justStarted && !new File(filename).exists()) {
					System.out.println("Info: Time=" + time + ", " + filename + " was moved, exiting");
					break;
				}

				saveWorld(filename);

				if (!justStarted && time % MAX_ITERATIONS == 0) {
					System.out.println("Info: Time=" + time + " exceeded limit, exiting");
					break;
				}
				justStarted = false;
			}
		}
	}

	private static void initializeEnvironment(int environmentWidth, int environmentHeight, int agentsCount, int foodCount, int minNumberOfAgents) {
		environment = new AgentsEnvironment(environmentWidth, environmentHeight);
		initializeAgents(agentsCount, minNumberOfAgents);
		initializeFood(foodCount);
	}

	private static IFood createRandomFood(int width, int height) {
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

	private static void loadWorld(String filename) throws Exception {
		File file = new File(filename);
		FileInputStream in = new FileInputStream(file);
		environment = AgentsEnvironment.unmarshall(in);
		in.close();
	}

	private static void saveWorld(String filename) throws Exception {
		File file = new File(filename);
		FileOutputStream out = new FileOutputStream(file);
		AgentsEnvironment.marshall(environment, out);
		out.close();
	}

	private static void initializeAgents(int agentsCount, int minNumberOfAgents) {
		int environmentWidth = environment.getWidth();
		int environmentHeight = environment.getHeight();
		environment.setMinNumberOfAgents(minNumberOfAgents);

		for (int i = 0; i < agentsCount; i++) {
			int x = random.nextInt(environmentWidth);
			int y = random.nextInt(environmentHeight);
			double direction = random.nextDouble() * 2*Math.PI;
			double speed = random.nextDouble() * NeuralNetworkDrivenAgent.MAX_SPEED;

			NeuralNetworkDrivenAgent agent = new NeuralNetworkDrivenAgent(x, y, direction, speed);
			NeuralNetwork brain = NeuralNetworkDrivenAgent.randomNeuralNetworkBrain();
			agent.setBrain(brain);

			environment.addAgent(agent);
		}
	}

	private static void initializeFood(int foodCount) {
		int environmentWidth = environment.getWidth();
		int environmentHeight = environment.getHeight();

		for (int i = 0; i < foodCount; i++) {
			IFood food = createRandomFood(environmentWidth, environmentHeight);
			environment.addFood(food);
		}
	}

}
