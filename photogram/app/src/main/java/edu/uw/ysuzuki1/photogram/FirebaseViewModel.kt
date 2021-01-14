package edu.uw.ysuzuki1.photogram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map

/**
 * Extends ViewModel class to reference authentication state
 */
class FirebaseViewModel : ViewModel() {

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    /**
     * Returns authentication state
     */
    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }
}

