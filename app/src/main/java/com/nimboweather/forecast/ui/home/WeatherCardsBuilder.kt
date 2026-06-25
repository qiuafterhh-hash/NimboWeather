package com.nimboweather.forecast.ui.home

import android.content.Context
import com.nimboweather.forecast.data.AirPollutionResponse
import com.nimboweather.forecast.data.AirQualityIndex
import com.nimboweather.forecast.data.CurrentWeather
import com.nimboweather.forecast.data.MoonPhase
import com.nimboweather.forecast.data.Nowcast as NowcastLogic
import com.nimboweather.forecast.data.nowcast.NowcastResult
import com.nimboweather.forecast.data.DailyForecast
import com.nimboweather.forecast.data.ForecastResponse
import com.nimboweather.forecast.data.HourlyForecast
import com.nimboweather.forecast.data.WeatherCache
import com.nimboweather.forecast.data.WeatherSnapshot
import com.nimboweather.forecast.R
import com.nimboweather.forecast.config.CardLayoutConfig
import com.nimboweather.forecast.prefs.UnitsStore
import com.nimboweather.forecast.ui.detail.DetailHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

/** Turns a (current + forecast) response into the ordered home card list. Shared
 *  by every city page. Also updates the detail holder + widget/notification cache. */
class WeatherCardsBuilder(private val context: Context) {

    private val unitsStore = UnitsStore(context)
    private val dateIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayOut = SimpleDateFormat("EEE", Locale.US)

    fun build(cur: CurrentWeather, fc: ForecastResponse, place: String, air: AirPollutionResponse? = null, nowcast: NowcastResult? = null): List<HomeCard> {
        val sym = unitsStore.tempSymbol()
        val hourlyAll = mapHourly(fc, Int.MAX_VALUE)
        val daily = mapDaily(fc)
        val precip = mapPrecip(fc)

        DetailHolder.place = place
        DetailHolder.hourly = hourlyAll
        DetailHolder.daily = daily

        val today = daily.firstOrNull()
        val temp = cur.main?.temp?.roundToInt() ?: 0

        WeatherCache(context).save(
            WeatherSnapshot(
                city = place,
                temp = temp,
                symbol = sym,
                condition = cur.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                icon = cur.weather.firstOrNull()?.icon
            )
        )

        return CardLayoutConfig.order().mapNotNull { type ->
            when (type) {
                HomeCardType.CURRENT -> HomeCard.Current(
                    place = place,
                    temp = temp,
                    symbol = sym,
                    desc = cur.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                    icon = cur.weather.firstOrNull()?.icon,
                    feels = cur.main?.feelsLike?.roundToInt() ?: temp,
                    max = today?.max ?: temp,
                    min = today?.min ?: temp,
                    rainProb = precip.firstOrNull()?.pop ?: 0,
                    pressure = cur.main?.pressure ?: 0,
                    windText = "${degToCompass(cur.wind?.deg)} · ${(cur.wind?.speed ?: 0.0).roundToInt()} ${unitsStore.speedSymbol()}",
                    windDeg = cur.wind?.deg,
                    windSpeed = (cur.wind?.speed ?: 0.0).toFloat()
                )
                HomeCardType.NOWCAST -> nowcast?.takeIf { it.series.isNotEmpty() }?.let {
                    HomeCard.Nowcast(NowcastLogic.headline(it.state), it.series)
                }
                HomeCardType.HOURLY -> hourlyAll.take(8).takeIf { it.isNotEmpty() }?.let { HomeCard.Hourly(it) }
                HomeCardType.PRECIP -> precip.takeIf { it.isNotEmpty() }?.let { HomeCard.Precip(it) }
                HomeCardType.DETAILS -> HomeCard.Details(buildMetrics(cur, sym, air))
                HomeCardType.SUNRISE_SUNSET -> cur.sys?.takeIf { it.sunrise > 0 }?.let {
                    HomeCard.SunriseSunset(fmtTime(it.sunrise, cur.timezone), fmtTime(it.sunset, cur.timezone))
                }
                HomeCardType.DAILY -> daily.takeIf { it.isNotEmpty() }?.let { HomeCard.Daily(it) }
            }
        }
    }

