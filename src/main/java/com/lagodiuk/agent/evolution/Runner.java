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
package com.lagodiuk.agent.evolution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.lagodiuk.environment.Environment;

public class Runner {
	private static final int MAX_ITERATIONS = 1000000;

	private static Environment environment;
	private static String filename = null;
	private static boolean justStarted = true;

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("Usage: Provide file.xml as an argument");
			return;
		}

		filename = args[0];
		if (new File(filename).getParent() == null) {
			filename = System.getProperty("user.dir") + File.separator + filename;
		}

		if (new File(filename).exists()) {
			loadWorld(filename);
			justStarted = false;
		} else {
			createWorld();
			justStarted = true;
		}

		mainEnvironmentLoop();
	}

	private static void mainEnvironmentLoop() throws Exception {
		for (;;) {
			environment.timeStep();
			int time = (int) environment.getTime();
			if (time % 1000 == 0) {
				if (!justStarted && !new File(filename).exists()) {
					System.out.println("Info: Time=" + time + ", " + filename + " was moved, exiting");
					break;
				}

				saveWorld(filename);

				if (!justStarted && time % MAX_ITERATIONS == 0) {
					System.out.println("Info: Time=" + time + " exceeded limit, exiting");
					break;
				}
				justStarted = false;
			}
		}
	}

	private static void createWorld() {
		environment = new Environment(DefaultWorldParameters.environmentWidth, DefaultWorldParameters.environmentHeight);
		environment.initialize(DefaultWorldParameters.agentsDensity, DefaultWorldParameters.foodDensity);
		environment.setMinNumberOfAgents(DefaultWorldParameters.minNumberOfAgents);
	}

	private static void loadWorld(String filename) throws Exception {
		File file = new File(filename);
		FileInputStream in = new FileInputStream(file);
		environment = Environment.unmarshall(in);
		in.close();
	}

	private static void saveWorld(String filename) throws Exception {
		File file = new File(filename);
		FileOutputStream out = new FileOutputStream(file);
		Environment.marshall(environment, out);
		out.close();
	}
}
