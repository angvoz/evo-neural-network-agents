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

import javax.xml.bind.annotation.XmlElement;

import com.lagodiuk.environment.IEnvironment;

abstract public class FertileAgent extends MovingAgent {
	protected static final int PARENTING_ENERGY_DEFAULT = 10;
	public static final int NEWBORN_ENERGY_DEFAULT = 6;

	@XmlElement
	private double age;

	protected FertileAgent() {
	}

	public FertileAgent(double x, double y, double angle, double speed) {
		super(x, y, angle, speed);

		this.age = 0;
		setEnergy(NEWBORN_ENERGY_DEFAULT);
	}

	private void feed(IEnvironment env) {
		double fishRadius = getRadius();
		for (IFood food : env.getFood()) {
			double deltaY = Math.abs(food.getY() - getY());
			if (deltaY < fishRadius) {
				double deltaX = Math.abs(food.getX() - getX());
				if (deltaX < fishRadius) {
					double distanceToFood = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
					if (distanceToFood < fishRadius) {
						feed(food);
						break;
					}
				}
			}
		}
	}

	@Override
	public synchronized void interact(IEnvironment env) {
		super.interact(env);

		feed(env);
		reproduce(env);
		grow(env);
	}

	private void dissipateEnergy(IEnvironment env) {
		final int radiateEnergy = 1;
		if (isAlive()) {
			double radius = getRadius();
			if (radius > 0 && (age % (int) (500.0 / radius) == 0)) {
				int energy = getEnergy();
				setEnergy(energy - radiateEnergy);
				env.addEnergyReserve(radiateEnergy);
			}
		}
	}

	public void grow(IEnvironment env) {
		age++;
		dissipateEnergy(env);
	}

	public void feed(IFood food) {
		setEnergy(getEnergy() + food.getEnergy());
		food.setEnergy(0);
	}

	abstract public FertileAgent reproduce(IEnvironment env);
}
