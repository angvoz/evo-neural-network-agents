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

public abstract class MovingAgent extends AbstractAgent {
	public static final double MAX_SPEED = 4;

	private double angle;
	private double speed;

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
		if (speed > MAX_SPEED) {
			speed = MAX_SPEED;
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

	private void collide(AgentsEnvironment env, MovingAgent otherAgent) {
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

	private void collide(AgentsEnvironment env) {
		if (!(this instanceof IFood) && getEnergy() > 0) {
			for (MovingAgent otherAgent : env.getFishes()) {
				if (this != otherAgent && !(otherAgent instanceof IFood) && otherAgent.getEnergy() > 0) {
					double futureDistance = module(otherAgent.getX() - (getX() + getRx() * getSpeed()), otherAgent.getY() - (getY() + this.getRy() * getSpeed()));
					if (futureDistance < this.getRadius() + otherAgent.getRadius() + 3) {
						collide(env, otherAgent);
						break;
					}
				}
			}
		}
	}

	protected void move(AgentsEnvironment env) {
		collide(env);

		env.removeAgent(this);

		double rx = getRx();
		double ry = getRy();

		setX(getX() + (rx * speed));
		setY(getY() + (ry * speed));

		env.addAgent(this);
	}
}
