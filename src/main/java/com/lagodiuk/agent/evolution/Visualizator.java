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
import java.util.List;

import com.lagodiuk.agent.AgentsEnvironment;
import com.lagodiuk.agent.FertileAgent;
import com.lagodiuk.agent.IFood;
import com.lagodiuk.agent.MovingAgent;
import com.lagodiuk.nn.NeuralNetworkDrivenAgent;

public class Visualizator {
	private AgentsEnvironment environment;

	public Visualizator(AgentsEnvironment environment) {
		this.environment = environment;
	}

	private Color getColorFood() {
		Color colorFood = new Color(255, 255, 0);
		return colorFood;
	}

	private Color getColorBody(FertileAgent agent) {
		Color colorBody = Color.GRAY;
		if (agent.getEnergy() <= 0) {
			colorBody = Color.BLACK;
		} else {
			int red = Math.max(255 - agent.getEnergy() * 25, 0);
			int green = Math.min(agent.getEnergy() * 25, 255);
			colorBody = new Color(red, green, 0);
		}
		return colorBody;
	}

	private Color colorBodyOutline(FertileAgent agent) {
		Color colorBodyOutline = Color.GRAY;
		if (agent.getEnergy() <= 0) {
			colorBodyOutline = Color.WHITE;
		} else {
			int agentEnergy = agent.getEnergy();
			if (agentEnergy <= 1) {
				colorBodyOutline = Color.RED;
			} else if (agentEnergy >= 9) {
				colorBodyOutline = Color.GREEN;
			} else {
				colorBodyOutline = Color.WHITE;
			}
		}
		return colorBodyOutline;
	}

	private Color getColorFlag(FertileAgent agent) {
		Color colorFlag = null;
		if (agent instanceof NeuralNetworkDrivenAgent) {
			final int maxGeneration = environment.getLongestGeneration();
			int generation = ((NeuralNetworkDrivenAgent) agent).getGeneration();
			if (generation == maxGeneration) {
				colorFlag = Color.MAGENTA;
			}
		}
		return colorFlag;
	}

