package com.weather.weather.Backend

import android.util.Log
import com.weather.weather.DaysOfTheWeek
import com.weather.weather.TemperatureSymbols
import com.weather.weather.WeatherCondition
import com.weather.weather.WeatherErrors
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.TimeZone
import java.util.concurrent.TimeUnit
//TODO find if rly needed to respond on errors
class OpenMeteoApi(settingsData:SettingsData, previousResponse : ResponseRaw? = null)
    : WeatherApiBaseClass(null,settingsData) {
    private var responseRaw: ResponseRaw? = null
    init{
        this.responseRaw = previousResponse
    }
    override fun start(forceCache:Boolean){
        val httpUrl:String = "https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&hourly=temperature_2m,weather_code"
        GlobalScope.launch { start(httpUrl,responseRaw,forceCache) }
    }
    companion object{
        suspend fun getLatLong(city: String,weatherApiKey: String?):LatNLong?{
            return _getLatLong(city, weatherApiKey)?.get(0)
        }
        suspend fun getLatLong(city: String,weatherApiKey: String?,length: Int):Array<LatNLong>?{
            return _getLatLong(city, weatherApiKey, length)
        }
        private suspend fun _getLatLong(city: String,weatherApiKey: String? = null,length:Int = 1):Array<LatNLong>?{
            try{
                val httpClient:OkHttpClient = OkHttpClient().newBuilder()
                    .callTimeout(10,TimeUnit.SECONDS)
                    .readTimeout(10,TimeUnit.SECONDS)
                    .build()
                val httpUrl:String = "https://geocoding-api.open-meteo.com/v1/search?name=${city}&count=${length}&language=en&format=json"
                val request = Request.Builder().url(httpUrl).build()
                val response:String = httpClient.newCall(request).execute().use {
                    it.body!!.string()
                }
                val parsedResponse:JsonObject = Json.parseToJsonElement(response).jsonObject
                val parsedResults:MutableList<LatNLong> = mutableListOf()
                val resultsOccurrence:JsonArray = parsedResponse["results"]!!.jsonArray
                resultsOccurrence.forEach{
                    val resultObject:JsonObject = it.jsonObject;
                    val latitude:Float = resultObject["latitude"]!!.jsonPrimitive.float
                    val longitude:Float = resultObject["longitude"]!!.jsonPrimitive.float
                    val findedCity:String = resultObject["name"]!!.jsonPrimitive.content
                    parsedResults.add(
                        LatNLong(
                        latitude = latitude,
                        longitude = longitude,
                        city = findedCity
                    ))
                }
                return parsedResults.toTypedArray()
            }catch (e:Exception){
                Log.e("weatherError",e.stackTraceToString())
                // TODO catch in static
            }
            return null
        }

    }
    override fun gRawResponse():ResponseRaw?{
        return responseRaw
    }
    protected override fun processData(responseRaw: ResponseRaw){
        dailyForecast.clear()
        hourlyForecast.clear()
        val parsedResponse:JsonObject = Json.parseToJsonElement(responseRaw.rawResponse).jsonObject
        try{
            var mostlyWeatherIs:IntArray = IntArray(WeatherCondition.entries.size)
            val hourly:JsonObject = parsedResponse["hourly"]!!.jsonObject
            val hourlyTemperature:JsonArray = hourly["temperature_2m"]!!.jsonArray
            val hourlyWeatherCodes:JsonArray = hourly["weather_code"]!!.jsonArray
            val hourlyTime:JsonArray = hourly["time"]!!.jsonArray
            for (i in hourlyTemperature.indices){
                val timeZone:ZonedDateTime = LocalDateTime.parse(hourlyTime[i].jsonPrimitive.content).toInstant(ZoneOffset.UTC).atZone(TimeZone.getDefault().toZoneId())
                if((timeZone.hour != LocalDateTime.now().hour &&
                    timeZone.dayOfMonth < LocalDateTime.now().dayOfMonth) &&
                    hourlyForecast.size == 0){
                    continue
                }
                val hourWeather: HourForecast = HourForecast(
                    temperature = when(temperatureSymbol){
                        TemperatureSymbols.CELSIUS -> hourlyTemperature[i].jsonPrimitive.float
                        TemperatureSymbols.FAHRENHEIT -> celsiusToFahrenheit(hourlyTemperature[i].jsonPrimitive.float)
                    } ,
                    weatherCondition = formatWeatherCode(hourlyWeatherCodes[i].jsonPrimitive.int),
                    hour = timeZone.hour,
                    dayOfMonth = timeZone.dayOfMonth,
                    dayOfWeek =  DaysOfTheWeek.entries[timeZone.dayOfWeek.value-1],
                    pressure = -1,
                    humidity = -1,
                    windSpeed = -1f,
                )
                hourlyForecast.add(hourWeather)
                if(dailyForecast.size == 0 || dailyForecast.last().dayOfMonth != hourWeather.dayOfMonth ){
                    mostlyWeatherIs = IntArray(WeatherCondition.entries.size)
                    val dayWeather:DailyForecast = DailyForecast(
                        minTemperature = hourWeather.temperature,
                        maxTemperature = hourWeather.temperature,
                        condition = hourWeather.weatherCondition,
                        dayOfMonth = hourWeather.dayOfMonth,
                        dayOfWeek = hourWeather.dayOfWeek,
                        sunrise = 1,
                        sunset = 1
                    )
                    mostlyWeatherIs[hourWeather.weatherCondition.ordinal] += 1
                    dailyForecast.add(dayWeather)
                }
                else{
                    if(dailyForecast.last().minTemperature > hourWeather.temperature){
                        dailyForecast.last().minTemperature = hourWeather.temperature
                    }
                    if(dailyForecast.last().maxTemperature < hourWeather.temperature){
                        dailyForecast.last().maxTemperature = hourWeather.temperature
                    }
                    mostlyWeatherIs[hourWeather.weatherCondition.ordinal] += 1
                    dailyForecast.last().condition = WeatherCondition.entries[mostlyWeatherIs.indexOf(mostlyWeatherIs.max())]
                }
            }
            this.responseRaw = responseRaw
            notifyListeners()
        }catch (e:Exception){
            //???
            notifyErrorListeners(WeatherErrors.Unknown)
            Log.e("weatherError",e.stackTraceToString())
        }
    }
    private fun formatWeatherCode(code:Int):WeatherCondition{
        return when(code){
            0 -> WeatherCondition.CLEAR
            in (1..2) -> WeatherCondition.PARTLYCLOUDY
            3 -> WeatherCondition.CLOUDY
            in (4..67) -> WeatherCondition.RAIN
            in (68..86) -> WeatherCondition.SNOW
            in (95..99) -> WeatherCondition.THUNDERSTORM
            else -> WeatherCondition.CLEAR
        }
    }

}