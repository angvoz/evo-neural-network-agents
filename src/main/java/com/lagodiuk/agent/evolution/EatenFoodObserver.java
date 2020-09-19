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

import com.lagodiuk.agent.AgentsEnvironment;
import com.lagodiuk.agent.AgentsEnvironmentObserver;
import com.lagodiuk.agent.FertileAgent;

/**
 * Calculating eaten pieces of food
 */
public class EatenFoodObserver implements AgentsEnvironmentObserver {
	protected static final double maxFishesDistance = 5;

	private double score = 0;

	@Override
	public void notify(AgentsEnvironment env) {
		this.score += env.countEatenFood();

		List<FertileAgent> collidedFishes = this.getCollidedFishes(env);
		this.score -= collidedFishes.size() * 0.5;
	}

	private List<FertileAgent> getCollidedFishes(AgentsEnvironment env) {
		List<FertileAgent> collidedFishes = new LinkedList<FertileAgent>();

		List<FertileAgent> allFishes = env.getFishes();
		int fishesCount = allFishes.size();

		for (int i = 0; i < (fishesCount - 1); i++) {
			FertileAgent firstFish = allFishes.get(i);
			for (int j = i + 1; j < fishesCount; j++) {
				FertileAgent secondFish = allFishes.get(j);
				double distanceToSecondFish = this.module(firstFish.getX() - secondFish.getX(), firstFish.getY() - secondFish.getY());
				if (distanceToSecondFish < maxFishesDistance) {
					collidedFishes.add(secondFish);
				}
			}
		}
		return collidedFishes;
	}

	public double getScore() {
		if (this.score < 0) {
			return 0;
		}
		return this.score;
	}

	protected double module(double vx1, double vy1) {
		return Math.sqrt((vx1 * vx1) + (vy1 * vy1));
	}
}
