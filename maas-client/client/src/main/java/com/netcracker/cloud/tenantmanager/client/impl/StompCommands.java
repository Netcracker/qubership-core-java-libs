package com.netcracker.cloud.tenantmanager.client.impl;

public enum StompCommands {
	CONNECT,

	// Client commands.
	SUBSCRIBE,
	UNSUBSCRIBE,
	DISCONNECT,

	// Server commands.
	MESSAGE,
	RECEIPT,
	ERROR,

	CONNECTED
}
