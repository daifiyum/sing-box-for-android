package io.nekohasekai.sfa.compose.screen.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.nekohasekai.sfa.compose.screen.profile.EditProfileContentScreen

@Composable
fun SubscriptionTemplateEditorScreen(navController: NavController) {
    EditProfileContentScreen(
        profileId = -1L,
        onNavigateBack = { navController.navigateUp() },
        isTemplate = true,
    )
}
