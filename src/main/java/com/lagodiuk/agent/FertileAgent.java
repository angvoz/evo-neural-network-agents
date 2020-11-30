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

abstract public class FertileAgent extends MovingAgent {
	protected static final int PARENTING_ENERGY_DEFAULT = 10;
	public static final int NEWBORN_ENERGY_DEFAULT = 6;

	private double age;

	public FertileAgent(double x, double y, double angle, double speed) {
		super(x, y, angle, speed);

		this.age = 0;
		setEnergy(NEWBORN_ENERGY_DEFAULT);
	}

	private int dissipateEnergy() {
		final int radiateEnergy = 1;
		int energy = getEnergy();
		if (energy > 0) {
			if (age % 100 == 0) {
				setEnergy(energy - radiateEnergy);
			}
		}
		return energy - getEnergy();
	}

	public int grow() {
		age++;
		return dissipateEnergy();
	}

	public void feed(IFood food) {
		setEnergy(getEnergy() + food.getEnergy());
	}

	abstract public FertileAgent reproduce(AgentsEnvironment env);
}
