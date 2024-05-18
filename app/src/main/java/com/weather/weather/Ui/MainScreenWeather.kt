package com.weather.weather.Ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.weather.Backend.WeatherApiBaseClass
import com.weather.weather.Controller
import com.weather.weather.DaysOfTheWeek
import com.weather.weather.Months
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MainScreenWeather(
    controller:Controller,
    ){
    private val controller:Controller
    private val currentDay = mutableStateOf<WeatherApiBaseClass.HourForecast?>(null)
    init{
        this.controller = controller
    }
    fun onForecastChange(){
        currentDay.value = if(controller.getHourlyForecast().size==0) null else{controller.getHourlyForecast().first()}
    }
    fun resetForecast(){
        currentDay.value = null
    }
    private fun formatTime(epochSeconds: Long): String{
        val dt = LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC)
        return dt.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }
    @Composable
    fun Render(modifier: Modifier = Modifier) {
        val currentHourForecast = currentDay.value
        val currentDayForecast = controller.getDailyForecast() // Assuming this function exists

        if (currentHourForecast != null && currentDayForecast != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 38.dp, 0.dp, 0.dp)
                    .fillMaxHeight(0.4f)
            ) {
                AutoSizeText(
                    text = controller.getCity(),
                    textStyle = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${currentHourForecast.temperature}${controller.getWeatherMetrics().symbol}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 64.sp
                )
                Text(
                    text = "${stringResource(Months.entries[LocalDateTime.now().monthValue - 1].shortName)} ${currentHourForecast.dayOfMonth} ${stringResource(currentHourForecast.dayOfWeek.shortName)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = currentHourForecast.weatherCondition.value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text("Pressure: ${currentHourForecast.pressure} hPa")
                Text("Humidity: ${currentHourForecast.humidity}%")
                Text("Wind Speed: ${currentHourForecast.windSpeed} m/s")
//                Text("Sunrise: ${formatTime(currentDayForecast.first().sunrise)}")
//                Text("Sunset: ${formatTime(currentDayForecast.first().sunset)}")
                Spacer(Modifier.weight(3f))
            }
        }
    }

    //https://stackoverflow.com/questions/63971569/androidautosizetexttype-in-jetpack-compose
    //i would force it to dp but compose don't allow me :c
    @Composable
    private fun AutoSizeText(
        text: String,
        textStyle: TextStyle,
        modifier: Modifier = Modifier
    ) {
        var scaledTextStyle by remember { mutableStateOf(textStyle) }
        var readyToDraw by remember { mutableStateOf(false) }

        Text(
            text,
            modifier.drawWithContent {
                if (readyToDraw) {
                    drawContent()
                }
            },
            style = scaledTextStyle,
            softWrap = false,
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.didOverflowWidth) {
                    scaledTextStyle =
                        scaledTextStyle.copy(fontSize = scaledTextStyle.fontSize * 0.9)
                } else {
                    readyToDraw = true
                }
            }
        )
    }


}