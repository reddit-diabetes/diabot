package com.dongtronic.diabot.commands

/**
 * Command category used for organisation.
 *
 * @property displayName How the category's name should be displayed
 */
enum class Category(val displayName: String) {
    ADMIN("Admin"),
    BG("BG conversions"),
    A1C("A1c estimations"),
    FUN("Fun"),
    UTILITIES("Utilities"),
    INFO("Informative"),

    UNSPECIFIED("No category"),
}