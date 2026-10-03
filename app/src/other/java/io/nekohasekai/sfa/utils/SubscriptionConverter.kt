package io.nekohasekai.sfa.utils

import android.os.Build
import io.nekohasekai.mobile.Mobile

object SubscriptionConverter {
    val available: Boolean get() = Build.SUPPORTED_ABIS.contains("arm64-v8a")

    fun convert(url: String, template: String, userAgent: String, insecure: Boolean): String =
        Mobile.convertURL(url, template, userAgent, "", if (insecure) "true" else "preserve")
}
