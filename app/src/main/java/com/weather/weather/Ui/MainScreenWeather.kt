package com.weather.weather.Ui

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnitType.Companion.Sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.weather.Backend.WeatherApiBaseClass
import com.weather.weather.Controller
import com.weather.weather.DaysOfTheWeek
import com.weather.weather.Months
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainScreenWeather(
    controller:Controller,
    ) {
    private val controller: Controller
    private val currentDay = mutableStateOf<WeatherApiBaseClass.HourForecast?>(null)

    init {
        this.controller = controller
    }

    fun onForecastChange() {
        currentDay.value = if (controller.getHourlyForecast().size == 0) null else {
            controller.getHourlyForecast().first()
        }
    }

    fun resetForecast() {
        currentDay.value = null
    }

    private fun formatTime(epochSeconds: Long): String {
        val dt = LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC)
        return dt.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }

    @Composable
    fun Render(modifier: Modifier = Modifier) {
        var searchQuery by remember { mutableStateOf("") }
        val currentHourForecast = currentDay.value
        val currentDayForecast = controller.getDailyForecast()
        var errorMessage by remember { mutableStateOf("") }
        var showErrorMessage by remember { mutableStateOf(false) }

        if (currentHourForecast != null && currentDayForecast != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 38.dp, 0.dp, 0.dp)
                    .fillMaxHeight(0.4f)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search City") },
                    placeholder = { Text("Enter city name") },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            GlobalScope.launch {
                                if (searchQuery.trim().isEmpty()) {
                                    errorMessage = "The specified city cannot be empty."
                                    showErrorMessage = true
//                                    Log.d("CitySearch", "Search query is empty.")
                                } else {
                                    val resultCode = controller.setCity(searchQuery)
                                    if (resultCode == -1) {
                                        errorMessage = "The specified city does not exist."
//                                        Log.d("CitySearch", errorMessage)
                                        showErrorMessage = true
                                    } else {
                                        showErrorMessage = false
                                    }
                                }
                            }
                        }
                    )
                )

                // Display error message if needed
                if (showErrorMessage && errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = TextStyle(fontSize = 12.sp),
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                AutoSizeText(
                    text = controller.getCity(),
                    textStyle = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.weight(1f))

                Text(
                    text = "${currentHourForecast.temperature}${controller.getWeatherMetrics().symbol} | ${currentHourForecast.weatherCondition.value}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                )
                Spacer(Modifier.weight(1f))

                Text(
                    text = "${stringResource(Months.entries[LocalDateTime.now().monthValue - 1].shortName)} ${currentHourForecast.dayOfMonth} ${
                        stringResource(
                            currentHourForecast.dayOfWeek.shortName
                        )
                    }",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(Modifier.height(16.dp))
//                Text("Pressure: ${currentHourForecast.pressure} hPa")
//                Text("\uD83D\uDCA7 Humidity: ${currentHourForecast.humidity}%")
//                Text("\uD83D\uDCA8 Wind Speed: ${currentHourForecast.windSpeed} m/s")
//                Text("\uD83C\uDF05 Sunrise: ${formatTime1(currentDayForecast.first().sunrise)}")
//                Text("\uD83C\uDF04 Sunset: ${formatTime1(currentDayForecast.first().sunset)}")

                WeatherCard(currentHourForecast)

                Spacer(Modifier.weight(3f))
            }
        }
    }

    private fun formatTime1(epochSeconds: Long): String {
        val instant = Instant.ofEpochSecond(epochSeconds)
        val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(zonedDateTime)
    }


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

    @Composable
    fun WeatherCard(weather: WeatherApiBaseClass.HourForecast) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Card for Humidity
            SquareStatCard(
                title = "Humidity",
                data = "${weather.humidity}%",
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(16.dp)) // Space between the cards

            // Card for Pressure
            SquareStatCard(
                title = "Pressure",
                data = "${weather.pressure} hPa",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // Card for Wind Speed
            SquareStatCard(
                title = "Wind",
                data = "${weather.windSpeed} m/s",
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(16.dp)) // Space between the cards

            SquareStatCard(
                title = "Pressure",
                data = "${weather.pressure} hPa",
                modifier = Modifier.weight(1f)
            )
        }
    }

    @Composable
    fun SquareStatCard(title: String, data: String, modifier: Modifier) {
        Card(
            modifier = modifier
                .aspectRatio(1f)
                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start, // Ensures the text alignment is to the left
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = data,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}