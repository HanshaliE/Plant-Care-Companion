package com.example.plantcarecompanion.ui.addplant

import com.example.plantcarecompanion.domain.LookupError

// Turns a LookupError into words the user can understand
fun LookupError.userMessage(): String = when (this) {
    LookupError.Offline -> "You appear to be offline."
    LookupError.Timeout -> "The plant database took too long to respond."
    LookupError.Network -> "Couldn't reach the plant database."
    is LookupError.Server -> "The plant database had a problem (error $code)."
    LookupError.NotFound -> "The plant database has no care details for this plant."
    is LookupError.RateLimited -> "Live search is paused to stay within the free daily limit. It will work again later."
    // friendly wording: this is not something the user did wrong, it is just a setup state on this device
    LookupError.MissingKey -> "Live plant search isn't set up on this device."
    LookupError.InvalidKey -> "The Perenual API key was rejected."
    LookupError.BadResponse -> "The plant database sent something unexpected."
}

// "Soft" notices are expected situations (no key, offline, daily limit reached).
// The screens show them in a calm neutral colour instead of the red error colour.
val LookupError.isSoftNotice: Boolean
    get() = when (this) {
        LookupError.MissingKey, LookupError.Offline, LookupError.NotFound, is LookupError.RateLimited -> true
        else -> false
    }
