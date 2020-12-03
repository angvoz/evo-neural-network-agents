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
package com.lagodiuk.nn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.lagodiuk.agent.AbstractAgent;
import com.lagodiuk.agent.AgentsEnvironment;

public class TestNeuralNetworkDrivenAgent {
	final static double LITTLE_BIT = 1E-10;

	final static double ANGLE_RIGHT = 0;
	final static double ANGLE_RIGHT_UP = Math.PI * 0.25;
	final static double ANGLE_UP = Math.PI * 0.5;
	final static double ANGLE_LEFT = Math.PI;
	final static double ANGLE_DOWN = Math.PI * 1.5;

	final static double EYSIGHT_ANGLE = NeuralNetworkDrivenAgent.EYSIGHT_ANGLE;
	final static double EYESIGHT_DISTANCE = NeuralNetworkDrivenAgent.EYESIGHT_DISTANCE;

	class AgentMock extends AbstractAgent {
		public AgentMock(double x, double y) {
			super(x, y);
		}
		@Override
		public double getRadius() {
			return 1;
		}
	}

	class NeuralNetworkDrivenAgentMock extends NeuralNetworkDrivenAgent {
		public NeuralNetworkDrivenAgentMock(double x, double y, double angle, double speed) {
			super(x, y, angle, speed);
		}
	}

