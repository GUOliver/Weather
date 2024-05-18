package com.weather.weather.Ui

import androidx.compose.foundation.Image
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.weather.Backend.WeatherApiBaseClass
import com.weather.weather.Controller
import com.weather.weather.R
import com.weather.weather.TemperatureSymbols
import com.weather.weather.WeatherProviders
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class WeatherSettings(controller: Controller) {
    private val controller:Controller
    init{
        this.controller = controller
    }
    @Composable
    fun Render(
        modifier: Modifier = Modifier
    ) {
        var weatherApiKey by rememberSaveable{mutableStateOf(controller.getWeatherKey()) }
        var weatherProvider by remember {mutableStateOf(controller.getWeatherProvider())}
//        var temperatureMetrics by remember {mutableStateOf(controller.getWeatherMetrics())}
        Column(modifier = Modifier) {

            run {
                val changeUi = remember { mutableStateOf(false )}
                val city = remember { mutableStateOf(controller.getCity()) }
                if(changeUi.value){
                    GlobalScope.launch { controller.setCity(city.value) }
                }
                ItemInList(
                    text = "Search city",
                    showSaveButton = false,
                    onCancelFunction = { city.value = controller.getCity()},
                    forceClose = changeUi
                ){
                    Column {
                        val key = remember { mutableStateOf(controller.getWeatherKey()) }
                        val weatherProvider = remember { mutableStateOf(controller.getWeatherProvider())}
                        SearchUi(
                            weatherProvider = weatherProvider,
                            city = city,
                            changeUi = changeUi,
                            key = key,
                            takeFullHeight = false,
                            showArrow = false)
                    }
                }
            }

            ItemInList(
                text = "Set Weather Service",
                onSaveFunction = {
                    controller.setWeatherApi(weatherApiKey,weatherProvider)
                },
                onCancelFunction = {
                    weatherApiKey = controller.getWeatherKey()
                    weatherProvider = controller.getWeatherProvider()
                }
            ) {
                Text(text = "Weather Service:",modifier = Modifier.padding(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically){
                    RadioButton(
                        selected =  weatherProvider == WeatherProviders.OPENWEATHER,
                        onClick = { weatherProvider = WeatherProviders.OPENWEATHER }
                    )
                    Text(text = "OpenWeather (You need an API key)")
                }
//                Row(verticalAlignment = Alignment.CenterVertically){
//                    RadioButton(
//                        selected = weatherProvider == WeatherProviders.OPENMETEO,
//                        onClick = { weatherProvider = WeatherProviders.OPENMETEO }
//                    )
//                    Text(text = "OpenMeteo")
//                }
                if(weatherProvider == WeatherProviders.OPENWEATHER) {
                    OutlinedTextField(
                        value = weatherApiKey,
                        onValueChange = { weatherApiKey = it },
                        label = { Text("Insert api key") },
                        singleLine = true
                    )
                }
            }
        }

    }
    @Composable
    fun ItemInList(
        text:String,
        onCancelFunction: () -> Unit = {},
        onSaveFunction: () -> Unit = {},
        showSaveButton:Boolean = true,
        forceClose: MutableState<Boolean> = mutableStateOf(false),
        content:@Composable () -> Unit
    ){
        var visible by remember { mutableStateOf(false) }
        val onDismissRequest: () -> Unit = {visible = !visible}
        if(forceClose.value){
            visible = false;
            forceClose.value = false
        }
        if(visible)Dialog(onDismissRequest = { onDismissRequest();onCancelFunction()},) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ){
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                ){
                    content()
                    Row{
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            onCancelFunction()
                            onDismissRequest();
                        }) {
                            Text("Cancel")
                        }
                        if(showSaveButton){
                            TextButton(onClick = {
                                onSaveFunction()
                                visible = !visible
                            }) {
                                Text("Save")
                            }
                        }
                    }

                }
            }
        }
        Column(modifier = Modifier
            .fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = {visible = !visible;},
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(0)
            ) {
                Text(text, modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                    textAlign = TextAlign.Start)

            }
            Divider(modifier = Modifier
                .fillMaxWidth(1f)
                .height(1.dp))
        }
    }
    @Composable
    fun RenderFirstStart(
        innerPadding: PaddingValues,
        currentContext: MutableState<String>
    ){
        val changeUi = remember { mutableStateOf(false )}
        val city = remember { mutableStateOf("") }
        val notDetectableCity = remember { mutableStateOf(false)}
        val key = remember { mutableStateOf("") }
        val temperatureSymbol = remember { mutableStateOf(TemperatureSymbols.CELSIUS)}
        val weatherProvider = remember { mutableStateOf(WeatherProviders.OPENWEATHER)}
        if(!changeUi.value){
            SetupUI(
                currentContext = currentContext,
                textFieldEdited = changeUi,
                city = city,
                key = key,
                temperatureSymbol = temperatureSymbol,
                weatherProvider = weatherProvider,
                notDetectableCity = notDetectableCity)
        } else{
            SearchUi(
                city = city,
                changeUi = changeUi,
                weatherProvider = weatherProvider,
                key = key
            )
        }
    }
    @Composable
    fun SearchUi(
        weatherProvider: MutableState<WeatherProviders>,
        city: MutableState<String>,
        changeUi: MutableState<Boolean>,
        key: MutableState<String>,
        takeFullHeight:Boolean = true,
        showArrow:Boolean = true
    ){
        //TODO make country code supported
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (takeFullHeight) Modifier.fillMaxHeight() else Modifier)
                .padding(50.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val findedCity = remember { mutableStateOf<Array<WeatherApiBaseClass.LatNLong>?>(null)}
            val focusRequester = FocusRequester()
            val offsetForBackButton = remember { mutableStateOf<Dp>(0.dp)}
            val localDensity = LocalDensity.current
            Row(
                modifier = Modifier.offset(-offsetForBackButton.value),
                verticalAlignment = Alignment.CenterVertically
            ){
                if(showArrow) {
                    TextButton(
                        onClick = { changeUi.value = !changeUi.value }
                    ) {
                        Image(
                            painterResource(id = R.drawable.baseline_keyboard_backspace_24), "",
                            colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.onSizeChanged {
                                offsetForBackButton.value = with(localDensity) { it.width.toDp() }
                            },
                        )
                    }
                }
            OutlinedTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onPlaced { focusRequester.requestFocus() },
                value = city.value,
                onValueChange = {
                    city.value = it
                    GlobalScope.launch {
                        GlobalScope.async {
                            findedCity.value =
                                controller.getCityFromNet(city.value, weatherProvider.value, key.value)
                        }.await()
                    }
                },
                label = {Text("Search city")},
                singleLine = true
            )}

            if(findedCity.value!=null){
                val tmp = findedCity.value
                if(tmp != null){
                    tmp.forEach {
                        TextButton(
                            onClick = {
                                city.value = it.city
                                changeUi.value = !changeUi.value; },
                            modifier = Modifier.fillMaxWidth()){
                            Text(text = "${it.city}", textAlign = TextAlign.Center)
                        }
                        Divider(modifier = Modifier
                            .fillMaxWidth(1f)
                            .height(1.dp))
                    }
                }
            }
        }

    }
    @Composable
    fun SetupUI(
        currentContext: MutableState<String>,
        textFieldEdited: MutableState<Boolean>,
        city: MutableState<String>,
        key: MutableState<String>,
        temperatureSymbol: MutableState<TemperatureSymbols>,
        weatherProvider: MutableState<WeatherProviders>,
        notDetectableCity: MutableState<Boolean>,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "First time setup:", fontSize = 32.sp)
            if(weatherProvider.value == WeatherProviders.OPENWEATHER){
                OutlinedTextField(
                    value = key.value,
                    onValueChange = {key.value = it},
                    label = {Text("Insert api key")},
                    singleLine = true,
                    isError = key.value == "")
            }
            Text(text = "Enter city name", fontSize = 24.sp)
            if(notDetectableCity.value){
                Text(text = "Couldn't find city",fontSize = 18.sp)
            }
            Column {
                OutlinedTextField(
                    modifier = Modifier.onFocusChanged { if(it.hasFocus){ textFieldEdited.value = it.hasFocus }},
                    value = city.value,
                    onValueChange = {
                        city.value = it
                        notDetectableCity.value=false },
                    label = {Text("Search City")},
                    singleLine = true,
                    isError = notDetectableCity.value)
            }
            Button(
                onClick = {
                    GlobalScope.launch {
                        val result: Int = GlobalScope.async {
                            controller.firstTimeRegister(
                                apiKey = key.value,
                                city = city.value,
                                temperatureSymbol = temperatureSymbol.value,
                                weatherProvider = weatherProvider.value
                            )
                        }.await()
                        if (result == 0) {
                            currentContext.value = "main"
                        } else if (result == -1) {
                            notDetectableCity.value = true
                        }
                    }
                },
                enabled = !notDetectableCity.value && city.value != ""
            ) {
                Text(text = "Finish")
            }
        }
    }

}