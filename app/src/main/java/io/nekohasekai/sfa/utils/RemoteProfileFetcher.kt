package io.nekohasekai.sfa.utils

import android.util.AtomicFile
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.database.Profile
import io.nekohasekai.sfa.database.ProfileManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.Date

object RemoteProfileFetcher {
    const val TEMPLATE_PATH = "/storage/emulated/0/Android/data/io.nekohasekai.sfa/files/template.json"
    const val MIHOMO_USER_AGENT = "clash-verge/v2.4.0"

    // Serialize creation and updates so overlapping requests use the persisted ID.
    private val updateMutex = Mutex()

    internal fun cacheIdFromConfig(content: String): String? =
        (JSONObject(content).optJSONObject("experimental")
            ?.optJSONObject("cache_file")
            ?.opt("cache_id") as? String)?.takeIf { it.isNotEmpty() }

    internal fun conversionTemplate(content: String, cacheId: String): String {
        if (cacheId.isNotEmpty()) return content
        val template = JSONObject(content)
        // A shared template must not give different subscriptions the same ID.
        template.optJSONObject("experimental")
            ?.optJSONObject("cache_file")
            ?.put("cache_id", "")
        return template.toString()
    }

    private fun fetch(profile: Profile): String {
        val typed = profile.typed
        if (!typed.mihomo) {
            return HTTPClient().use { it.getString(typed.remoteURL) }
        }
        require(!typed.userAgent.contains('\r') && !typed.userAgent.contains('\n')) {
            Application.application.getString(R.string.profile_user_agent_invalid)
        }
        check(SubscriptionConverter.available) {
            Application.application.getString(R.string.profile_mihomo_unavailable)
        }
        val template = File(TEMPLATE_PATH)
        check(template.isFile && template.canRead()) {
            Application.application.getString(R.string.profile_template_missing, TEMPLATE_PATH)
        }
        val cacheId = profile.cacheId.orEmpty()
        return SubscriptionConverter.convert(
            typed.remoteURL,
            conversionTemplate(template.readText(), cacheId),
            typed.userAgent.ifBlank { MIHOMO_USER_AGENT },
            typed.insecure,
            cacheId,
        )
    }

    suspend fun create(profile: Profile): Profile = updateMutex.withLock {
        val content = fetch(profile)
        Libbox.checkConfig(content)
        save(profile, content, create = true)
        profile
    }

    suspend fun update(profile: Profile): Boolean = updateMutex.withLock {
        val current = checkNotNull(ProfileManager.get(profile.id)) { "Profile no longer exists" }
        val content = fetch(current)
        Libbox.checkConfig(content)
        val changed = save(current, content, create = false)
        profile.cacheId = current.cacheId
        profile.typed = current.typed
        changed
    }

    private fun write(file: AtomicFile, content: ByteArray) {
        val stream = file.startWrite()
        try {
            stream.write(content)
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
    }

    private suspend fun save(profile: Profile, content: String, create: Boolean): Boolean {
        val file = AtomicFile(File(profile.typed.path))
        val previousContent = if (file.baseFile.exists()) file.readFully() else null
        val bytes = content.toByteArray(Charsets.UTF_8)
        val changed = previousContent == null || !previousContent.contentEquals(bytes)
        val previousId = profile.cacheId
        val previousDate = profile.typed.lastUpdated
        val nextId = if (profile.typed.mihomo) cacheIdFromConfig(content) ?: previousId else previousId
        if (changed) write(file, bytes)
        try {
            profile.cacheId = nextId
            profile.typed.lastUpdated = Date()
            if (create) {
                ProfileManager.create(profile, andSelect = true)
            } else {
                check(ProfileManager.update(profile) == 1) { "Profile no longer exists" }
            }
        } catch (e: Exception) {
            profile.cacheId = previousId
            profile.typed.lastUpdated = previousDate
            withContext(NonCancellable) {
                try {
                    if (changed) {
                        if (previousContent == null) file.delete() else write(file, previousContent)
                    }
                } catch (rollback: Exception) {
                    e.addSuppressed(rollback)
                }
            }
            throw e
        }
        return changed
    }
}
