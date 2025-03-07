package org.qubership.cloud.routesregistration.common.gateway.route;

import lombok.Getter;
import lombok.ToString;

/**
 * The class {@code ProgressiveTimeout} is used to
 * construct progressive timeouts with the specified
 * base timeout value, starting and ending multiplier value
 * and multiplier step. Instance of this class lets you
 * obtain next timeout value in progression by calling
 * {@link ProgressiveTimeout#nextTimeoutValue()} method.
 */
@ToString
public class ProgressiveTimeout {
    @Getter
    private final long baseTimeout;
    @Getter
    private final int startMultiplier;
    @Getter
    private final int endMultiplier;
    @Getter
    private final int multiplierStep;

    private int currentMultiplierValue;
    private final long maxTimeoutValue;

    /**
     * This object is used to synchronize inner logic of
     * the {@code ProgressiveTimeout} instances.
     */
    private final Object lock = new Object();

    /**
     * Constructs instance of the {@code ProgressiveTimeout} class.
     *
     * @param baseTimeout     base timeout value
     * @param startMultiplier starting multiplier value
     * @param endMultiplier   ending multiplier value
     * @param multiplierStep  value which will be added to multiplier
     *                        on each new iteration
     */
    public ProgressiveTimeout(long baseTimeout, int startMultiplier, int endMultiplier, int multiplierStep) {
        if (endMultiplier <= startMultiplier) {
            throw new IllegalArgumentException("endMultiplier must be greater than startMultiplier in ProgressiveTimeout!");
        }
        this.baseTimeout = baseTimeout;
        this.startMultiplier = startMultiplier;
        this.endMultiplier = endMultiplier;
        this.multiplierStep = multiplierStep;

        currentMultiplierValue = startMultiplier;
        maxTimeoutValue = endMultiplier * baseTimeout;
    }

    /**
     * Method to obtain next progressive timeout value. When this
     * value reaches {@code endMultiplier * baseTimeout}, it stays
     * the same until {@link ProgressiveTimeout#reset()} method is called.
     *
     * @return next progressive timeout value
     */
    public long nextTimeoutValue() {
        synchronized (lock) {
            if (currentMultiplierValue >= endMultiplier) return maxTimeoutValue;

            long result = currentMultiplierValue * baseTimeout;
            currentMultiplierValue += multiplierStep;
            return result;
        }
    }

    /**
     * Method to reset timeout to its starting value. After
     * calling {@code reset()} method next call of the
     * {@link ProgressiveTimeout#nextTimeoutValue()} will return
     * {@code baseTimeout * startMultiplier}.
     */
    public void reset() {
        synchronized (lock) {
            currentMultiplierValue = startMultiplier;
        }
    }
}
