package com.tsourcecode.wiki.lib.domain.navigation

import com.tsourcecode.wiki.lib.domain.AppNavigator
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.net.URI

class UriNavigationTest {
    @Test
    fun `PROJECTS screen navigation`() {
        assertEquals(
                NavigationScreen.PROJECTS,
                NavigationScreen.resolveScreen(AppNavigator.PROJECTS_URI)
        )
    }

    @Test
    @Ignore("Currently disabled!")
    fun `FILE_MANAGER screen navigation`() {
        assertEquals(
                NavigationScreen.FILE_MANAGER,
                resolveScreen("open://notes/README.md")
        )
    }

    @Test
    @Ignore("Currently disabled!")
    fun `EDITOR screen navigation`() {
        assertEquals(
                NavigationScreen.EDITOR,
                resolveScreen("edit://notes/README.md")
        )
    }

    @Test
    fun `CHANGES screen navigation`() {
        assertEquals(
                NavigationScreen.CHANGES,
                resolveScreen("settings://changes")
        )
    }

    @Test
    @Ignore("Currently disabled!")
    fun `SEARCH screen navigation`() {
        assertEquals(
                NavigationScreen.SEARCH,
                resolveScreen("settings://search/notes")
        )
    }

    private fun resolveScreen(uriString: String) = NavigationScreen.resolveScreen(URI(uriString))
}