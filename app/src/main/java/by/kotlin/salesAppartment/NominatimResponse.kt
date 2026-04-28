package by.kotlin.salesAppartment

data class NominatimResponse(
    val place_id: Long,
    val lat: String,
    val lon: String,
    val display_name: String,
    val `class`: String?,
    val type: String?
)