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
	private static void setFlagOnFood(Graphics2D canvas, Food food, Color color) {
		if (food != null) {
			canvas.setColor(color);
			// Small triangle flag
			int x0 = (int) food.getX();
			int y0 = (int) food.getY() - (int)food.getRadius()*2 - 1;
			int x1 = x0;
			int y1 = y0 - 8;
			int x2 = x0 + 6;
			int y2 = y0 - 6;
			int x3 = x0;
			int y3 = y0 - 4;
			canvas.drawLine(x0, y0, x1, y1);
			canvas.drawLine(x1, y1, x2, y2);
			canvas.drawLine(x3, y3, x2, y2);
		}
	}

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

			double angle = agent.getAngle();
			double theta = 0.3;
			int radiusEyeBase = agentRadius + -1;
			int radiusEye = 2;

			canvas.setColor(Color.WHITE);
			{
				double rxEye1 = -Math.sin(angle + theta);
				int xEye1 = (int) (rxEye1*radiusEyeBase + x - (double)radiusEye/2);
				double ryEye1 = Math.cos(angle + theta);
				int yEye1 = (int) (ryEye1*radiusEyeBase + y - (double)radiusEye/2);
				canvas.drawOval(xEye1, yEye1, radiusEye, radiusEye);
			}
			{
				double rxEye1 = -Math.sin(angle - theta);
				int xEye1 = (int) (rxEye1*radiusEyeBase + x - (double)radiusEye/2);
				double ryEye1 = Math.cos(angle - theta);
				int yEye1 = (int) (ryEye1*radiusEyeBase + y - (double)radiusEye/2);
				canvas.drawOval(xEye1, yEye1, radiusEye, radiusEye);
			}

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

				// Find nearest food
				Food nearestFood_1 = null;
				Food nearestFood_2 = null;
				double nearestFoodDist = Double.MAX_VALUE;

				for (Food currFood : environment.filter(Food.class)) {
					// agent can see only ahead
					if (((NeuralNetworkDrivenAgent) agent).inSight(currFood)) {
						double currFoodDist = ((NeuralNetworkDrivenAgent) agent).distanceTo(currFood);
						if ((nearestFood_1 == null) || (currFoodDist <= nearestFoodDist)) {
							nearestFood_2 = nearestFood_1;
							nearestFood_1 = currFood;
							nearestFoodDist = currFoodDist;
						}
					}
				}
				setFlagOnFood(canvas, nearestFood_1, Color.MAGENTA);
				setFlagOnFood(canvas, nearestFood_2, Color.MAGENTA);
			}
		}
	}
}
