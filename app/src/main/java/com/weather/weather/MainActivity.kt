package com.weather.weather

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.weather.weather.Ui.DaysWeatherLong
import com.weather.weather.Ui.HourWeatherShort
import com.weather.weather.Ui.MainScreenWeather
import com.weather.weather.Ui.WeatherBar
import com.weather.weather.Ui.WeatherSettings
import com.weather.weather.ui.theme.WeatherTheme

class MainActivity : ComponentActivity() {
    companion object{
        private var instance: ComponentActivity? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        val controller:Controller = Controller()
        setContent {
            WeatherTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        controller.MainView()
                    }
                }
            }
        }
    }

}

