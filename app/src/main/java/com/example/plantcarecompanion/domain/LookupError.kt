package com.example.plantcarecompanion.domain

sealed interface LookupError {
    data object Offline : LookupError

    data object Timeout : LookupError

    data object Network : LookupError

    data class Server(val code: Int) : LookupError

    data class RateLimited(val reopensAtMillis: Long) : LookupError

    data object MissingKey : LookupError

    data object InvalidKey : LookupError

    data object BadResponse : LookupError
}

val LookupError.isTransient: Boolean
    get() = when (this) {
        LookupError.Timeout, LookupError.Network, is LookupError.Server -> true
        LookupError.Offline -> false          // retrying instantly won't help; user taps Retry once back online
        is LookupError.RateLimited, LookupError.MissingKey, LookupError.InvalidKey, LookupError.BadResponse -> false
    }

val LookupError.canRetry: Boolean
    get() = when (this) {
        LookupError.Offline, LookupError.Timeout, LookupError.Network, is LookupError.Server, LookupError.BadResponse -> true
        is LookupError.RateLimited, LookupError.MissingKey, LookupError.InvalidKey -> false
    }

data class SearchOutcome(
    val species: List<Species>,
    val error: LookupError? = null
)

data class CareInfoOutcome(
    val species: Species,
    val error: LookupError? = null
)
