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
import java.awt.Color;
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
import java.util.List;
import java.util.Map.Entry;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.lagodiuk.agent.AgentsEnvironment;
import com.lagodiuk.agent.FertileAgent;
import com.lagodiuk.agent.IAgent;
import com.lagodiuk.agent.IFood;
import com.lagodiuk.agent.MovingAgent;
import com.lagodiuk.nn.NeuralNetworkDrivenAgent;

public class Visualizator {
	private static final String PREFS_KEY_SAVE_DIRECTORY = "BrainsDirectory";

	private AgentsEnvironment environment;

	private static JFrame appFrame;
	private static JPanel environmentPanel;
	private static JPanel controlsPanel;
	private static JButton playPauseButton;
	private static JButton loadButton;
	private static JButton saveButton;
	private static JLabel statusBar;
	private static BufferedImage displayEnvironmentBufferedImage;
	private static Graphics2D displayEnvironmentCanvas;
	private static JFileChooser fileChooser;
	private static Preferences prefs;
	volatile boolean play = true;
	private int timeBetweenFrames = 5;

	private static NeuralNetworkDrivenAgent selectedAgent;

	public Visualizator(AgentsEnvironment environment) {
		this.environment = environment;
	}

	public AgentsEnvironment getEnvironment() {
		return environment;
	}

	private static void initializeCanvas(int environmentWidth, int environmentHeight) {
		displayEnvironmentBufferedImage = new BufferedImage(environmentWidth, environmentHeight,
				BufferedImage.TYPE_INT_RGB);

		displayEnvironmentCanvas = (Graphics2D) displayEnvironmentBufferedImage.getGraphics();
		displayEnvironmentCanvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

	private void initializeUI(int environmentWidth, int environmentHeight) {
		int buttonWidth = 100;
		int buttonHeight = 40;
		int horButtonGap = 10;
		int verButtonGap = 5;
		int statusBarHeight = 56;
		int horFrameGap = 20;

		appFrame = new JFrame("Evolving neural network driven agents");
		appFrame.setSize(environmentWidth + buttonWidth + horButtonGap * 2 + horFrameGap, environmentHeight + statusBarHeight);
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

		statusBar = new JLabel();
		statusBar.setPreferredSize(new Dimension(100, 16));
		appFrame.add(statusBar, java.awt.BorderLayout.SOUTH);

		prefs = Preferences.userNodeForPackage(Main.class);
		String saveDirPath = prefs.get(PREFS_KEY_SAVE_DIRECTORY, "");
		fileChooser = new JFileChooser(new File(saveDirPath));
	}

	private void initializeLoadFunctionality() {
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

	private void initializeSaveFunctionality() {
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

	private void enableControls() {
		loadButton.setEnabled(!play);
		saveButton.setEnabled(!play);
	}

	private void initializeAddingFoodFunctionality() {
		environmentPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent click) {
				int x = click.getX();
				int y = click.getY();

				if (SwingUtilities.isLeftMouseButton(click)) {
					setSelectedAgent(x, y);
				} else if (SwingUtilities.isRightMouseButton(click)) {
					environment.createRandomFood(x, y);
				}
			}
		});
	}

