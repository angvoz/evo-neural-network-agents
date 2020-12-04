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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.lagodiuk.agent.AgentsEnvironment;
import com.lagodiuk.agent.FertileAgent;
import com.lagodiuk.agent.IAgent;
import com.lagodiuk.agent.IFood;
import com.lagodiuk.agent.MovingAgent;
import com.lagodiuk.nn.NeuralNetwork;
import com.lagodiuk.nn.NeuralNetworkDrivenAgent;

public class Main {
	private static Visualizator visualizator;

	private static final String PREFS_KEY_SAVE_DIRECTORY = "BrainsDirectory";

	private static Random random = new Random();

	private static AgentsEnvironment environment;

	private static volatile boolean play = true;

	// UI

	private static JFrame appFrame;

	private static JPanel environmentPanel;

	private static JPanel controlsPanel;

	private static JButton playPauseButton;

	private static JButton loadButton;

	private static JButton saveButton;

	private static JProgressBar progressBar;

	private static JLabel statusBar;

	private static BufferedImage displayEnvironmentBufferedImage;

	private static Graphics2D displayEnvironmentCanvas;

	private static JFileChooser fileChooser;

	private static Preferences prefs;

	public static void main(String[] args) throws Exception {
		// TODO maybe, add ability to define these parameters as environment
		// constants
		int environmentWidth = 1470;
		int environmentHeight = 850;
		int agentsCount = 50;
		int minNumberOfAgents = 10;
		// Density per million pixels
		int foodDensity = 800;
		int totalEnergy = environmentWidth * environmentHeight * foodDensity / 1000000;
		int foodCount = totalEnergy - agentsCount * FertileAgent.NEWBORN_ENERGY_DEFAULT;

		initializeEnvironment(environmentWidth, environmentHeight, agentsCount, foodCount, minNumberOfAgents);

		initializeCanvas(environmentWidth, environmentHeight);

		initializeUI(environmentWidth, environmentHeight);

		initializePlayPauseButtonFunctionality();

		initializeAddingFoodFunctionality();

		initializeLoadFunctionality();

		initializeSaveFunctionality();

		displayUI();

		enableControls();

		mainEnvironmentLoop();
	}

	private static void updateStatusBar(AgentsEnvironment env) {
		int countFishes = 0;
		int countFood = 0;
		int energyReserve = env.getEnergyReserve();
		int countEnergy = 0 + energyReserve;
		int longestGeneration = 0;
		int shortestGeneration = Integer.MAX_VALUE;
		HashMap<Integer, Integer> generationCountMap = new HashMap<>();
		for (IAgent agent : env.getAgents()) {
			if (agent instanceof IFood) {
				countFood++;
				countEnergy += 1;
			} else if (agent instanceof FertileAgent) {
				countFishes++;
				countEnergy += ((FertileAgent) agent).getEnergy();
				if (agent instanceof NeuralNetworkDrivenAgent) {
					int generation = ((NeuralNetworkDrivenAgent) agent).getGeneration();
					longestGeneration = Math.max(generation, longestGeneration);
					shortestGeneration = Math.min(generation, shortestGeneration);
					Integer count = generationCountMap.get(generation);
					if (count == null) {
						count = 0;
					}
					count++;
					generationCountMap.put(generation, count);
				}
			}
		}
		String biggestGenerationStr = "";
		int biggestCount = 0;
		for (Entry<Integer, Integer> entry : generationCountMap.entrySet()) {
			Integer gen = entry.getKey();
			Integer count = entry.getValue();
			if (count >= biggestCount) {
				if (count > biggestCount) {
					biggestGenerationStr = "" + gen + "(" + count + " fish)";
				} else {
					biggestGenerationStr = biggestGenerationStr + ", " + gen + "(" + count + " fish)";
				}
				biggestCount = count;
			}
		}
		statusBar.setText("Time: " + (int) env.getTime()
		+ ",   Energy Total: " + countEnergy
		+ ",   Energy Reserve: " + energyReserve
		+ ",   Food: " + countFood
		+ ",   Fishes: " + countFishes
		+ ",   Mutations: " + NeuralNetworkDrivenAgent.getMutationCount()
		+ ",   Shortest/Biggest/Longest Generation: " + shortestGeneration + "(" + generationCountMap.get(shortestGeneration) + " fish)" + "/" + biggestGenerationStr + "/" + longestGeneration + "(" + generationCountMap.get(longestGeneration) + " fish)"
				);
	}

