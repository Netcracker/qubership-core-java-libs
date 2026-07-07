package com.netcracker.cloud.maas.bluegreen.versiontracker.impl;

import com.netcracker.cloud.bluegreen.api.model.BlueGreenState;
import com.netcracker.cloud.bluegreen.api.model.NamespaceVersion;
import com.netcracker.cloud.bluegreen.api.model.State;
import com.netcracker.cloud.bluegreen.api.model.Version;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Builds the predicate a blue-green consumer uses to decide whether to process a message, based on the
 * current {@link BlueGreenState}. Messages are routed by a header value; two flavours are supported:
 * <ul>
 *     <li>{@link #constructVersionFilter} &mdash; routes by the {@code X-Version} header (version number);</li>
 *     <li>{@link #constructVersionNameFilter} &mdash; routes by the {@code X-Version-Name} header (state name).</li>
 * </ul>
 *
 * <p>Both flavours share the same routing rules, derived from the current namespace state and its sibling:
 * <ul>
 *     <li>no sibling, or sibling is {@link State#IDLE} &rarr; accept every message (no rollout in progress);</li>
 *     <li>current is {@link State#ACTIVE} &rarr; accept everything <em>except</em> the sibling's value
 *         (that traffic belongs to the candidate/legacy sibling);</li>
 *     <li>current is {@link State#CANDIDATE} or {@link State#LEGACY} &rarr; accept <em>only</em> its own value.</li>
 * </ul>
 */
public class VersionFilterConstructor {

    /**
     * Builds a filter over the {@code X-Version} header value (the version number, e.g. {@code v2}).
     * Comparison is numeric: both the header and the target are parsed into a {@link Version}, so e.g.
     * {@code 4} and {@code v4} match. The returned predicate throws if the header holds an unparseable version.
     *
     * @param bgState current blue-green state
     * @return predicate over the {@code X-Version} header value
     */
    public static Predicate<String> constructVersionFilter(BlueGreenState bgState) {
        return constructFilter(bgState, NamespaceVersion::getVersion, Version::new, Version::equals);
    }

    /**
     * Builds a filter over the {@code X-Version-Name} header value (the BG state name: {@code active}/{@code candidate}/{@code legacy}).
     * Mirrors {@link #constructVersionFilter} but matches by state name instead of version number, so it works
     * regardless of the numeric version assigned to the sibling/current namespace. Matching is case-insensitive.
     *
     * @param bgState current blue-green state
     * @return predicate over the {@code X-Version-Name} header value}
     */
    public static Predicate<String> constructVersionNameFilter(BlueGreenState bgState) {
        return constructFilter(bgState, ns -> ns.getState().getName(), Function.identity(), String::equalsIgnoreCase);
    }

    /**
     * Common blue-green filter skeleton. Accepts everything when there is no active sibling; when current is
     * {@link State#ACTIVE} rejects only the sibling's value; when current is {@link State#CANDIDATE}/{@link State#LEGACY}
     * accepts only its own value. The {@code extractor}/{@code parser}/{@code eq} arguments carry the only logic that
     * differs between concrete filters.
     *
     * @param bgState   current blue-green state
     * @param extractor pulls the value to match from a namespace (its version or its state name)
     * @param parser    parses the raw header value into the same type before comparison
     * @param eq        equality between the parsed header value and the extracted target
     * @param <T>       type the header value and target are compared as (e.g. {@link Version} or {@link String})
     * @return a {@link PrintablePredicate} over the raw header value
     * @throws IllegalStateException if the current state is not ACTIVE, CANDIDATE or LEGACY
     */
    private static <T> Predicate<String> constructFilter(BlueGreenState bgState,
                                                         Function<NamespaceVersion, T> extractor,
                                                         Function<String, T> parser,
                                                         BiPredicate<T, T> eq) {
        NamespaceVersion current = bgState.getCurrent();
        Optional<NamespaceVersion> sibling = bgState.getSibling();
        if (sibling.isEmpty() || sibling.get().getState() == State.IDLE) {
            return new PrintablePredicate<>(v -> true, "true");
        }
        return switch (current.getState()) {
            case ACTIVE -> {
                T target = extractor.apply(sibling.get());
                yield new PrintablePredicate<>(v -> !eq.test(parser.apply(v), target), "!" + target);
            }
            case CANDIDATE, LEGACY -> {
                T target = extractor.apply(current);
                yield new PrintablePredicate<>(v -> eq.test(parser.apply(v), target), String.valueOf(target));
            }
            default -> throw new IllegalStateException("Invalid Blue Green State " + current.getState());
        };
    }
}
