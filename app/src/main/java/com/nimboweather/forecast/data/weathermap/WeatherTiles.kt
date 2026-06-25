package com.nimboweather.forecast.data.weathermap

/**
 * Pure tile-URL builders. The map view supplies z/x/y per visible tile. Note Esri's Static
 * Basemap Tiles service uses level/row/column = z/y/x order (unlike the XYZ standard) and serves
 * 512px PNGs, so it needs a custom source (not osmdroid's plain XYTileSource).
 */
object WeatherTiles {

    fun owmUrl(layer: String, z: Int, x: Int, y: Int, key: String): String =
        "https://tile.openweathermap.org/map/$layer/$z/$x/$y.png?appid=$key"

    // Esri ArcGIS Static Basemap Tiles service (the post-2026-06 replacement for the retired
    // ibasemaps MapServer endpoint). `arcgis/light-gray` is a neutral canvas so the (semi-
    // transparent) OWM weather overlay reads clearly on top. 512px tiles.
    fun esriUrl(z: Int, x: Int, y: Int, token: String): String =
        "https://static-map-tiles-api.arcgis.com/arcgis/rest/services/" +
            "static-basemap-tiles-service/v1/arcgis/light-gray/static/tile/$z/$y/$x?token=$token"

    fun nexradUrl(z: Int, x: Int, y: Int): String =
        "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/" +
            "nexrad-n0q-900913/$z/$x/$y.png"
}
