package com.weather.weather.Ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.weather.weather.Backend.WeatherApiBaseClass
import com.weather.weather.Controller
import com.weather.weather.DaysOfTheWeek
import com.weather.weather.TemperatureSymbols
import com.weather.weather.WeatherCondition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class DaysWeatherLong(controller: Controller) {
    private val controller:Controller
    private val forecast =  mutableStateListOf<WeatherApiBaseClass.DailyForecast>()

    init {
        this.controller = controller
    }
    fun onForecastChange(){
        forecast.clear()
        forecast += controller.getDailyForecast()
    }
    fun resetForecast(){
        forecast.clear()
    }
    @Composable
    fun Render(modifier: Modifier = Modifier){
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            forecast.forEach{it ->
                WeatherForDay(
                    day = it.dayOfWeek,
                    type = it.condition,
                    minTemperature = it.minTemperature,
                    maxTemperature = it.maxTemperature
                )
            }
        }
    }
    @Composable
    private fun WeatherForDay(
        day: DaysOfTheWeek = DaysOfTheWeek.MONDAY,
        type:WeatherCondition = WeatherCondition.CLEAR,
        minTemperature:Float = 0f, maxTemperature:Float = 0f
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(
                stringResource(day.shortName),
                modifier = Modifier.weight(0.4f)
            )
            Text(
                type.value,
                modifier = Modifier.weight(0.3f)
            )
            Text(
                "$minTemperature / $maxTemperature ${controller.getWeatherMetrics().symbol}",
                modifier = Modifier.weight(0.3f)
            )
        }

    }

}