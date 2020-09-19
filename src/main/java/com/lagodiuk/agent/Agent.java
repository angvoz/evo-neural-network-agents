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

import java.util.Random;

public class Agent implements AbstractAgent {

	public static final int STARTING_ENERGY = 6;
	public static final int REPRODUCE_ENERGY_TRIGGER = 10;

	private static final double RADIUS = 5;

	private double x;

	private double y;

	private double angle;

	private double speed;

	private int energy;

	public Agent(double x, double y, double angle) {
		this.x = x;
		this.y = y;
		this.speed = 0;
		this.angle = angle;
		this.energy = STARTING_ENERGY;
	}

	public void move() {
		double rx = -Math.sin(this.angle);
		double ry = Math.cos(this.angle);
		this.x += rx * this.speed;
		this.y += ry * this.speed;
	}

	@Override
	public double getX() {
		return this.x;
	}

	@Override
	public double getY() {
		return this.y;
	}

	@Override
	public void setX(double x) {
		this.x = x;
	}

	@Override
	public void setY(double y) {
		this.y = y;
	}

	@Override
	public double getRadius() {
		return RADIUS;
	}

	public double getSpeed() {
		return this.speed;
	}

	public void setSpeed(double v) {
		this.speed = v;
	}

	public double getAngle() {
		return this.angle;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public double getRx() {
		double rx = -Math.sin(this.angle);
		return rx;
	}

	public double getRy() {
		double ry = Math.cos(this.angle);
		return ry;
	}

	public int getEnergy() {
		return energy;
	}

	public void setEnergy(int energy) {
		this.energy = energy;
	}

	public void eatFood(Food food) {
		this.energy++;
	}

	@Override
	public void interact(AgentsEnvironment env) {
		// Stub
	}

	public Agent reproduce() {
		if (energy >= REPRODUCE_ENERGY_TRIGGER) {
			double newAngle = new Random().nextDouble();
			Agent newAgent = new Agent(this.x, this.y, newAngle);
			energy = REPRODUCE_ENERGY_TRIGGER - newAgent.getEnergy();
			return newAgent;
		}
		return null;
	}
}
