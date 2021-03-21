package com.dongtronic.diabot.commands

/**
 * Mapper for [ResponseLevel] enums
 */
interface ResponseLevelMapper {
    /**
     * Maps a [ResponseLevel] to an output prefix.
     *
     * @param responseLevel The response level
     * @param includeSpace Include a space at the end of the output
     * @return Prefix for the given response level
     */
    fun getResponseIndicator(responseLevel: ResponseLevel, includeSpace: Boolean = true): String
}