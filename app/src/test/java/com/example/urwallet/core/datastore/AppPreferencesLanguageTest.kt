package com.example.urwallet.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.urwallet.core.common.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AppPreferencesLanguageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun createPreferences(): AppPreferences {
        val testFile = tempFolder.newFile("test_preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        return AppPreferences(dataStore)
    }

    @Test
    fun `default language should be English`() = testScope.runTest {
        val preferences = createPreferences()
        val language = preferences.appLanguage.first()
        assertEquals(Constants.DEFAULT_LANGUAGE, language)
        assertEquals(Constants.LANGUAGE_ENGLISH, language)
    }

    @Test
    fun `setting language to Arabic updates flow value`() = testScope.runTest {
        val preferences = createPreferences()
        preferences.setAppLanguage(Constants.LANGUAGE_ARABIC)
        val language = preferences.appLanguage.first()
        assertEquals(Constants.LANGUAGE_ARABIC, language)
    }

    @Test
    fun `setting language to English updates flow value`() = testScope.runTest {
        val preferences = createPreferences()
        preferences.setAppLanguage(Constants.LANGUAGE_ARABIC)
        assertEquals(Constants.LANGUAGE_ARABIC, preferences.appLanguage.first())

        preferences.setAppLanguage(Constants.LANGUAGE_ENGLISH)
        assertEquals(Constants.LANGUAGE_ENGLISH, preferences.appLanguage.first())
    }
}
