package com.nimboweather.forecast.data.weathermap

/**
 * Pure tile-URL builders. The map view supplies z/x/y per visible tile. Note Esri uses z/y/x
 * order (unlike the XYZ standard), so it cannot use osmdroid's plain XYTileSource.
 */
object WeatherTiles {

    fun owmUrl(layer: String, z: Int, x: Int, y: Int, key: String): String =
        "https://tile.openweathermap.org/map/$layer/$z/$x/$y.png?appid=$key"

    fun esriUrl(z: Int, x: Int, y: Int, token: String): String =
        "https://ibasemaps-api.arcgis.com/arcgis/rest/services/World_Topo_Map/" +
            "MapServer/tile/$z/$y/$x?token=$token"

    fun nexradUrl(z: Int, x: Int, y: Int): String =
        "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/" +
            "nexrad-n0q-900913/$z/$x/$y.png"
}