	@Test
	public void testInSight_angles() {
		AgentsEnvironment env = new AgentsEnvironment(200, 200);
		NeuralNetworkDrivenAgentMock agent = new NeuralNetworkDrivenAgentMock(100.0, 100.0, ANGLE_RIGHT, 1);
		env.addAgent(agent);

		{
//			double angle = 0;
			AgentMock target = new AgentMock(101.0, 100.0);
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = 10.0;
			AgentMock target = new AgentMock(100.0 + dist * Math.cos(angle), 100.0 + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = EYSIGHT_ANGLE + LITTLE_BIT;
			double dist = 10.0;
			AgentMock target = new AgentMock(100.0 + dist * Math.cos(angle), 100.0 + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
//			double angle = Math.PI/2;
			AgentMock target = new AgentMock(100.0, 101.0);
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
//			double angle = Math.PI;
			AgentMock target = new AgentMock(99.0, 100.0);
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
//			double angle = -Math.PI/2;
			AgentMock target = new AgentMock(100.0, 99.0);
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = -(EYSIGHT_ANGLE + LITTLE_BIT);
			double dist = 10.0;
			AgentMock target = new AgentMock(100.0 + dist * Math.cos(angle), 100.0 + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = -(EYSIGHT_ANGLE - LITTLE_BIT);
			double dist = 10.0;
			AgentMock target = new AgentMock(100.0 + dist * Math.cos(angle), 100.0 + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		env.removeAgent(agent);
		assertEquals(0, env.getAgents().size());
	}

	@Test
	public void testInSight_distance() {
		AgentsEnvironment env = new AgentsEnvironment(200, 200);
		NeuralNetworkDrivenAgentMock agent = new NeuralNetworkDrivenAgentMock(100.0, 100.0, ANGLE_RIGHT, 1);
		env.addAgent(agent);

		// distance < eyesight
		{
			AgentMock target = new AgentMock(100.0 + EYESIGHT_DISTANCE - LITTLE_BIT, 100.0);
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		// distance > eyesight
		{
			AgentMock target = new AgentMock(100.0 + EYESIGHT_DISTANCE + LITTLE_BIT, 100.0);
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		// distance < eyesight
		{
			double angle = Math.PI / 8.0;
			AgentMock target = new AgentMock(100.0 + (EYESIGHT_DISTANCE - LITTLE_BIT) * Math.cos(angle),
					100.0 + EYESIGHT_DISTANCE * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		// distance > eyesight
		{
			double angle = Math.PI / 8.0;
			AgentMock target = new AgentMock(100.0 + (EYESIGHT_DISTANCE + LITTLE_BIT) * Math.cos(angle),
					100.0 + EYESIGHT_DISTANCE * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		env.removeAgent(agent);
		assertEquals(0, env.getAgents().size());
	}

	@Test
	public void testInSightBoundary_fromBottomRight_toRight() {
		final int XMAX = 200;
		final int YMAX = 300;
		AgentsEnvironment env = new AgentsEnvironment(XMAX, YMAX);
		NeuralNetworkDrivenAgentMock agent = new NeuralNetworkDrivenAgentMock(XMAX, 0.0, ANGLE_RIGHT, 1);
		env.addAgent(agent);

		final double XTBASE = 0.0;
		final double YTBASE = 0.0;
		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() - (EYSIGHT_ANGLE - LITTLE_BIT);
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE + LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		env.removeAgent(agent);
		assertEquals(0, env.getAgents().size());
	}

	@Test
	public void testInSightBoundary_fromBottomRight_toBottom() {
		final int XMAX = 200;
		final int YMAX = 300;
		AgentsEnvironment env = new AgentsEnvironment(XMAX, YMAX);
		NeuralNetworkDrivenAgentMock agent = new NeuralNetworkDrivenAgentMock(XMAX, 0.0, ANGLE_DOWN, 1);
		env.addAgent(agent);

		final double XTBASE = XMAX;
		final double YTBASE = YMAX;
		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() - (EYSIGHT_ANGLE - LITTLE_BIT);
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE + LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		env.removeAgent(agent);
		assertEquals(0, env.getAgents().size());
	}

	@Test
	public void testInSightBoundary_fromBottomLeft_toBottom() {
		final int XMAX = 200;
		final int YMAX = 300;
		AgentsEnvironment env = new AgentsEnvironment(XMAX, YMAX);
		NeuralNetworkDrivenAgentMock agent = new NeuralNetworkDrivenAgentMock(0.0, 0.0, ANGLE_DOWN, 1);
		env.addAgent(agent);

		final double XTBASE = 0.0;
		final double YTBASE = YMAX;
		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() - (EYSIGHT_ANGLE - LITTLE_BIT);
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE + LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		env.removeAgent(agent);
		assertEquals(0, env.getAgents().size());
	}

	@Test
	public void testInSightBoundary_fromBottomLeft_toLeft() {
		final int XMAX = 200;
		final int YMAX = 300;
		AgentsEnvironment env = new AgentsEnvironment(XMAX, YMAX);
		NeuralNetworkDrivenAgentMock agent = new NeuralNetworkDrivenAgentMock(0.0, 0.0, ANGLE_LEFT, 1);
		env.addAgent(agent);

		final double XTBASE = XMAX;
		final double YTBASE = 0.0;
		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() - (EYSIGHT_ANGLE - LITTLE_BIT);
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE + LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		env.removeAgent(agent);
		assertEquals(0, env.getAgents().size());
	}

	@Test
	public void testInSightBoundary_fromTopLeft_toLeft() {
		final int XMAX = 200;
		final int YMAX = 300;
		AgentsEnvironment env = new AgentsEnvironment(XMAX, YMAX);
		NeuralNetworkDrivenAgentMock agent = new NeuralNetworkDrivenAgentMock(0.0, YMAX, ANGLE_LEFT, 1);
		env.addAgent(agent);

		final double XTBASE = XMAX;
		final double YTBASE = YMAX;
		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() - (EYSIGHT_ANGLE - LITTLE_BIT);
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE + LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		env.removeAgent(agent);
		assertEquals(0, env.getAgents().size());
	}

	@Test
	public void testInSightBoundary_fromTopLeft_toUp() {
		final int XMAX = 200;
		final int YMAX = 300;
		AgentsEnvironment env = new AgentsEnvironment(XMAX, YMAX);
		NeuralNetworkDrivenAgentMock agent = new NeuralNetworkDrivenAgentMock(0.0, YMAX, ANGLE_UP, 1);
		env.addAgent(agent);

		final double XTBASE = 0.0;
		final double YTBASE = 0.0;
		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() - (EYSIGHT_ANGLE - LITTLE_BIT);
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE + LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		env.removeAgent(agent);
		assertEquals(0, env.getAgents().size());
	}

	@Test
	public void testInSightBoundary_fromTopRight_toUp() {
		final int XMAX = 200;
		final int YMAX = 300;
		AgentsEnvironment env = new AgentsEnvironment(XMAX, YMAX);
		NeuralNetworkDrivenAgentMock agent = new NeuralNetworkDrivenAgentMock(XMAX, YMAX, ANGLE_UP, 1);
		env.addAgent(agent);

		final double XTBASE = XMAX;
		final double YTBASE = 0.0;
		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() - (EYSIGHT_ANGLE - LITTLE_BIT);
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE + LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		env.removeAgent(agent);
		assertEquals(0, env.getAgents().size());
	}

	@Test
	public void testInSightBoundary_fromTopRight_toRight() {
		final int XMAX = 200;
		final int YMAX = 300;
		AgentsEnvironment env = new AgentsEnvironment(XMAX, YMAX);
		NeuralNetworkDrivenAgentMock agent = new NeuralNetworkDrivenAgentMock(XMAX, YMAX, ANGLE_RIGHT, 1);
		env.addAgent(agent);

		final double XTBASE = 0.0;
		final double YTBASE = YMAX;
		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() - (EYSIGHT_ANGLE - LITTLE_BIT);
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE + LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		env.removeAgent(agent);
		assertEquals(0, env.getAgents().size());
	}

	@Test
	public void testInSightBoundary_fromTopRight_toRightUp() {
		final int XMAX = 200;
		final int YMAX = 300;
		AgentsEnvironment env = new AgentsEnvironment(XMAX, YMAX);
		NeuralNetworkDrivenAgentMock agent = new NeuralNetworkDrivenAgentMock(XMAX, YMAX, ANGLE_RIGHT_UP, 1);
		env.addAgent(agent);

		final double XTBASE = 0.0;
		final double YTBASE = YMAX;
		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() - (EYSIGHT_ANGLE - LITTLE_BIT);
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE + LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle() + EYSIGHT_ANGLE - LITTLE_BIT;
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE - LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertTrue(agent.inSight(target, env));
			env.removeAgent(target);
		}

		{
			double angle = agent.getAngle();
			double dist = EYESIGHT_DISTANCE + LITTLE_BIT;
			AgentMock target = new AgentMock(XTBASE + dist * Math.cos(angle), YTBASE + dist * Math.sin(angle));
			env.addAgent(target);
			assertFalse(agent.inSight(target, env));
			env.removeAgent(target);
		}

		env.removeAgent(agent);
		assertEquals(0, env.getAgents().size());
	}
}
