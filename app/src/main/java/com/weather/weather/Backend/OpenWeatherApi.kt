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
import kotlinx.serialization.json.long
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.ZonedDateTime
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class OpenWeatherApi(weatherApiKey:String, settingsData:SettingsData, previousResponse : ResponseRaw? = null)
    : WeatherApiBaseClass(weatherApiKey,settingsData) {
    private var responseRaw: ResponseRaw? = null
    init{
        this.responseRaw = previousResponse
    }
    override fun start(forceCache: Boolean){
        val httpUrl:String = "https://api.openweathermap.org/data/2.5/forecast?lat=${latitude}&lon=${longitude}&appid=${weatherApiKey}"
        GlobalScope.launch { start(httpUrl,responseRaw,forceCache) }
    }

    companion object{
        suspend fun getLatLong(city: String,weatherApiKey: String?):LatNLong?{
            if (_getLatLong(city, weatherApiKey)?.isEmpty() == true) {
                return null
            }
            return _getLatLong(city,weatherApiKey)?.get(0)
        }
        suspend fun getLatLong(city: String,weatherApiKey: String?,length: Int):Array<LatNLong>?{
            return _getLatLong(city,weatherApiKey,length)
        }
        private suspend fun _getLatLong(city: String,weatherApiKey: String? = null,length:Int = 1):Array<LatNLong>?{
            try{
                val httpClient:OkHttpClient = OkHttpClient().newBuilder()
                    .callTimeout(10,TimeUnit.SECONDS)
                    .readTimeout(10,TimeUnit.SECONDS)
                    .build()
                val httpUrl:String = "https://api.openweathermap.org/geo/1.0/direct?q=${city}&limit=${length}&appid=${weatherApiKey}"
                val request = Request.Builder().url(httpUrl).build()
                val response:String = httpClient.newCall(request).execute().use {
                    it.body!!.string()
                }
                val parsedResponse:JsonArray = Json.parseToJsonElement(response).jsonArray
                val parsedResults:MutableList<LatNLong> = mutableListOf()
                parsedResponse.forEach{
                    val resultObject:JsonObject = it.jsonObject;
                    val latitude:Float = resultObject["lat"]!!.jsonPrimitive.float
                    val longitude:Float = resultObject["lon"]!!.jsonPrimitive.float
                    val findedCity:String = resultObject["name"]!!.jsonPrimitive.content
                    parsedResults.add(
                        LatNLong(
                            latitude = latitude,
                            longitude = longitude,
                            city = findedCity
                        ))
                }
                return parsedResults.toTypedArray()
            } catch (e:Exception){
                Log.e("weatherError",e.stackTraceToString())
                //it will be a huge pain to work with static
                //so i blame everything on 'unknown city'
                //TODO catch error in static?
            }
            return null
        }

    }

    override fun gRawResponse():ResponseRaw?{
        return responseRaw
    }

    protected override fun processData(responseRaw: ResponseRaw) {
        dailyForecast.clear()
        hourlyForecast.clear()

        val parsedResponse: JsonObject = Json.parseToJsonElement(responseRaw.rawResponse).jsonObject

        try {
            var mostlyWeatherIs: IntArray = IntArray(WeatherCondition.entries.size)
            val hourlyTemperature: JsonArray = parsedResponse["list"]!!.jsonArray

            // Assuming the city object contains the daily data including sunrise and sunset
            val cityInfo: JsonObject = parsedResponse["city"]!!.jsonObject
            val sunrise: Long = cityInfo["sunrise"]!!.jsonPrimitive.long
            val sunset: Long = cityInfo["sunset"]!!.jsonPrimitive.long

            hourlyTemperature.forEach {
                val jsonObject: JsonObject = it.jsonObject
                val mainObject: JsonObject = jsonObject["main"]!!.jsonObject
                val windObject: JsonObject = jsonObject["wind"]!!.jsonObject

                val timeZone: ZonedDateTime = Instant.ofEpochSecond(jsonObject["dt"]!!.jsonPrimitive.long)
                    .atZone(TimeZone.getDefault().toZoneId())
                val temperature = when (temperatureSymbol) {
                    TemperatureSymbols.CELSIUS -> kelvinToCelcius(mainObject["temp"]!!.jsonPrimitive.float)
                    TemperatureSymbols.FAHRENHEIT -> celsiusToFahrenheit(kelvinToCelcius(mainObject["temp"]!!.jsonPrimitive.float))
                    else -> mainObject["temp"]!!.jsonPrimitive.float  // Add default case for safety, even if unused
                }

                val feelLikeTemperature = when (temperatureSymbol) {
                    TemperatureSymbols.CELSIUS -> kelvinToCelcius(mainObject["feels_like"]!!.jsonPrimitive.float)
                    TemperatureSymbols.FAHRENHEIT -> celsiusToFahrenheit(kelvinToCelcius(mainObject["feels_like"]!!.jsonPrimitive.float))
                    else -> mainObject["feels_like"]!!.jsonPrimitive.float  // Add default case for safety, even if unused
                }

                val hourWeather = HourForecast(
                    temperature = temperature,  // Use the computed temperature here
                    weatherCondition = formatWeatherCode(jsonObject["weather"]!!.jsonArray[0].jsonObject["id"]!!.jsonPrimitive.int),
                    hour = timeZone.hour,
                    dayOfMonth = timeZone.dayOfMonth,
                    dayOfWeek = DaysOfTheWeek.entries[timeZone.dayOfWeek.value-1],
                    pressure = mainObject["pressure"]!!.jsonPrimitive.int,
                    humidity = mainObject["humidity"]!!.jsonPrimitive.int,
                    windSpeed = windObject["speed"]!!.jsonPrimitive.float,
                    feelLikeTemp = feelLikeTemperature
                )

                hourlyForecast.add(hourWeather)

                if (dailyForecast.isEmpty() || dailyForecast.last().dayOfMonth != hourWeather.dayOfMonth) {
                    val dayWeather = DailyForecast(
                        minTemperature = hourWeather.temperature,
                        maxTemperature = hourWeather.temperature,
                        condition = hourWeather.weatherCondition,
                        dayOfMonth = hourWeather.dayOfMonth,
                        dayOfWeek = hourWeather.dayOfWeek,
                        sunrise = sunrise,
                        sunset = sunset
                    )
                    mostlyWeatherIs[hourWeather.weatherCondition.ordinal] += 1
                    dailyForecast.add(dayWeather)
                } else {
                    val lastForecast = dailyForecast.last()
                    if (lastForecast.minTemperature > hourWeather.temperature) {
                        lastForecast.minTemperature = hourWeather.temperature
                    }
                    if (lastForecast.maxTemperature < hourWeather.temperature) {
                        lastForecast.maxTemperature = hourWeather.temperature
                    }
                    mostlyWeatherIs[hourWeather.weatherCondition.ordinal] += 1
                    lastForecast.condition = WeatherCondition.entries[mostlyWeatherIs.indexOf(mostlyWeatherIs.max())]
                }
            }

            this.responseRaw = responseRaw
            notifyListeners()
        } catch (e: Exception) {
            val error: WeatherErrors? = getErrorFromResponse(parsedResponse)
            if (error != null) {
                notifyErrorListeners(error)
            } else {
                notifyErrorListeners(WeatherErrors.Unknown)
                Log.e("weatherError", e.stackTraceToString())
            }
        }
    }


    private fun getErrorFromResponse(parsedResponse:JsonObject):WeatherErrors?{
        try {
            val code:Int =  parsedResponse["cod"]!!.jsonPrimitive.int
            if(code == 401){
                return WeatherErrors.ApiKeyInvalid
            } else {
                return WeatherErrors.Unknown
            }
        } catch (e:Exception){
            return null
        }
    }

    private fun formatWeatherCode(code:Int):WeatherCondition{
        return when(code){
            in (200..299) -> WeatherCondition.THUNDERSTORM
            in (300..599) -> WeatherCondition.RAIN
            in (600..699) -> WeatherCondition.SNOW
            in (700..800) -> WeatherCondition.CLEAR
            in (801..802) -> WeatherCondition.PARTLYCLOUDY
            in (803..810) -> WeatherCondition.CLOUDY
            else -> WeatherCondition.CLEAR
        }
    }

}