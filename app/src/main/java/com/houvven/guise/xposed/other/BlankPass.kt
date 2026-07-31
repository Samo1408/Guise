package com.houvven.guise.xposed.other

import android.content.ContentResolver
import android.content.ContentProviderClient
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.beforeHookAllMethods

class BlankPass : LoadPackageHandler {

    override fun onHook() {
        if (!config.passAudio && !config.passVideo && !config.passPhoto && !config.passContacts) return

        hookQueries(ContentResolver::class.java)
        hookQueries(ContentProviderClient::class.java)
    }

    private fun hookQueries(clazz: Class<*>) {
        clazz.beforeHookAllMethods("query") { param ->
            val uri = param.args.firstOrNull { it is Uri } as? Uri
                ?: return@beforeHookAllMethods
            if (!shouldReturnEmpty(uri)) return@beforeHookAllMethods

            val columns = (param.args.firstOrNull { it is Array<*> } as? Array<*>)
                ?.map(Any?::toString)
                ?.toTypedArray()
                ?: emptyArray()
            param.result = MatrixCursor(columns)
        }
    }

    private fun shouldReturnEmpty(uri: Uri): Boolean {
        if (config.passContacts && uri.authority == ContactsContract.AUTHORITY) {
            return uri.pathSegments.firstOrNull() == ContactsContract.Contacts.CONTENT_URI.pathSegments.firstOrNull()
        }
        if (uri.authority != "media") return false

        val segments = uri.pathSegments.map(String::lowercase)
        return when {
            config.passPhoto && "images" in segments -> true
            config.passVideo && "video" in segments -> true
            config.passAudio && "audio" in segments -> true
            else -> false
        }
    }

}
