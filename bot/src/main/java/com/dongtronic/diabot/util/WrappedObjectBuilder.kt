package com.dongtronic.diabot.util

/**
 * A 'wrapper' for builder objects. This class will automatically create new builders once the current in-use builder
 * exceeds a certain length.
 *
 * @param B Builder type
 * @param T Built object type
 * @property builder The builder to start with
 * @property duplicateBuilder Duplication function for a builder
 * @property buildObject Function which builds an object using the builder
 * @property isUnderLimit Checks if a builder is under the limitations. If this returns false, then the builder will be
 * wrapped.
 * @property prepForWrap Prepares a given builder to be wrapped. This should clear any existing data that should not
 * be brought to the next wrapped builder.
 * @property stack The stack of built objects
 */
class WrappedObjectBuilder<B, T>(
        var builder: B,
        val duplicateBuilder: (builder: B) -> B,
        val buildObject: (builder: B) -> T,
        val isUnderLimit: (oldBuilder: B, newBuilder: B) -> Boolean,
        val prepForWrap: (builder: B) -> B
) {
    private val stack = mutableListOf<T>()

    /**
     * Applies a modification to the builder.
     *
     * @param modification Function which applies a modification to a builder and returns the new builder
     * @return this [WrappedObjectBuilder] instance
     */
    fun modify(modification: (B) -> B) = apply {
        var output = modification(duplicateBuilder(builder))

        if (!isUnderLimit(duplicateBuilder(builder), output)) {
            val preppedBuilder = prepForWrap(duplicateBuilder(builder))
            output = modification(preppedBuilder)
            stack.add(buildObject(builder))
        }

        builder = output
    }

    /**
     * Applies a modification to the builder without requiring returning the builder object. This is useful when builder
     * objects do not create new instances of itself for each modification.
     *
     * @param modification Function which applies a modification to a builder
     * @return this [WrappedObjectBuilder] instance
     */
    fun modifyInPlace(modification: (B) -> Unit) = modify { it.apply { modification(it) } }

    /**
     * Gets a list of all built (and possibly wrapped) objects.
     *
     * @return List of built objects
     */
    fun build(): List<T> {
        return stack.plus(buildObject(builder))
    }

    /**
     * Gets the current size of the object stack.
     *
     * @return Size of object stack
     */
    fun getStackSize(): Int = stack.size
}