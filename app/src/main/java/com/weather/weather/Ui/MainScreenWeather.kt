package com.weather.weather.Ui

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.weather.weather.Backend.WeatherApiBaseClass
import com.weather.weather.Controller
import com.weather.weather.Months
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import android.Manifest
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat

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

    private fun getCurrentLocation(context: Context, onLocationReceived: (Location?) -> Unit) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.create().apply {
                interval = 10_000
                fastestInterval = 5_000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let {
                        onLocationReceived(it)
                    } ?: run {
                        onLocationReceived(null)
                    }
                    fusedLocationClient.removeLocationUpdates(this)
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        onLocationReceived(null)
                    }
                }
            }

            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            } else {
                onLocationReceived(null)
            }

        } catch (e: Exception) {
            onLocationReceived(null)
        }
    }

    @Composable
    fun Render() {
        val context = LocalContext.current
        var backgroundImageUri by remember { mutableStateOf<Uri?>(null) }
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        // Activity Result Launcher for taking a picture
        val takeImageResult = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
            bmp?.let {
                bitmap = it
            } ?: run {
                // Handle the situation when bmp is null to avoid potential crashes
                bitmap = null
            }
            Log.d("TestCamera", "check the background bitmap: $bitmap" )
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions: Map<String, Boolean> ->
            if (permissions[Manifest.permission.CAMERA] == true) {
                takeImageResult.launch(null)
                // Call the function to render the UI if permissions are granted
                Log.d("Permissions", "Camera permission granted: $permissions")
            } else {
                Log.d("Permissions", "Camera permission denied: $permissions")
            }
        }

        // Activity Result Launcher for picking an image from the gallery
        val pickImageResult = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                backgroundImageUri = it
            } ?: run {
                // Handle the situation when uri is null to avoid potential crashes
                backgroundImageUri = null
            }
            Log.d("TestCamera", "check the background bitmap picked: $backgroundImageUri" )
        }

        // Button to trigger photo taking or gallery selection
        Button(onClick = {
            val photoOptions = arrayOf("Take Photo", "Pick from Gallery")
            AlertDialog.Builder(context).setTitle("Choose an option").setItems(photoOptions) { _, which ->
                when (which) {
                    0 -> {
                        // Check for camera permission before taking a picture
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                        } else {
                            takeImageResult.launch(null)
                        }
                    }
                    1 -> pickImageResult.launch("image/*")
                }
            }.show()
        }) {
            Text("Update Background")
        }

        Column(modifier = Modifier.fillMaxSize()) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp)
                        .border(1.dp, Color.Gray)
                )
            }

            backgroundImageUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(model = it),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp)
                        .border(1.dp, Color.Gray)
                )
            }
        }


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
                Spacer(Modifier.height(16.dp))

                // New Button to fetch weather by current location
                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        getCurrentLocation(context) { location ->
                            if (location != null) {
                                GlobalScope.launch {
                                    val resultCode = controller.setCityByCoordinates(context, location.latitude, location.longitude)
                                    if (resultCode == -1) {
                                        errorMessage = "Unable to determine city from current location."
                                        showErrorMessage = true
                                    } else {
                                        showErrorMessage = false
                                    }
                                }
                            } else {
                                errorMessage = "Failed to fetch location."
                                showErrorMessage = true
                            }
                        }
                    } else {
                        errorMessage = "Location permission is required to get current location"
                        showErrorMessage = true
                    }
                }

                Button(onClick = {
                    if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        getCurrentLocation(context) { location ->
                            if (location != null) {
                                GlobalScope.launch {
                                    val resultCode = controller.setCityByCoordinates(context, location.latitude, location.longitude)
                                    if (resultCode == -1) {
                                        errorMessage = "Unable to determine city from current location."
                                        showErrorMessage = true
                                    } else {
                                        showErrorMessage = false
                                    }
                                }
                            } else {
                                errorMessage = "Failed to fetch location."
                                showErrorMessage = true
                            }
                        }
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }) {
                    Text("Fetch Weather by Current Location")
                }

                AutoSizeText(
                    text = controller.getCity(),
                    textStyle = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "${currentHourForecast.temperature}${controller.getWeatherMetrics().symbol} | ${currentHourForecast.weatherCondition.value}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                )

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
                WeatherCard(currentHourForecast)
                Spacer(Modifier.height(16.dp))
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
            SquareStatCard(
                title = "Feel Like",
                data = "${weather.feelLikeTemp} ${controller.getWeatherMetrics().symbol}",
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(16.dp)) // Space between the cards

            // Card for Humidity
            SquareStatCard(
                title = "Humidity",
                data = "${weather.humidity}%",
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

            // Card for Pressure
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