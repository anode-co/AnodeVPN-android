package com.pkt.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import com.pkt.core.extensions.getColorByAttribute

object IntentUtil {

    fun openWebUrl(context: Context, url: String) {
        val uri = Uri.parse(if (url.startsWith("http://") || url.startsWith("https://")) url else "http://$url")
        if (getCustomTabsPackages(context).isEmpty()) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } else {
            CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(context.getColorByAttribute(android.R.attr.colorBackground)).build()
                )
                .build()
                .launchUrl(context, uri)
        }
    }

    private fun getCustomTabsPackages(context: Context): List<ResolveInfo> {
        val pm = context.packageManager
        // Get default VIEW intent handler.
        val activityIntent = Intent()
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.fromParts("http", "", null))

        // Get all apps that can handle VIEW intents.
        val resolvedActivityList = pm.queryIntentActivities(activityIntent, 0)
        val packagesSupportingCustomTabs = mutableListOf<ResolveInfo>()
        for (info in resolvedActivityList) {
            val serviceIntent = Intent()
            serviceIntent.action = CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
            serviceIntent.setPackage(info.activityInfo.packageName)
            // Check if this package also resolves the Custom Tabs service.
            if (pm.resolveService(serviceIntent, 0) != null) {
                packagesSupportingCustomTabs.add(info)
            }
        }
        return packagesSupportingCustomTabs
    }
}
