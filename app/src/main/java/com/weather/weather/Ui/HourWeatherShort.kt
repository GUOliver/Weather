package com.weather.weather.Ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weather.weather.Backend.WeatherApiBaseClass
import com.weather.weather.Controller

class HourWeatherShort(controller: Controller) {
    private val controller:Controller
    private val forecast =  mutableStateListOf<WeatherApiBaseClass.HourForecast>()
    init {
        this.controller = controller
    }
    fun onForecastChange(){
        forecast.clear()
        val hourlyForecst = controller.getHourlyForecast()
        if(hourlyForecst.size < 25){
            forecast += hourlyForecst
        } else {
            forecast += hourlyForecst.slice(0..24)
        }

    }
    fun resetForecast(){
        forecast.clear()
    }
    @Composable
    fun Render(modifier: Modifier = Modifier) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(5.dp)
            ) {
                forecast.forEach{it ->
                    WeatherBox(
                        hour = it.hour,
                        type = it.weatherCondition.value,
                        temperature = it.temperature
                    )
                }
            }
            Divider(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(3.dp)
            )
        }
    }
    @Composable
    private  fun WeatherBox(hour:Int = 0,type:String = "000000",temperature:Float = -99f){
        Column(
            modifier = Modifier
                .padding(horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Text("${if(hour<10) "0" else ""}$hour:00")
            Text(type)
            Text(temperature.toString() + controller.getWeatherMetrics().symbol, fontWeight = FontWeight.Bold)
        }
    }

}