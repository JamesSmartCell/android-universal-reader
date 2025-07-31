import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.remove

// At the top level of your Kotlin file:
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "login_credentials")

class UserPreferencesRepository(private val context: Context) {
    companion object {
        val USERNAME_KEY = stringPreferencesKey("username")
        val PASSWORD_KEY = stringPreferencesKey("password") // Caution with storing raw passwords
        // Consider storing a session token or API key received AFTER login instead of the password.
    }

    val usernameFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USERNAME_KEY]
        }

    val passwordFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PASSWORD_KEY]
        }

    suspend fun saveCredentials(username: String, password: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
            preferences[PASSWORD_KEY] = password // Re-iterate caution
        }
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { preferences ->
            preferences.remove(USERNAME_KEY)
            preferences.remove(PASSWORD_KEY)
        }
    }
}