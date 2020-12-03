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

import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.lagodiuk.environment.IEnvironment;

abstract public class FertileAgent extends MovingAgent {
	private static final int PARENT_POSTBIRTH_ENERGY_DEFAULT = 10;
	public static final int NEWBORN_ENERGY_DEFAULT = 6;

	@XmlElement
	private double age;

	private int parentPostBirthEnergy = PARENT_POSTBIRTH_ENERGY_DEFAULT;
	private int newbornEnergy = NEWBORN_ENERGY_DEFAULT;

	@XmlTransient
	private SortedSet<AbstractAgent> foodInReach = null;

	protected FertileAgent() {
	}

	public FertileAgent(double x, double y, double angle, double speed) {
		super(x, y, angle, speed);

		this.age = 0;
		setEnergy(NEWBORN_ENERGY_DEFAULT);
	}

	private void feed(IEnvironment env) {
		for (AbstractAgent food : foodInReach) {
			if (food.isAlive()) {
				feed((IFood) food);
				break;
			}
		}
	}

	@Override
	public synchronized void evaluate(IEnvironment env) {
		super.evaluate(env);

		final double fishRadius = getRadius();
		foodInReach = new TreeSet<AbstractAgent>(sorterByDistance);
		for (AbstractAgent agent : env.getAgents()) {
			if (agent.isAlive() && agent instanceof IFood) {
				double distanceSquare = env.squareOfDistance(this, agent);
				if (distanceSquare < fishRadius * fishRadius) {
					foodInReach.add(agent);
				}
			}
		}
	}

	@Override
	public synchronized void interact(IEnvironment env) {
		super.interact(env);

		feed(env);
		grow(env);
		if (isFertile() && getEnergy() > newbornEnergy + parentPostBirthEnergy) {
			reproduce(env);
		}
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

	public int getParentingEnergy() {
		return parentPostBirthEnergy;
	}

	public void setParentingEnergy(int energy) {
		this.parentPostBirthEnergy = energy;
	}

	public int getNewbornEnergy() {
		return newbornEnergy;
	}

	public void setNewbornEnergy(int energy) {
		this.newbornEnergy = energy;
	}

	public boolean isFertile() {
		boolean result = newbornEnergy > 0 && parentPostBirthEnergy > 0;
		return result;
	}

	abstract public FertileAgent reproduce(IEnvironment env);
}
