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

import com.lagodiuk.environment.IEnvironment;

public class MovingFood extends MovingAgent implements IFood {
	private static int ENERGY = 1;
	public static final double MAX_SPEED = 3;

	@SuppressWarnings("unused")
	private MovingFood() {
		// Required by JAXB
	}

	public MovingFood(double x, double y, double angle, double speed) {
		super(x, y, angle, speed);

		setEnergy(ENERGY);
	}

	@Override
	public MovingFood reproduce(IEnvironment env) {
		Random random = new Random();
		double angle = random.nextDouble() * 2*Math.PI;
		double speed = random.nextDouble() * MAX_SPEED;
		MovingFood newFood = new MovingFood(getX(), getY(), angle, speed);
		env.addAgent(newFood);
		return newFood;
	}

}
