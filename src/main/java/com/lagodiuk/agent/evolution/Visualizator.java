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

import java.awt.Color;
import java.awt.Graphics2D;

import com.lagodiuk.agent.Agent;
import com.lagodiuk.agent.AgentsEnvironment;
import com.lagodiuk.agent.Food;

public class Visualizator {

	public static void paintEnvironment(Graphics2D canvas, AgentsEnvironment environment) {
		canvas.clearRect(0, 0, environment.getWidth(), environment.getHeight());

		canvas.setColor(new Color(255, 255, 0));
		for (Food food : environment.filter(Food.class)) {
			int x = (int) food.getX();
			int y = (int) food.getY();
			int foodRadius = (int)food.getRadius();

			canvas.fillOval(x - foodRadius, y - foodRadius, foodRadius * 2, foodRadius * 2);
		}

		int maxGeneration = environment.getLongestGeneration();

		for (Agent agent : environment.filter(Agent.class)) {
			int x = (int) agent.getX();
			int y = (int) agent.getY();
			int agentRadius = (int)agent.getRadius();

			int red = Math.max(255 - agent.getEnergy() * 25, 0);
			int green = Math.min(agent.getEnergy() * 25, 255);
			canvas.setColor(new Color(red, green, 0));
			canvas.fillOval(x - agentRadius, y - agentRadius, agentRadius * 2, agentRadius * 2);

			int signSpeed = - (int) Math.signum(agent.getSpeed());
			int rx = (int) ((agent.getRx() * (agentRadius + 4) * signSpeed) + x);
			int ry = (int) ((agent.getRy() * (agentRadius + 4) * signSpeed) + y);

			if (agent.getEnergy() <= 1) {
				canvas.setColor(Color.RED);
			} else if (agent.getEnergy() >=9) {
				canvas.setColor(Color.GREEN);
			} else {
				canvas.setColor(Color.WHITE);
			}
			canvas.drawOval(x - agentRadius, y - agentRadius, agentRadius * 2, agentRadius * 2);

			canvas.drawLine(x, y, rx, ry);

			int generation = ((NeuralNetworkDrivenAgent) agent).getGeneration();
			if (generation == maxGeneration) {

				canvas.setColor(Color.MAGENTA);

				{
					// Big triangle flag
					int triX1 = x;
					int triY1 = y - agentRadius - 16;
					int triX2 = x + 10;
					int triY2 = triY1 + 4;
					int triX3 = x;
					int triY3 = triY2 + 4;
					canvas.drawLine(x, y - agentRadius, triX1, triY1);
					canvas.drawLine(triX1, triY1, triX2, triY2);
					canvas.drawLine(triX3, triY3, triX2, triY2);
				}
				{
					// Small triangle flag inside the big one
					int triX1 = x;
					int triY1 = y - agentRadius - 14;
					int triX2 = x + 6;
					int triY2 = triY1 + 2;
					int triX3 = x;
					int triY3 = triY2 + 2;
					canvas.drawLine(x, y - agentRadius, triX1, triY1);
					canvas.drawLine(triX1, triY1, triX2, triY2);
					canvas.drawLine(triX3, triY3, triX2, triY2);
				}
			}
		}
	}
}
