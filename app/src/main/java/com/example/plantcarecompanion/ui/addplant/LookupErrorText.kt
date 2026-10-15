package com.example.plantcarecompanion.ui.addplant

import com.example.plantcarecompanion.domain.LookupError

/** One place that turns a [LookupError] into words for the user. `when` is exhaustive (sealed). */
fun LookupError.userMessage(): String = when (this) {
    LookupError.Offline -> "You appear to be offline."
    LookupError.Timeout -> "The plant database took too long to respond."
    LookupError.Network -> "Couldn't reach the plant database."
    is LookupError.Server -> "The plant database had a problem (error $code)."
    is LookupError.RateLimited -> "Live search is paused to stay within the free daily limit. It will work again later."
    LookupError.MissingKey -> "No Perenual API key is set up, so live search is off."
    LookupError.InvalidKey -> "The Perenual API key was rejected."
    LookupError.BadResponse -> "The plant database sent something unexpected."
}
