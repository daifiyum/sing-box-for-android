package io.nekohasekai.sfa.utils

import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.database.TypedProfile
import java.io.File

object RemoteProfileFetcher {
    const val TEMPLATE_PATH = "/storage/emulated/0/Android/data/io.nekohasekai.sfa/files/template.json"
    const val MIHOMO_USER_AGENT = "clash-verge/v2.4.0"

    fun fetch(profile: TypedProfile): String {
        if (!profile.mihomo) {
            return HTTPClient().use { it.getString(profile.remoteURL) }
        }
        require(!profile.userAgent.contains('\r') && !profile.userAgent.contains('\n')) {
            Application.application.getString(R.string.profile_user_agent_invalid)
        }
        check(SubscriptionConverter.available) {
            Application.application.getString(R.string.profile_mihomo_unavailable)
        }
        val template = File(TEMPLATE_PATH)
        check(template.isFile && template.canRead()) {
            Application.application.getString(R.string.profile_template_missing, TEMPLATE_PATH)
        }
        return SubscriptionConverter.convert(
            profile.remoteURL,
            template.readText(),
            profile.userAgent.ifBlank { MIHOMO_USER_AGENT },
            profile.insecure,
        )
    }
}
