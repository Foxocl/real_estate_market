package by.kotlin.salesAppartment

import android.content.Context

class NominatimRepository(
    private val dao: GeocodeDao,
    private val api: NominatimApiService,
    private val context: Context
) {
    suspend fun getCoordinates(address: String): Pair<Double, Double>? {
        val normalized = address.trim().lowercase()
        // 1. Cached?
        dao.getByAddress(normalized)?.let {
            return it.latitude to it.longitude
        }

        // 2. If internet, call API
        if (isNetworkAvailable()) {
            return try {
                val response = api.search(address)
                val first = response.firstOrNull()
                if (first != null) {
                    val lat = first.lat.toDoubleOrNull()
                    val lon = first.lon.toDoubleOrNull()
                    if (lat != null && lon != null) {
                        dao.insert(CachedGeocode(normalized, lat, lon))
                        return lat to lon
                    }
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        return null
    }

    private fun isNetworkAvailable(): Boolean {
        return NetworkMonitor(context).isConnected()
    }
}