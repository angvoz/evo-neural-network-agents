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
	private double angle;
	private double speed;

	public MovingAgent(double x, double y, double angle, double speed) {
		super(x, y);
		this.speed = speed;
		this.angle = angle;
	}

	public double getAngle() {
		return this.angle;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}

	public double getSpeed() {
		return this.speed;
	}

	public void setSpeed(double v) {
		this.speed = v;
	}

	public double getRx() {
		double rx = -Math.sin(this.angle);
		return rx;
	}

	public double getRy() {
		double ry = Math.cos(this.angle);
		return ry;
	}

	public void move(AgentsEnvironment env) {
		double rx = -Math.sin(this.angle);
		double ry = Math.cos(this.angle);
		this.setX(this.getX() + (rx * this.speed));
		this.setY(this.getY() + (ry * this.speed));


		double newX = this.getX();
		double newY = this.getY();
		if (newX < 0) {
			newX = env.getWidth() - 1;
		}
		if (newY < 0) {
			newY = env.getHeight() - 1;
		}
		if (newX > env.getWidth()) {
			newX = 1;
		}
		if (newY > env.getHeight()) {
			newY = 1;
		}

		this.setX(newX);
		this.setY(newY);

	}
}
