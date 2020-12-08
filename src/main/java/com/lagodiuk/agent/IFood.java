package com.lagodiuk.agent;

import com.lagodiuk.environment.IEnvironment;

public interface IFood extends IAgent {
	public IFood reproduce(IEnvironment env);
}
