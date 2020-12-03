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

import java.util.Comparator;

import javax.xml.bind.annotation.XmlTransient;

import com.lagodiuk.environment.IEnvironment;

public abstract class MovingAgent extends AbstractAgent {
	private static final double NORMAL_RADIUS = 5;
	private static final double NORMAL_SPEED = 4;
	private static final double MIN_RADIUS = 1;
	public static final double MAX_SPEED = NORMAL_SPEED * NORMAL_RADIUS / MIN_RADIUS;

	private double angle;
	private double speed;

	@XmlTransient
	protected SortByDistance sorterByDistance;

	private class SortByDistance implements Comparator<AbstractAgent> {
		private IEnvironment environment;

		public SortByDistance(IEnvironment env) {
			environment = env;
		}

		@Override
		public int compare(AbstractAgent a, AbstractAgent b) {
			return (int) Math.signum(environment.squareOfDistance(MovingAgent.this, a) - environment.squareOfDistance(MovingAgent.this, b));
		}
	}

	protected MovingAgent() {
	}

	public MovingAgent(double x, double y, double angle, double speed) {
		super(x, y);
		this.speed = speed;
		this.angle = angle;
	}

	public double getAngle() {
		return angle;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public double getSpeed() {
		return speed;
	}

	private double underSpeedLimit(double speed) {
		double maxSpeed = MAX_SPEED * MIN_RADIUS / getRadius();
		if (speed > maxSpeed) {
			speed = maxSpeed;
		} else if (speed < 0) {
			speed = 0;
		}
		return speed;
	}

	public void setSpeed(double v) {
		this.speed = underSpeedLimit(v);
	}

	public double getRx() {
		double rx = Math.cos(angle);
		return rx;
	}

	public double getRy() {
		double ry = Math.sin(angle);
		return ry;
	}

	protected double module(double vx1, double vy1) {
		return Math.sqrt((vx1 * vx1) + (vy1 * vy1));
	}

	private void collide(IEnvironment env, MovingAgent otherAgent) {
		// Check if the agent moves towards the other one (angle less than 90 degree)
		double ux = otherAgent.getX() - this.getX();
		double uy = otherAgent.getY() - this.getY();
		double vx = this.getRx();
		double vy = this.getRy();
		double d = ux * vx + uy * vy;
		if (d > 0) {
			// if less than 90 degree move the agent in opposite direction
			double newAngle = Math.atan2(uy, ux) + Math.PI / 2;
			this.setAngle(newAngle);
			// if the other agent not moving move it
			if (otherAgent.getSpeed() <= 0) {
				otherAgent.setAngle(newAngle + Math.PI);
				otherAgent.setSpeed(Double.MAX_VALUE);
				otherAgent.move(env);
			}
			if (this.getSpeed() <= 0) {
				this.setSpeed(Double.MAX_VALUE);
			}
		}
	}

	private void collide(IEnvironment env) {
		if (!(this instanceof IFood) && isAlive()) {
			for (MovingAgent otherAgent : env.getFishes()) {
				if (this != otherAgent && !(otherAgent instanceof IFood) && otherAgent.isAlive()) {
					double futureDistance = module(otherAgent.getX() - (getX() + getRx() * getSpeed()), otherAgent.getY() - (getY() + this.getRy() * getSpeed()));
					if (futureDistance < this.getRadius() + otherAgent.getRadius() + 3) {
						collide(env, otherAgent);
						break;
					}
				}
			}
		}
	}

	public void move(IEnvironment env) {
		collide(env);

		env.removeAgent(this);

		double rx = getRx();
		double ry = getRy();

		setX(getX() + (rx * speed));
		setY(getY() + (ry * speed));

		env.addAgent(this);
	}

	@Override
	public synchronized void evaluate(IEnvironment env) {
		super.evaluate(env);

		if (sorterByDistance == null) {
			sorterByDistance = new SortByDistance(env);
		}
	}

	@Override
	public void interact(IEnvironment env) {
		super.interact(env);

		move(env);
	}
}
