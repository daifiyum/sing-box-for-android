package io.nekohasekai.sfa.compose.screen.configuration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.utils.RemoteProfileFetcher
import io.nekohasekai.sfa.utils.SubscriptionConverter

@Composable
fun RemoteSubscriptionOptions(
    mihomo: Boolean,
    insecure: Boolean,
    userAgent: String,
    onMihomoChange: (Boolean) -> Unit,
    onInsecureChange: (Boolean) -> Unit,
    onUserAgentChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.profile_mihomo), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = mihomo,
                onCheckedChange = onMihomoChange,
                enabled = SubscriptionConverter.available,
            )
        }
        if (mihomo) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.profile_insecure), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = insecure, onCheckedChange = onInsecureChange)
            }
            OutlinedTextField(
                value = userAgent,
                onValueChange = onUserAgentChange,
                label = { Text(stringResource(R.string.profile_user_agent)) },
                supportingText = {
                    Text(
                        stringResource(R.string.profile_user_agent_hint, RemoteProfileFetcher.MIHOMO_USER_AGENT),
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
