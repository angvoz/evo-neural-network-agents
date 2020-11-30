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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;

public class TestMovingAgent {
	final static double DELTA_ZERO = 1E-10;

	final static double ANGLE_RIGHT = 0;
	final static double ANGLE_RIGHT_UP = Math.PI * 0.25;
	final static double ANGLE_UP = Math.PI * 0.5;
	final static double ANGLE_LEFT = Math.PI;
	final static double ANGLE_DOWN = Math.PI * 1.5;

	class MovingAgentMock extends MovingAgent {
		public MovingAgentMock(double x, double y, double angle, double speed) {
			super(x, y, angle, speed);
		}
		@Override
		public double getRadius() {
			return 5;
		}
	}

	@Test
	public void testBasic() {
		AgentsEnvironment env = new AgentsEnvironment(100, 200);
		MovingAgentMock agent = new MovingAgentMock(1.5, 2.5, ANGLE_RIGHT, 2);
		assertEquals(1.5, agent.getX(), DELTA_ZERO);
		assertEquals(2.5, agent.getY(), DELTA_ZERO);

		env.addAgent(agent);
		List<IAgent> agents = env.getAgents();
		assertEquals(1, agents.size());
		assertSame(agent, agents.get(0));

		agent.move(env);
		assertEquals(3.5, agent.getX(), DELTA_ZERO);
		assertEquals(2.5, agent.getY(), DELTA_ZERO);

		env.removeAgent(agent);
		assertEquals(0, env.getAgents().size());
	}

	@Test
	public void testBoundaries() {
		AgentsEnvironment env = new AgentsEnvironment(100, 200);
		{
			MovingAgentMock agent = new MovingAgentMock(99.0, 0.0, ANGLE_RIGHT, 1);
			env.addAgent(agent);
			agent.move(env);
			assertEquals(0.0, agent.getX(), DELTA_ZERO);
			assertEquals(0.0, agent.getY(), DELTA_ZERO);
			env.removeAgent(agent);
		}

		{
			MovingAgentMock agent = new MovingAgentMock(99.0, 199.0, ANGLE_RIGHT_UP, 2.0 / Math.cos(ANGLE_RIGHT_UP));
			env.addAgent(agent);
			agent.move(env);
			assertEquals(1.0, agent.getX(), DELTA_ZERO);
			assertEquals(1.0, agent.getY(), DELTA_ZERO);
			env.removeAgent(agent);
		}

		{
			MovingAgentMock agent = new MovingAgentMock(0.0, 199.0, ANGLE_UP, 1);
			env.addAgent(agent);
			agent.move(env);
			assertEquals(0.0, agent.getX(), DELTA_ZERO);
			assertEquals(0.0, agent.getY(), DELTA_ZERO);
			env.removeAgent(agent);
		}

		{
			MovingAgentMock agent = new MovingAgentMock(0.0, 0.0, ANGLE_LEFT, 1);
			env.addAgent(agent);
			agent.move(env);
			assertEquals(99.0, agent.getX(), DELTA_ZERO);
			assertEquals(0.0, agent.getY(), DELTA_ZERO);
			env.removeAgent(agent);
		}

		{
			MovingAgentMock agent = new MovingAgentMock(0.0, 0.0, ANGLE_DOWN, 1);
			env.addAgent(agent);
			agent.move(env);
			assertEquals(0.0, agent.getX(), DELTA_ZERO);
			assertEquals(199.0, agent.getY(), DELTA_ZERO);
			env.removeAgent(agent);
		}

		assertEquals(0, env.getAgents().size());
	}

}
