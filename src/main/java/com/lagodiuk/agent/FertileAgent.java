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
	public static final int STARTING_ENERGY = 6;
	protected static final int PREGNANCY_ENERGY = 10;

	private double age;
	private int energy;

	public FertileAgent(double x, double y, double angle, double speed) {
		super(x, y, angle, speed);

		this.age = 0;
		this.energy = STARTING_ENERGY;
	}

	@Override
	public int getEnergy() {
		return energy;
	}

	public void setEnergy(int energy) {
		this.energy = energy;
	}

	public int growOlder() {
		int oldEnergy = energy;
		this.age++;
		if (age % 100 == 0) {
			this.energy = energy - 1;
		}
		return oldEnergy - energy;
	}

	public void feed(IFood food) {
		this.energy += food.getEnergy();
	}

	abstract public void reproduce(AgentsEnvironment env);
}
