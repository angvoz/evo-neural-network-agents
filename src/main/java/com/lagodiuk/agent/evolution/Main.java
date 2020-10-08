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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.lagodiuk.agent.AgentsEnvironment;
import com.lagodiuk.agent.FertileAgent;
import com.lagodiuk.agent.IAgent;
import com.lagodiuk.agent.IFood;
import com.lagodiuk.agent.MovingAgent;
import com.lagodiuk.agent.MovingFood;
import com.lagodiuk.agent.StaticFood;
import com.lagodiuk.ga.Fitness;
import com.lagodiuk.ga.GeneticAlgorithm;
import com.lagodiuk.ga.IterartionListener;
import com.lagodiuk.ga.Population;
import com.lagodiuk.nn.NeuralNetwork;
import com.lagodiuk.nn.genetic.OptimizableNeuralNetwork;

public class Main {
	private static Visualizator visualizator;

	private static final String PREFS_KEY_SAVE_DIRECTORY = "BrainsDirectory";

	private static Random random = new Random();

	private static GeneticAlgorithm<OptimizableNeuralNetwork, Double> ga;

	private static AgentsEnvironment environment;

	private static int populationNumber = 0;

	private static volatile boolean play = true;

	private static volatile boolean staticFood = false;

	// UI

	private static JFrame appFrame;

	private static JPanel environmentPanel;

	private static JPanel controlsPanel;

	private static JTextField evolveTextField;

	private static JButton evolveButton;

	private static JButton playPauseButton;

	private static JButton loadButton;

	private static JButton saveButton;

	private static JButton resetButton;

	private static JRadioButton staticFoodRadioButton;

	private static JRadioButton dynamicFoodRadioButton;

	private static ButtonGroup foodTypeButtonGroup;

	private static JProgressBar progressBar;

	private static JLabel statusBar;

	private static JLabel populationInfoLabel;

	private static BufferedImage displayEnvironmentBufferedImage;

	private static Graphics2D displayEnvironmentCanvas;

	private static JFileChooser fileChooser;

	private static Preferences prefs;

