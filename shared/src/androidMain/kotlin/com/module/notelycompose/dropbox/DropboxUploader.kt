package com.module.notelycompose.dropbox

import android.app.Activity
import android.content.Context
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Holds the current Activity so the Dropbox OAuth flow can be launched. */
object ActivityHolder {
    @Volatile var activity: Activity? = null
}

/**
 * Dropbox upload via the official SDK PKCE flow (no secret in the APK).
 * App key comes from the manifest meta-data "dropbox_app_key" (local.properties
 * dropbox.app.key -> manifest placeholder).
 */
class DropboxUploader(private val context: Context) {

    private val prefs get() =
        context.getSharedPreferences("dropbox_prefs", Context.MODE_PRIVATE)

    val isLinked: Boolean get() = !prefs.getString(KEY_CRED, null).isNullOrBlank()

    fun requestLink(activity: Activity) {
        val appKey = appKey()
        val config = DbxRequestConfig(CLIENT_ID)
        Auth.startOAuth2PKCE(
            activity, appKey, config,
            listOf("files.content.write", "files.content.read", "account_info.read")
        )
    }

    /** Call from the host Activity onResume; returns true when a link landed. */
    fun captureAuthResult(): Boolean {
        val credential = Auth.getDbxCredential() ?: return false
        prefs.edit().putString(KEY_CRED, DbxCredential.Writer.writeToString(credential)).apply()
        return true
    }

    fun unlink() = prefs.edit().remove(KEY_CRED).apply()

    suspend fun uploadText(remotePath: String, content: String): String =
        withContext(Dispatchers.IO) {
            val stored = prefs.getString(KEY_CRED, null)
                ?: throw IllegalStateException("Dropbox not linked")
            val credential = DbxCredential.Reader.readFully(stored)
            content.byteInputStream().use { input ->
                val meta = DbxClientV2(DbxRequestConfig(CLIENT_ID), credential)
                    .files()
                    .uploadBuilder(remotePath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(input)
                meta.pathLower ?: remotePath
            }
        }

    private fun appKey(): String {
        val ai = context.packageManager.getApplicationInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        return ai.metaData?.getString("dropbox_app_key").orEmpty()
    }

    companion object {
        private const val KEY_CRED = "credential"
        private const val CLIENT_ID = "notelyvoice-sermon/1.0"
        const val REMOTE_DIR = "/sermon-notes"
    }
}