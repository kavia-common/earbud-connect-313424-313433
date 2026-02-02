package org.example.app.ui.shared

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Collects a StateFlow with lifecycle awareness.
 */
fun <T> StateFlow<T>.collectWithLifecycle(owner: LifecycleOwner, collector: (T) -> Unit) {
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            this@collectWithLifecycle.collect { collector(it) }
        }
    }
}
