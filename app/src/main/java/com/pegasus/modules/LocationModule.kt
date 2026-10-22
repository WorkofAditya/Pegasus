
package com.pegasus.modules

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.pegasus.core.PegasusModule
import org.json.JSONObject

class LocationModule(private val context: Context) : PegasusModule {

    override val name = "location"

    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null

    @SuppressLint("MissingPermission")
    override fun execute(payload: JSONObject): JSONObject {
        locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val action = payload.optString("action", "once")
        val provider = payload.optString("provider", LocationManager.GPS_PROVIDER)

        return try {
            when (action) {
                "once" -> getLocationOnce(provider)
                "watch" -> startWatching(provider)
                "stop" -> stopWatching()
                else -> JSONObject().put("error", "Unknown location action")
            }
        } catch (e: SecurityException) {
            JSONObject().put("error", "Location permission not granted")
        } catch (e: Exception) {
            JSONObject().put("error", e.message ?: "Location operation failed")
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationOnce(provider: String): JSONObject {
        val location = locationManager.getLastKnownLocation(provider)
            ?: return JSONObject().put("error", "No last-known location available")

        return locationToJson(location)
    }

    @SuppressLint("MissingPermission")
    private fun startWatching(provider: String): JSONObject {
        stopWatching()

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Handle the location locally in your app.
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated by Android")
            override fun onStatusChanged(
                provider: String?,
                status: Int,
                extras: Bundle?
            ) {}
        }

        locationListener = listener
        locationManager.requestLocationUpdates(
            provider,
            5000L,
            10f,
            listener
        )

        return JSONObject().put("success", true)
    }

    private fun stopWatching(): JSONObject {
        return try {
            locationListener?.let { listener ->
                if (::locationManager.isInitialized) {
                    locationManager.removeUpdates(listener)
                }
            }
            locationListener = null
            JSONObject().put("success", true)
        } catch (e: Exception) {
            JSONObject().put("error", e.message ?: "Could not stop location updates")
        }
    }

    private fun locationToJson(location: Location): JSONObject {
        return JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("accuracy", location.accuracy)
            put("altitude", location.altitude)
            put("speed", location.speed)
            put("bearing", location.bearing)
            put("time", location.time)
        }
    }
}
