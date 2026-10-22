package com.pegasus.modules

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import com.pegasus.core.PegasusModule
import org.json.JSONObject

class PackageManagerModule(private val context: Context) : PegasusModule {
    override val name = "package"

    override fun execute(payload: JSONObject): JSONObject {
        val action = payload.optString("action")
        val packageName = payload.optString("package_name")
        val apkUri = payload.optString("apk_uri")

        return when (action) {
            "list" -> listPackages()
            "info" -> getPackageInfo(packageName)
            "install" -> installPackage(apkUri)
            "uninstall" -> uninstallPackage(packageName)
            "launch" -> launchPackage(packageName)
            "force_stop" -> JSONObject().put("error", "Android does not allow ordinary apps to force-stop other packages")
            else -> JSONObject().put("error", "Unknown package action")
        }
    }

    private fun listPackages(): JSONObject {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        val apps = org.json.JSONArray()
        packages.filter { it.applicationInfo != null }.forEach { pkg ->
            apps.put(JSONObject().apply {
                put("package_name", pkg.packageName)
                put("version_name", pkg.versionName)
                put("version_code", pkg.longVersionCode)
                put("system_app", ((pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM) != 0)
            })
        }
        return JSONObject().put("packages", apps)
    }

    private fun getPackageInfo(packageName: String): JSONObject {
        return try {
            val info = context.packageManager.getPackageInfo(packageName, 0)
            JSONObject().apply {
                put("package_name", info.packageName)
                put("version_name", info.versionName)
                put("version_code", info.longVersionCode)
                put("first_install_time", info.firstInstallTime)
                put("last_update_time", info.lastUpdateTime)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            JSONObject().put("error", "Package not found")
        }
    }

    private fun installPackage(apkUri: String): JSONObject {
        return try {
            val uri = Uri.parse(apkUri)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            JSONObject().put("success", true).put("message", "Install confirmation opened")
        } catch (e: Exception) {
            JSONObject().put("error", e.message ?: "Unable to open installer")
        }
    }

    private fun uninstallPackage(packageName: String): JSONObject {
        return try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            JSONObject().put("success", true).put("message", "Uninstall confirmation opened")
        } catch (e: Exception) {
            JSONObject().put("error", e.message ?: "Unable to open uninstaller")
        }
    }

    private fun launchPackage(packageName: String): JSONObject {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                JSONObject().put("success", true)
            } else {
                JSONObject().put("error", "No launch intent found")
            }
        } catch (e: Exception) {
            JSONObject().put("error", e.message ?: "Unable to launch app")
        }
    }
}
