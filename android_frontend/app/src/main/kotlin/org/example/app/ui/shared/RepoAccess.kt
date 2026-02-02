package org.example.app.ui.shared

import org.example.app.MainActivity
import org.example.app.repo.AppRepository
import java.lang.reflect.Field

object RepoAccess {

    // PUBLIC_INTERFACE
    /**
     * Access the MainActivity repository instance without exposing it publicly in the Activity API.
     * Uses reflection only within app module for simplicity.
     */
    fun repo(activity: MainActivity): AppRepository {
        val field: Field = activity.javaClass.getDeclaredField("repository")
        field.isAccessible = true
        return field.get(activity) as AppRepository
    }
}
