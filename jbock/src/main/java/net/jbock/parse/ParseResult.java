package net.jbock.parse;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class represents the result of a successful parsing operation.
 *
 * @param <T> type of option keys
 */
public interface ParseResult<T> {

    /**
     * Get all option values for the given option key,
     * in the order in which they were passed on the command line.
     *
     * <p>For a non-repeatable option, the stream will not contain more than
     * one element.
     *
     * <p>For a required option, the stream will contain exactly one element.
     *
     * @param optionKey a key that represents a named option
     * @return stream of tokens
     */
    Stream<String> option(T optionKey);

    /**
     * Get the positional parameter at the given index.
     *
     * <p>Named options do not have an index. They must be retrieved via
     * {@linkplain #option(Object)}.
     *
     * <p>If the index is negative, or if there is no positional parameter at this index,
     * an empty {@code Optional} is returned.
     *
     * @param index a number
     * @return the positional parameter at the given index
     */
    Optional<String> param(int index);

    /**
     * Returns the remaining tokens.
     *
     * <p>This stream may be nonempty if there is a {@linkplain net.jbock.VarargsParameter},
     * or if the command is a {@linkplain net.jbock.SuperCommand}.
     * Otherwise it will be an empty stream.
     *
     * @return remaining tokens
     */
    Stream<String> rest();
}
