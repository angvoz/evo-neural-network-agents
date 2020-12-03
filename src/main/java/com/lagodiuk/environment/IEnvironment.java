package com.lagodiuk.environment;

import java.util.List;

import com.lagodiuk.agent.AbstractAgent;
import com.lagodiuk.agent.FertileAgent;
import com.lagodiuk.agent.IFood;

public interface IEnvironment {
	int getWidth();
	int getHeight();
	double getTime();

	double squareOfDistance(AbstractAgent agent1, AbstractAgent agent2);

	public void addEnergyReserve(int energy);

	public void initialize(int agentsDensity, int foodDensity);

	void addAgent(AbstractAgent agent);
	void removeAgent(AbstractAgent agent);
	List<AbstractAgent> getAgents();

	List<IFood> getFood();
	List<FertileAgent> getFishes();

	void timeStep();
}
