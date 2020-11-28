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
import javax.xml.bind.annotation.XmlTransient;

abstract public class AbstractAgent implements IAgent {
	private double x;
	private double y;

	@XmlElement
	private int energy;

	protected AbstractAgent() {
	}

	public AbstractAgent(double x, double y) {
		this.x = x;
		this.y = y;
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
	@XmlTransient
	public int getEnergy() {
		return energy;
	}

	public void setEnergy(int newEnergy) {
		if (newEnergy < 0) {
			throw new RuntimeException("Attempt to set negative energy=" + newEnergy);
		}
		energy = newEnergy;
	}

	@Override
	public void interact(AgentsEnvironment env) {
	}
}
