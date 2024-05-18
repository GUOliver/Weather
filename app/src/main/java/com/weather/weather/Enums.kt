package com.weather.weather

enum class TemperatureSymbols(val symbol:String){
    CELSIUS("°C"),
    FAHRENHEIT("°F")
}
enum class DaysOfTheWeek(val shortName: Int){
    MONDAY(R.string.monday),
    TUESDAY(R.string.tuesday),
    WEDNESDAY(R.string.wednesday),
    THURSDAY(R.string.thursday),
    FRIDAY(R.string.friday),
    SATURDAY(R.string.saturday),
    SUNDAY(R.string.sunday)
}
enum class Months(val shortName: Int){
    JANUARY(R.string.january),
    FEBRUARY(R.string.february),
    MARCH(R.string.march),
    APRIL(R.string.april),
    MAY(R.string.may),
    JUNE(R.string.june),
    JULY(R.string.july),
    AUGUST(R.string.august),
    SEPTEMBER(R.string.september),
    OCTOBER(R.string.october),
    NOVEMBER(R.string.november),
    DECEMBER(R.string.december)
}
enum class WeatherProviders(val site:String){
    OPENWEATHER("openweathermap.org"),
    OPENMETEO("open-meteo.com")
}
enum class WeatherCondition(val value:String){
    CLEAR("Clear"),
    RAIN("Rainy"),
    THUNDERSTORM("Thunder"),
    SNOW("Snow"),
    PARTLYCLOUDY("Cloudy"),
    CLOUDY("Cloudy")
}
enum class WeatherErrors(val message:String = ""){
    UnknownHost("Either site is down or you are offline"),
    IOError("Either site is down or response timeout was reached"),
    CacheForceLoadFailed("Loading previous forecast from cache failed"),
    ApiKeyInvalid("Invalid api key, change key in settings"),
    Unknown("Something that we couldn't catch happened,logs are printed")
}