	private static void mainEnvironmentLoop() throws InterruptedException {
		for (;;) {
			Thread.sleep(5);
			if (play) {
				environment.timeStep();
			}
			visualizator.paintEnvironment(displayEnvironmentCanvas);
			updateStatusBar(environment);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					environmentPanel.getGraphics().drawImage(displayEnvironmentBufferedImage, 0, 0, null);
				}
			});
		}
	}

	private static void initializeCanvas(int environmentWidth, int environmentHeight) {
		displayEnvironmentBufferedImage = new BufferedImage(environmentWidth, environmentHeight, BufferedImage.TYPE_INT_RGB);

		displayEnvironmentCanvas = (Graphics2D) displayEnvironmentBufferedImage.getGraphics();
		displayEnvironmentCanvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	private static void initializeEnvironment(int environmentWidth, int environmentHeight, int agentsCount, int foodCount, int minNumberOfAgents) {
		environment = new AgentsEnvironment(environmentWidth, environmentHeight);
		visualizator = new Visualizator(environment);

		initializeAgents(agentsCount, minNumberOfAgents);
		initializeFood(foodCount);
	}

	private static void displayUI() {
		// put application frame to the center of screen
		appFrame.setLocationRelativeTo(null);

		appFrame.setVisible(true);
	}

	private static JButton addNewButton(JPanel controlsPanel, String text, Dimension buttonSize, boolean enabled) {
		JButton button = new JButton(text);
		button.setPreferredSize(buttonSize);

		JPanel panel = new JPanel();
		panel.add(button);
		controlsPanel.add(panel);

		return button;
	}

	private static void initializeUI(int environmentWidth, int environmentHeight) {
		int buttonWidth = 100;
		int buttonHeight = 40;
		int horButtonGap = 10;
		int verButtonGap = 5;
		int statusBarHeight = 56;

		appFrame = new JFrame("Evolving neural network driven agents");
		appFrame.setSize(environmentWidth + buttonWidth + horButtonGap * 2, environmentHeight + statusBarHeight);
		appFrame.setResizable(false);
		appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		appFrame.setLayout(new BorderLayout());

		environmentPanel = new JPanel();
		environmentPanel.setSize(environmentWidth, environmentHeight);
		appFrame.add(environmentPanel, BorderLayout.CENTER);

		Dimension buttonSize = new Dimension(buttonWidth, buttonHeight);
		int envHeight = environment.getHeight();
		int buttonRows = (int) (envHeight / (buttonSize.getHeight() + verButtonGap + 5));
		controlsPanel = new JPanel();
		appFrame.add(controlsPanel, BorderLayout.EAST);
		controlsPanel.setLayout(new GridLayout(buttonRows, 1, horButtonGap, verButtonGap));

		playPauseButton = addNewButton(controlsPanel, play ? "Pause" : "Start", buttonSize, true);
		saveButton = addNewButton(controlsPanel, "Save", buttonSize, !play);
		loadButton = addNewButton(controlsPanel, "Load", buttonSize, !play);

		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setVisible(false);
		appFrame.add(progressBar, BorderLayout.SOUTH);

		statusBar = new JLabel();
		statusBar.setPreferredSize(new Dimension(100, 16));
		appFrame.add(statusBar, java.awt.BorderLayout.SOUTH);

		prefs = Preferences.userNodeForPackage(Main.class);
		String saveDirPath = prefs.get(PREFS_KEY_SAVE_DIRECTORY, "");
		fileChooser = new JFileChooser(new File(saveDirPath));
	}

	private static void initializeLoadFunctionality() {
		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				disableControls();

				int returnVal = fileChooser.showOpenDialog(appFrame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						File file = fileChooser.getSelectedFile();
						prefs.put(PREFS_KEY_SAVE_DIRECTORY, file.getParent());

						FileInputStream in = new FileInputStream(file);
						environment = AgentsEnvironment.unmarshall(in);
						in.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				enableControls();
			}
		});
	}

	private static void initializeSaveFunctionality() {
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				disableControls();

				int returnVal = fileChooser.showSaveDialog(appFrame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						File file = fileChooser.getSelectedFile();
						prefs.put(PREFS_KEY_SAVE_DIRECTORY, file.getParent());

						FileOutputStream out = new FileOutputStream(file);
						AgentsEnvironment.marshall(environment, out);
						out.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				enableControls();
			}
		});
	}

	private static void disableControls() {
		loadButton.setEnabled(false);
		saveButton.setEnabled(false);
	}

	private static void enableControls() {
		loadButton.setEnabled(!play);
		saveButton.setEnabled(!play);
	}

	private static void initializeAddingFoodFunctionality() {
		environmentPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent click) {
				int x = click.getX();
				int y = click.getY();

				if (SwingUtilities.isRightMouseButton(click)) {
					environment.createRandomFood(x, y);
				}
			}
		});
	}

	private static void initializePlayPauseButtonFunctionality() {
		playPauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				play = !play;
				if (play) {
					playPauseButton.setText("Pause");
				} else {
					playPauseButton.setText("Continue");
				}
				enableControls();
			}
		});
	}

	private static void initializeAgents(int agentsCount, int minNumberOfAgents) {
		int environmentWidth = environment.getWidth();
		int environmentHeight = environment.getHeight();
		environment.setMinNumberOfAgents(minNumberOfAgents);

		for (int i = 0; i < agentsCount; i++) {
			int x = random.nextInt(environmentWidth);
			int y = random.nextInt(environmentHeight);
			double direction = random.nextDouble() * 2*Math.PI;
			double speed = random.nextDouble() * MovingAgent.MAX_SPEED;

			NeuralNetworkDrivenAgent agent = new NeuralNetworkDrivenAgent(x, y, direction, speed);
			NeuralNetwork brain = NeuralNetworkDrivenAgent.randomNeuralNetworkBrain();
			agent.setBrain(brain);

			environment.addAgent(agent);
		}
	}

	private static void initializeFood(int foodCount) {
		for (int i = 0; i < foodCount; i++) {
			environment.addNewRandomFood();
		}
	}

}
