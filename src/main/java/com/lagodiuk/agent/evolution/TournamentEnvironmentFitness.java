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
import com.lagodiuk.agent.IFood;
import com.lagodiuk.agent.StaticFood;
import com.lagodiuk.ga.Fitness;
import com.lagodiuk.nn.genetic.OptimizableNeuralNetwork;

public class TournamentEnvironmentFitness implements Fitness<OptimizableNeuralNetwork, Double> {

	private static Random random = new Random();

	@Override
	public Double calculate(OptimizableNeuralNetwork chromosome) {
		// TODO maybe, its better to initialize these parameters in constructor
		final int width = 200;
		final int height = 200;
		int agentsCount = 10;
		int foodCount = 5;
		int environmentIterations = 50;

		AgentsEnvironment env = new AgentsEnvironment(width, height);

		for (int i = 0; i < agentsCount; i++) {
			int x = random.nextInt(width);
			int y = random.nextInt(height);
			double direction = 2 * Math.PI * random.nextDouble();
			double speed = 0;

			NeuralNetworkDrivenAgent agent = new NeuralNetworkDrivenAgent(x, y, direction, speed);
			agent.setBrain(chromosome.clone());

			env.addAgent(agent);
		}

		for (int i = 0; i < foodCount; i++) {
			IFood food = this.newPieceOfFood(width, height);
			env.addAgent(food);
		}

		EatenFoodObserver tournamentListener = new EatenFoodObserver();
		env.addListener(tournamentListener);

		for (int i = 0; i < environmentIterations; i++) {
			env.timeStep();
		}

		double score = tournamentListener.getScore();
		return 1.0 / score;
	}

	protected IFood newPieceOfFood(int width, int height) {
		IFood food = new StaticFood(random.nextInt(width), random.nextInt(height));
		return food;
	}
}