    private fun degToCompass(deg: Int?): String {
        if (deg == null) return "--"
        val dirs = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        return dirs[((deg / 22.5).roundToInt() % 16 + 16) % 16]
    }

    private fun buildMetrics(cur: CurrentWeather, sym: String, air: AirPollutionResponse?): List<Metric> {
        val list = mutableListOf<Metric>()
        air?.list?.firstOrNull()?.components?.pm25?.let { pm ->
            val aqi = AirQualityIndex.usAqiFromPm25(pm)
            list.add(Metric("Air quality", aqi.toString(), R.drawable.ic_aqi, AirQualityIndex.category(aqi)))
        }
        val now = System.currentTimeMillis()
        list.add(Metric("Moon", MoonPhase.phaseName(now), R.drawable.ic_moon, "${(MoonPhase.illumination(now) * 100).roundToInt()}% lit"))
        cur.sys?.takeIf { it.sunrise > 0 }?.let {
            list.add(Metric("Sunrise", fmtTime(it.sunrise, cur.timezone), R.drawable.ic_sunrise, "Sunset ${fmtTime(it.sunset, cur.timezone)}"))
        }
        cur.main?.feelsLike?.let { list.add(Metric("Feels like", "${it.roundToInt()}$sym", R.drawable.ic_feels)) }
        cur.main?.humidity?.let { list.add(Metric("Humidity", "$it%", R.drawable.ic_humidity)) }
        cur.main?.pressure?.let { list.add(Metric("Pressure", "$it hPa", R.drawable.ic_pressure)) }
        cur.wind?.speed?.let { list.add(Metric("Wind", "${it.roundToInt()} ${unitsStore.speedSymbol()}", R.drawable.ic_wind)) }
        cur.visibility?.let { list.add(Metric("Visibility", "${it / 1000} km", R.drawable.ic_visibility)) }
        cur.clouds?.all?.let { list.add(Metric("Cloudiness", "$it%", R.drawable.ic_cloud)) }
        return list
    }

    private fun mapPrecip(fc: ForecastResponse): List<PrecipPoint> =
        fc.list.take(6).map {
            PrecipPoint(
                time = it.dtTxt?.let { t -> if (t.length >= 16) t.substring(11, 16) else t } ?: "--",
                pop = (it.pop * 100).roundToInt()
            )
        }

    private fun mapHourly(fc: ForecastResponse, limit: Int): List<HourlyForecast> =
        fc.list.take(limit).map { item ->
            HourlyForecast(
                timeLabel = item.dtTxt?.let { if (it.length >= 16) it.substring(11, 16) else it } ?: "--",
                temp = item.main?.temp?.roundToInt() ?: 0,
                icon = item.weather.firstOrNull()?.icon
            )
        }

    private fun mapDaily(fc: ForecastResponse): List<DailyForecast> {
        val byDate = fc.list
            .groupBy { it.dtTxt?.substringBefore(' ') ?: "" }
            .filterKeys { it.isNotEmpty() }
        return byDate.entries.take(6).map { (date, items) ->
            val temps = items.mapNotNull { it.main?.temp }
            val midday = items.minByOrNull { abs((it.dtTxt?.substring(11, 13)?.toIntOrNull() ?: 0) - 12) } ?: items.first()
            DailyForecast(
                dayLabel = runCatching { dayOut.format(dateIn.parse(date)!!) }.getOrDefault(date),
                min = (temps.minOrNull() ?: 0.0).roundToInt(),
                max = (temps.maxOrNull() ?: 0.0).roundToInt(),
                icon = midday.weather.firstOrNull()?.icon,
                desc = midday.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() }
            )
        }
    }

    private fun fmtTime(unixUtc: Long, tzOffsetSec: Int): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date((unixUtc + tzOffsetSec) * 1000L))
    }
}