	private void markFood(Graphics2D canvas, IFood food, Color color) {
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

	private void drawFood(Graphics2D canvas) {
		Color colorFood = getColorFood();
		canvas.setColor(colorFood);
		for (IFood food : environment.getFood()) {
			int x = (int) food.getX();
			int y = (int) food.getY();
			int foodRadius = (int) food.getRadius();

			canvas.fillOval(x - foodRadius, y - foodRadius, foodRadius * 2, foodRadius * 2);
		}
	}

	private void markAgent(Graphics2D canvas, MovingAgent agent, int x, int y, Color color) {
		int agentRadius = (int) agent.getRadius();
		canvas.setColor(color);
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

	private void drawAgentBody(Graphics2D canvas, FertileAgent agent, int x, int y, Color colorBody, Color colorBodyOutline) {
		int agentRadius = (int) agent.getRadius();
		canvas.setColor(colorBody);
		canvas.fillOval(x - agentRadius, y - agentRadius, agentRadius * 2, agentRadius * 2);
		canvas.setColor(colorBodyOutline);
		canvas.drawOval(x - agentRadius, y - agentRadius, agentRadius * 2, agentRadius * 2);
	}

	private void drawAgentEye(Graphics2D canvas, MovingAgent agent, double theta) {
		int radiusEyeBase = (int) agent.getRadius() - 1;
		int radiusEye = 1;
		int diameterEye = radiusEye * 2;
		double angleEye = agent.getAngle() + theta;
		double rx = Math.cos(angleEye);
		int x = (int) (rx * radiusEyeBase + agent.getX() - radiusEye);
		double ry = Math.sin(angleEye);
		int y = (int) (ry * radiusEyeBase + agent.getY() - radiusEye);

		canvas.drawOval(x, y, diameterEye, diameterEye);
	}

	private void drawAgentTail(Graphics2D canvas, FertileAgent agent, int x, int y) {
		int signSpeed = -(int) Math.signum(agent.getSpeed());
		double agentRadius = agent.getRadius();
		int rx = (int) ((agent.getRx() * (agentRadius + 4) * signSpeed) + x);
		int ry = (int) ((agent.getRy() * (agentRadius + 4) * signSpeed) + y);

		canvas.drawLine(x, y, rx, ry);
	}

	private void replicateAgentAt(Graphics2D canvas, FertileAgent agent, int x, int y, Color colorBody, Color colorBodyOutline, Color colorFlag) {
		drawAgentBody(canvas, agent, x, y, colorBody, colorBodyOutline);
		canvas.setColor(colorBodyOutline);
		drawAgentEye(canvas, agent, 0.3);
		drawAgentEye(canvas, agent, -0.3);
		drawAgentTail(canvas, agent, x, y);

		if (colorFlag != null) {
			markAgent(canvas, agent, x, y, colorFlag);
		}
	}

	private void drawAgent(Graphics2D canvas, FertileAgent agent) {
		Color colorBody = getColorBody(agent);
		Color colorBodyOutline = colorBodyOutline(agent);
		Color colorFlag = getColorFlag(agent);

		int x = (int) agent.getX();
		int y = (int) agent.getY();

		int envWidth = environment.getWidth();
		int envHeight = environment.getHeight();
		int distanceFromBorder = (int) NeuralNetworkDrivenAgent.EYESIGHT_DISTANCE;

		replicateAgentAt(canvas, agent, x, y, colorBody, colorBodyOutline, colorFlag);

		// Replicate agent on the other sides of the boundaries
		if (x - distanceFromBorder < 0) {
			replicateAgentAt(canvas, agent, x + envWidth, y, colorBody, colorBodyOutline, colorFlag);
		}
		if (x + distanceFromBorder > envWidth) {
			replicateAgentAt(canvas, agent, x - envWidth, y, colorBody, colorBodyOutline, colorFlag);
		}
		if (y - distanceFromBorder < 0) {
			replicateAgentAt(canvas, agent, x, y + envHeight, colorBody, colorBodyOutline, colorFlag);
		}
		if (y + distanceFromBorder > envHeight) {
			replicateAgentAt(canvas, agent, x, y - envHeight, colorBody, colorBodyOutline, colorFlag);
		}
		if (x - distanceFromBorder < 0 && y - distanceFromBorder < 0) {
			replicateAgentAt(canvas, agent, x + envWidth, y + envHeight, colorBody, colorBodyOutline, colorFlag);
		}

		if (x + distanceFromBorder > envWidth && y - distanceFromBorder < 0) {
			replicateAgentAt(canvas, agent, x - envWidth, y + envHeight, colorBody, colorBodyOutline, colorFlag);
		}
		if (x - distanceFromBorder < 0 && y + distanceFromBorder > envHeight) {
			replicateAgentAt(canvas, agent, x + envWidth, y - envHeight, colorBody, colorBodyOutline, colorFlag);
		}
		if (x + distanceFromBorder > envWidth && y + distanceFromBorder > envHeight) {
			replicateAgentAt(canvas, agent, x - envWidth, y - envHeight, colorBody, colorBodyOutline, colorFlag);
		}

		if (colorFlag != null) {
			if (agent instanceof NeuralNetworkDrivenAgent) {
				for (IFood food : environment.getFood()) {
					if (((NeuralNetworkDrivenAgent) agent).inSight(food)) {
						markFood(canvas, food, colorFlag);
					}
				}
			}
		}
	}

	private void drawAgents(Graphics2D canvas) {
		List<FertileAgent> agents = environment.getFishes();
		for (FertileAgent agent : agents) {
			drawAgent(canvas, agent);
		}
	}

	public void paintEnvironment(Graphics2D canvas) {
		canvas.clearRect(0, 0, environment.getWidth(), environment.getHeight());

		drawAgents(canvas);
		drawFood(canvas);
	}
}