	public static void main(String[] args) throws Exception {
		// TODO maybe, add ability to define these parameters as environment
		// constants
		int gaPopulationSize = 5;
		int parentalChromosomesSurviveCount = 1;
		int environmentWidth = 1470;
		int environmentHeight = 850;
		int agentsCount = 50;
		int minNumberOfAgents = 10;
		int foodCount = 1000 - agentsCount * FertileAgent.NEWBORN_ENERGY_DEFAULT;

		initializeGeneticAlgorithm(gaPopulationSize, parentalChromosomesSurviveCount, null);

		initializeEnvironment(environmentWidth, environmentHeight - 11, agentsCount, foodCount, minNumberOfAgents);

		initializeCanvas(environmentWidth, environmentHeight);

		initializeUI(environmentWidth, environmentHeight);

		initializeEvolveButtonFunctionality();

		initializePlayPauseButtonFunctionality();

		initializeAddingFoodFunctionality();

		initializeLoadFunctionality();

		initializeSaveFunctionality();

		initializeChangingFoodTypeFunctionality();

		initializeResetButtonFunctionality();

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

	private static IFood createRandomFood(int width, int height) {
		int x = random.nextInt(width);
		int y = random.nextInt(height);

		IFood food = null;
		if (staticFood) {
			food = new StaticFood(x, y);
		} else {
			double speed = random.nextDouble() * 2;
			double direction = random.nextDouble() * 2 * Math.PI;

			food = new MovingFood(x, y, direction, speed);
		}
		return food;
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
		appFrame = new JFrame("Evolving neural network driven agents");
		appFrame.setSize(environmentWidth + 130, environmentHeight + 50);
		appFrame.setResizable(false);
		appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		appFrame.setLayout(new BorderLayout());

		environmentPanel = new JPanel();
		environmentPanel.setSize(environmentWidth, environmentHeight);
		appFrame.add(environmentPanel, BorderLayout.CENTER);

		Dimension buttonSize = new Dimension(100, 40);
		int hgap = 0;
		int vgap = 5;
		int envHeight = environment.getHeight();
		int buttonRows = (int) (envHeight / (buttonSize.getHeight() + vgap + 5));
		controlsPanel = new JPanel();
		appFrame.add(controlsPanel, BorderLayout.EAST);
		controlsPanel.setLayout(new GridLayout(buttonRows, 1, hgap, vgap));

		playPauseButton = addNewButton(controlsPanel, play ? "Pause" : "Start", buttonSize, true);
		saveButton = addNewButton(controlsPanel, "Save", buttonSize, !play);
		loadButton = addNewButton(controlsPanel, "Load", buttonSize, !play);

		evolveTextField = new JTextField("10");
		controlsPanel.add(evolveTextField);

		evolveButton = new JButton("evolve");
		controlsPanel.add(evolveButton);

		staticFoodRadioButton = new JRadioButton("static food");
		dynamicFoodRadioButton = new JRadioButton("dynamic food");
		foodTypeButtonGroup = new ButtonGroup();
		foodTypeButtonGroup.add(staticFoodRadioButton);
		foodTypeButtonGroup.add(dynamicFoodRadioButton);
		controlsPanel.add(staticFoodRadioButton);
		controlsPanel.add(dynamicFoodRadioButton);
		if (staticFood) {
			staticFoodRadioButton.setSelected(true);
		} else {
			dynamicFoodRadioButton.setSelected(true);
		}

		resetButton = new JButton("reset");
		controlsPanel.add(resetButton);

		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setVisible(false);
		appFrame.add(progressBar, BorderLayout.SOUTH);

		statusBar = new JLabel();
		statusBar.setPreferredSize(new Dimension(100, 16));
		appFrame.add(statusBar, java.awt.BorderLayout.SOUTH);

		populationInfoLabel = new JLabel("Population: " + populationNumber, SwingConstants.CENTER);
		appFrame.add(populationInfoLabel, BorderLayout.NORTH);

		prefs = Preferences.userNodeForPackage(Main.class);
		String saveDirPath = prefs.get(PREFS_KEY_SAVE_DIRECTORY, "");
		fileChooser = new JFileChooser(new File(saveDirPath));
	}

	protected static void initializeChangingFoodTypeFunctionality() {
		ItemListener changingFoodTypeListener = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					disableControls();
					boolean wasPlaying = play;
					play = false;

					staticFood = !staticFood;

					List<IFood> foods = new LinkedList<IFood>();
					for (IFood f : environment.getFood()) {
						foods.add(f);
					}
					for (IFood f : foods) {
						environment.removeFood(f);

						IFood newFood = createRandomFood(1, 1);
						newFood.setX(f.getX());
						newFood.setY(f.getY());

						environment.addFood(newFood);
					}

					play = wasPlaying;
					enableControls();
				}
			}
		};
		staticFoodRadioButton.addItemListener(changingFoodTypeListener);
		dynamicFoodRadioButton.addItemListener(changingFoodTypeListener);
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

	private static void initializeResetButtonFunctionality() {
		resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				disableControls();

				int populationSize = ga.getPopulation().getSize();
				int parentalChromosomesSurviveCount = ga.getParentChromosomesSurviveCount();
				initializeGeneticAlgorithm(populationSize, parentalChromosomesSurviveCount, null);

				NeuralNetwork newBrain = ga.getBest();

				setAgentBrains(newBrain, 0);

				// reset population number counter
				populationNumber = 0;
				populationInfoLabel.setText("Population: " + populationNumber);

				enableControls();
			}
		});
	}

	private static void initializeEvolveButtonFunctionality() {
		evolveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				disableControls();
				progressBar.setVisible(true);
				progressBar.setValue(0);
				environmentPanel.getGraphics().drawImage(displayEnvironmentBufferedImage, 0, 0, null);

				String iterCountStr = evolveTextField.getText();
				if (!iterCountStr.matches("\\d+")) {
					evolveButton.setEnabled(true);
					evolveTextField.setEnabled(true);
					progressBar.setVisible(false);
					environmentPanel.getGraphics().drawImage(displayEnvironmentBufferedImage, 0, 0, null);
					return;
				}

				final int iterCount = Integer.parseInt(iterCountStr);

				new Thread(new Runnable() {
					@Override
					public void run() {
						IterartionListener<OptimizableNeuralNetwork, Double> progressListener =
								new IterartionListener<OptimizableNeuralNetwork, Double>() {
									@Override
									public void update(GeneticAlgorithm<OptimizableNeuralNetwork, Double> environment) {
										final int iteration = environment.getIteration();
										SwingUtilities.invokeLater(new Runnable() {
											@Override
											public void run() {
												progressBar.setValue((iteration * 100) / iterCount);
											}
										});
									}
								};

						ga.addIterationListener(progressListener);
						ga.evolve(iterCount);
						ga.removeIterationListener(progressListener);
						populationNumber += iterCount;

						NeuralNetwork newBrain = ga.getBest();
						setAgentBrains(newBrain, populationNumber);

						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								progressBar.setVisible(false);
								populationInfoLabel.setText("Population: " + populationNumber);
								enableControls();
								evolveButton.requestFocusInWindow();
							}
						});
					}
				}).start();
			}
		});
	}

	private static void disableControls() {
		evolveButton.setEnabled(false);
		evolveTextField.setEnabled(false);
		loadButton.setEnabled(false);
		saveButton.setEnabled(false);
		staticFoodRadioButton.setEnabled(false);
		dynamicFoodRadioButton.setEnabled(false);
	}

	private static void enableControls() {
		evolveButton.setEnabled(true);
		evolveTextField.setEnabled(true);
		loadButton.setEnabled(!play);
		saveButton.setEnabled(!play);
		staticFoodRadioButton.setEnabled(true);
		dynamicFoodRadioButton.setEnabled(true);
	}

	private static void initializeAddingFoodFunctionality() {
		environmentPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent click) {
				double x = click.getX();
				double y = click.getY();

				if (SwingUtilities.isLeftMouseButton(click)) {
					IFood food = createRandomFood(1, 1);
					food.setX(x);
					food.setY(y);
					environment.addFood(food);
				} else {
					double angle = 2 * Math.PI * random.nextDouble();
					double speed = 0;
					NeuralNetworkDrivenAgent agent = new NeuralNetworkDrivenAgent(x, y, angle, speed);
					OptimizableNeuralNetwork brain = ga.getBest();
					agent.setBrain(brain);
					environment.addAgent(agent);
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
			double direction = random.nextDouble() * 2 * Math.PI;
			double speed = random.nextDouble() * MovingAgent.MAX_SPEED;

			NeuralNetworkDrivenAgent agent = new NeuralNetworkDrivenAgent(x, y, direction, speed);
			NeuralNetwork brain = NeuralNetworkDrivenAgent.randomNeuralNetworkBrain();
			agent.setBrain(brain);

			environment.addAgent(agent);
		}
	}

	private static void initializeFood(int foodCount) {
		int environmentWidth = environment.getWidth();
		int environmentHeight = environment.getHeight();

		for (int i = 0; i < foodCount; i++) {
			IFood food = createRandomFood(environmentWidth, environmentHeight);
			environment.addFood(food);
		}
	}

	private static void initializeGeneticAlgorithm(
			int populationSize,
			int parentalChromosomesSurviveCount,
			OptimizableNeuralNetwork baseNeuralNetwork) {
		Population<OptimizableNeuralNetwork> brains = new Population<OptimizableNeuralNetwork>();

		for (int i = 0; i < (populationSize - 1); i++) {
			if (baseNeuralNetwork == null) {
				brains.addChromosome(NeuralNetworkDrivenAgent.randomNeuralNetworkBrain());
			} else {
				brains.addChromosome(baseNeuralNetwork.mutate());
			}
		}
		if (baseNeuralNetwork != null) {
			brains.addChromosome(baseNeuralNetwork);
		} else {
			brains.addChromosome(NeuralNetworkDrivenAgent.randomNeuralNetworkBrain());
		}

		Fitness<OptimizableNeuralNetwork, Double> fit = new TournamentEnvironmentFitness();

		ga = new GeneticAlgorithm<OptimizableNeuralNetwork, Double>(brains, fit);

		addGASystemOutIterationListener();

		ga.setParentChromosomesSurviveCount(parentalChromosomesSurviveCount);
	}

	private static void addGASystemOutIterationListener() {
		ga.addIterationListener(new IterartionListener<OptimizableNeuralNetwork, Double>() {
			@Override
			public void update(GeneticAlgorithm<OptimizableNeuralNetwork, Double> ga) {
				OptimizableNeuralNetwork bestBrain = ga.getBest();
				Double fit = ga.fitness(bestBrain);
				System.out.println(ga.getIteration() + "\t" + fit);

				ga.clearCache();
			}
		});
	}

	private static void setAgentBrains(NeuralNetwork newBrain, int generation) {
		for (FertileAgent fish : environment.getFishes()) {
			if (fish instanceof NeuralNetworkDrivenAgent) {
				((NeuralNetworkDrivenAgent) fish).setBrain(newBrain.clone());
				((NeuralNetworkDrivenAgent) fish).setGeneration(generation);
			}
		}
	}
}