	private void initializePlayPauseButtonFunctionality() {
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

	private void updateStatusBar(AgentsEnvironment env) {
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
		String generationStatus = "";
		if (countFishes > 0) {
			generationStatus = ",   Shortest/Biggest/Longest Generation: "
							+ shortestGeneration + "(" + generationCountMap.get(shortestGeneration) + " fish)" + "/"
							+ biggestGenerationStr + "/" + longestGeneration + "(" + generationCountMap.get(longestGeneration)
							+ " fish)";
		}
		String selectedStatus = "";
		if (selectedAgent != null) {
			selectedStatus = ",  Selected: energy=" + selectedAgent.getEnergy()
					+ ", generation=" + selectedAgent.getGeneration();
		}
		statusBar.setText("Time: " + (int) env.getTime() + ",   Energy Total: " + countEnergy + ",   Energy Reserve: "
				+ energyReserve + ",   Food: " + countFood + ",   Fishes: " + countFishes + ",   Mutations: "
				+ NeuralNetworkDrivenAgent.getMutationCount() + generationStatus + selectedStatus);
	}

	public NeuralNetworkDrivenAgent getSelectedAgent() {
		return selectedAgent;
	}

	public void setSelectedAgent(double x, double y) {
		double min = Double.MAX_VALUE;
		NeuralNetworkDrivenAgent closestAgent = null;
		for (IAgent agent : getEnvironment().getAgents()) {
			if (agent instanceof NeuralNetworkDrivenAgent) {
				double dx = x - agent.getX();
				double dy = y - agent.getY();
				double distFactor = dx * dx + dy * dy;
				if (distFactor < min) {
					min = distFactor;
					closestAgent = (NeuralNetworkDrivenAgent) agent;
				}
			}
		}
		double dist = Math.sqrt(min);
		double range = 2 * closestAgent.getRadius() + 200;
		if (dist <= range) {
			selectedAgent = closestAgent;
		} else {
			selectedAgent = null;
		}
	}

	private Color getColorFood() {
		Color colorFood = new Color(255, 255, 0);
		return colorFood;
	}

	private Color getColorBody(FertileAgent agent) {
		Color colorBody = Color.GRAY;
		if (agent.getEnergy() <= 0) {
			colorBody = Color.BLACK;
		} else {
			int red = Math.max(255 - agent.getEnergy() * 25, 0);
			int green = Math.min(agent.getEnergy() * 25, 255);
			colorBody = new Color(red, green, 0);
		}
		return colorBody;
	}

	private Color colorBodyOutline(FertileAgent agent) {
		Color colorBodyOutline = Color.GRAY;
		if (agent.getEnergy() <= 0) {
			colorBodyOutline = Color.WHITE;
		} else {
			int agentEnergy = agent.getEnergy();
			if (agentEnergy <= 1) {
				colorBodyOutline = Color.RED;
			} else if (agentEnergy >= 9) {
				colorBodyOutline = Color.GREEN;
			} else {
				colorBodyOutline = Color.WHITE;
			}
		}
		return colorBodyOutline;
	}

	private Color getColorFlag(FertileAgent agent) {
		Color colorFlag = null;
		if (agent instanceof NeuralNetworkDrivenAgent) {
			final int maxGeneration = environment.getLongestGeneration();
			int generation = ((NeuralNetworkDrivenAgent) agent).getGeneration();
			if (agent == selectedAgent) {
				colorFlag = Color.CYAN;
			} else if (generation == maxGeneration) {
				colorFlag = Color.MAGENTA;
			}
		}
		return colorFlag;
	}

	private void markFood(Graphics2D canvas, IFood food, Color color) {
		if (food != null) {
			canvas.setColor(color);
			// Small triangle flag
			int x0 = (int) food.getX();
			int y0 = (int) food.getY() - (int)food.getRadius()*2 - 1;
			int x1 = x0;
			int y1 = y0 - 8;
			int x2 = x0 + 6;
			int y2 = y0 - 6;
			int x3 = x0;
			int y3 = y0 - 4;
			canvas.drawLine(x0, y0, x1, y1);
			canvas.drawLine(x1, y1, x2, y2);
			canvas.drawLine(x3, y3, x2, y2);
		}
	}

	private void drawFood(Graphics2D canvas) {
		Color colorFood = getColorFood();
		canvas.setColor(colorFood);
		for (IFood food : environment.getFood()) {
			int x = (int) food.getX();
			int y = (int) food.getY();
			int foodRadius = (int) food.getRadius();

			canvas.fillOval(x - foodRadius, y - foodRadius, foodRadius * 2, foodRadius * 2);
		}
	}

	private void markAgent(Graphics2D canvas, MovingAgent agent, int x, int y, Color color) {
		int agentRadius = (int) agent.getRadius();
		canvas.setColor(color);
		{
			// Big triangle flag
			int triX1 = x;
			int triY1 = y - agentRadius - 16;
			int triX2 = x + 10;
			int triY2 = triY1 + 4;
			int triX3 = x;
			int triY3 = triY2 + 4;
			canvas.drawLine(x, y - agentRadius, triX1, triY1);
			canvas.drawLine(triX1, triY1, triX2, triY2);
			canvas.drawLine(triX3, triY3, triX2, triY2);
		}
		{
			// Small triangle flag inside the big one
			int triX1 = x;
			int triY1 = y - agentRadius - 14;
			int triX2 = x + 6;
			int triY2 = triY1 + 2;
			int triX3 = x;
			int triY3 = triY2 + 2;
			canvas.drawLine(x, y - agentRadius, triX1, triY1);
			canvas.drawLine(triX1, triY1, triX2, triY2);
			canvas.drawLine(triX3, triY3, triX2, triY2);
		}
	}

	private void drawAgentBody(Graphics2D canvas, FertileAgent agent, int x, int y, Color colorBody, Color colorBodyOutline) {
		int agentRadius = (int) agent.getRadius();
		canvas.setColor(colorBody);
		canvas.fillOval(x - agentRadius, y - agentRadius, agentRadius * 2, agentRadius * 2);
		canvas.setColor(colorBodyOutline);
		canvas.drawOval(x - agentRadius, y - agentRadius, agentRadius * 2, agentRadius * 2);
	}

	private void drawAgentEye(Graphics2D canvas, MovingAgent agent, double theta) {
		int radiusEyeBase = (int) agent.getRadius() - 1;
		int radiusEye = 1;
		int diameterEye = radiusEye * 2;
		double angleEye = agent.getAngle() + theta;
		double rx = Math.cos(angleEye);
		int x = (int) (rx * radiusEyeBase + agent.getX() - radiusEye);
		double ry = Math.sin(angleEye);
		int y = (int) (ry * radiusEyeBase + agent.getY() - radiusEye);

		canvas.drawOval(x, y, diameterEye, diameterEye);
	}

	private void drawAgentTail(Graphics2D canvas, FertileAgent agent, int x, int y) {
		int signSpeed = -(int) Math.signum(agent.getSpeed());
		double agentRadius = agent.getRadius();
		int rx = (int) ((agent.getRx() * (agentRadius + 4) * signSpeed) + x);
		int ry = (int) ((agent.getRy() * (agentRadius + 4) * signSpeed) + y);

		canvas.drawLine(x, y, rx, ry);
	}

	private void replicateAgentAt(Graphics2D canvas, FertileAgent agent, int x, int y, Color colorBody, Color colorBodyOutline, Color colorFlag) {
		drawAgentBody(canvas, agent, x, y, colorBody, colorBodyOutline);
		canvas.setColor(colorBodyOutline);
		drawAgentEye(canvas, agent, 0.3);
		drawAgentEye(canvas, agent, -0.3);
		drawAgentTail(canvas, agent, x, y);

		if (colorFlag != null) {
			markAgent(canvas, agent, x, y, colorFlag);
		}
	}

	private void drawAgent(Graphics2D canvas, FertileAgent agent) {
		Color colorBody = getColorBody(agent);
		Color colorBodyOutline = colorBodyOutline(agent);
		Color colorFlag = getColorFlag(agent);

		int x = (int) agent.getX();
		int y = (int) agent.getY();

		int envWidth = environment.getWidth();
		int envHeight = environment.getHeight();
		int distanceFromBorder = (int) NeuralNetworkDrivenAgent.EYESIGHT_DISTANCE;

		replicateAgentAt(canvas, agent, x, y, colorBody, colorBodyOutline, colorFlag);

		// Replicate agent on the other sides of the boundaries
		if (x - distanceFromBorder < 0) {
			replicateAgentAt(canvas, agent, x + envWidth, y, colorBody, colorBodyOutline, colorFlag);
		}
		if (x + distanceFromBorder > envWidth) {
			replicateAgentAt(canvas, agent, x - envWidth, y, colorBody, colorBodyOutline, colorFlag);
		}
		if (y - distanceFromBorder < 0) {
			replicateAgentAt(canvas, agent, x, y + envHeight, colorBody, colorBodyOutline, colorFlag);
		}
		if (y + distanceFromBorder > envHeight) {
			replicateAgentAt(canvas, agent, x, y - envHeight, colorBody, colorBodyOutline, colorFlag);
		}
		if (x - distanceFromBorder < 0 && y - distanceFromBorder < 0) {
			replicateAgentAt(canvas, agent, x + envWidth, y + envHeight, colorBody, colorBodyOutline, colorFlag);
		}

		if (x + distanceFromBorder > envWidth && y - distanceFromBorder < 0) {
			replicateAgentAt(canvas, agent, x - envWidth, y + envHeight, colorBody, colorBodyOutline, colorFlag);
		}
		if (x - distanceFromBorder < 0 && y + distanceFromBorder > envHeight) {
			replicateAgentAt(canvas, agent, x + envWidth, y - envHeight, colorBody, colorBodyOutline, colorFlag);
		}
		if (x + distanceFromBorder > envWidth && y + distanceFromBorder > envHeight) {
			replicateAgentAt(canvas, agent, x - envWidth, y - envHeight, colorBody, colorBodyOutline, colorFlag);
		}

		if (colorFlag != null) {
			if (agent instanceof NeuralNetworkDrivenAgent) {
				for (IFood food : environment.getFood()) {
					if (((NeuralNetworkDrivenAgent) agent).inSight(food)) {
						markFood(canvas, food, colorFlag);
					}
				}
			}
		}
	}

	private void drawAgents(Graphics2D canvas) {
		List<FertileAgent> agents = environment.getFishes();
		for (FertileAgent agent : agents) {
			if (agent != selectedAgent) {
				drawAgent(canvas, agent);
			}
		}
		// Draw separately to mark food over last generation marks
		if (agents.contains(selectedAgent)) {
			drawAgent(canvas, selectedAgent);
		} else {
			selectedAgent = null;
		}
	}

	public void paintEnvironment(Graphics2D canvas) {
		canvas.clearRect(0, 0, environment.getWidth(), environment.getHeight());

		drawAgents(canvas);
		drawFood(canvas);
	}

	public void paintEnvironment() throws InterruptedException {
		paintEnvironment(displayEnvironmentCanvas);
		updateStatusBar(environment);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				environmentPanel.getGraphics().drawImage(displayEnvironmentBufferedImage, 0, 0, null);
			}
		});
		Thread.sleep(play ? timeBetweenFrames : 150);
	}

	public void timeStep() throws InterruptedException {
		if (play) {
			environment.timeStep();
		}
		paintEnvironment();
	}

	public void initialize(int environmentWidth, int environmentHeight) {
		initializeCanvas(environmentWidth, environmentHeight);
		initializeUI(environmentWidth, environmentHeight);
		initializePlayPauseButtonFunctionality();
		initializeAddingFoodFunctionality();
		initializeLoadFunctionality();
		initializeSaveFunctionality();
		displayUI();
		enableControls();
	}
}
