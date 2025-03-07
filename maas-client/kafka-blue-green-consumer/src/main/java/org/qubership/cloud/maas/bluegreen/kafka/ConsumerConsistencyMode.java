package org.qubership.cloud.maas.bluegreen.kafka;

/**
 * Message consuming consistency mode enum.
 *
 * This enum controls message consuming offset manipulation strategies applied when promote/rollback application event arrive.
 */
public enum ConsumerConsistencyMode {
	/**
	 * No group offsets manipulation performed during promote/rollback application events. Messages can be missed during B/G promote operations
	 */
	EVENTUAL,

	/**
	 * On promote event rewind group offsets to avoid missing messages.
	 * After offset shifting, consumer may receive already consumed messages. So consumer routine should be implemented in reenterable manner
	 */
	GUARANTEE_CONSUMPTION
}
