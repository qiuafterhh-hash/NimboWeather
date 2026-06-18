package com.nimboweather.forecast.data

/** Popular world cities shown on the add-city page. */
object PopularCities {
    val list: List<GeoLocation> = listOf(
        GeoLocation("New York", 40.7128, -74.0060, "US"),
        GeoLocation("Los Angeles", 34.0522, -118.2437, "US"),
        GeoLocation("London", 51.5074, -0.1278, "GB"),
        GeoLocation("Paris", 48.8566, 2.3522, "FR"),
        GeoLocation("Berlin", 52.5200, 13.4050, "DE"),
        GeoLocation("Tokyo", 35.6762, 139.6503, "JP"),
        GeoLocation("Singapore", 1.3521, 103.8198, "SG"),
        GeoLocation("Hong Kong", 22.3193, 114.1694, "HK"),
        GeoLocation("Sydney", -33.8688, 151.2093, "AU"),
        GeoLocation("Dubai", 25.2048, 55.2708, "AE"),
        GeoLocation("Toronto", 43.6532, -79.3832, "CA"),
        GeoLocation("Moscow", 55.7558, 37.6173, "RU"),
        GeoLocation("São Paulo", -23.5505, -46.6333, "BR"),
        GeoLocation("Mumbai", 19.0760, 72.8777, "IN")
    )
}
