package com.example.weather_app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import fuel.Fuel
import fuel.get
import kotlinx.coroutines.runBlocking

class LocationManager {
    fun checkLocationPermission(context: Context): Boolean {
        return (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    fun requestLocationPermission(activity: AppCompatActivity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), 1
        )
    }

    fun getLocationDataByCityCords(
        location: android.location.Location?,
        context: Context
    ): WeatherData? {
        if (location == null) {
            Toast.makeText(context, "Nie można uzyskać aktualnej lokalizacji", Toast.LENGTH_SHORT).show()
            return null
        }

        val apiManager = ApiManager(location.latitude, location.longitude)
        return getWeatherDataFromApi(apiManager, context)
    }

    fun getLocationDataByCityName(cityName: String, context: Context): WeatherData? {
        if (cityName == "") {
            Toast.makeText(context, "Nie można uzyskać aktualnej lokalizacji", Toast.LENGTH_SHORT).show()
            return null
        }

        val apiManager = ApiManager(cityName)
        return getWeatherDataFromApi(apiManager, context)
    }

    fun getLocationForecastByCityCords(
        location: android.location.Location?,
        context: Context
    ): WeatherForecast? {
        if (location == null) {
            Toast.makeText(context, "Nie można uzyskać aktualnej lokalizacji", Toast.LENGTH_SHORT).show()
            return null
        }

        val apiManager = ApiManager()
        apiManager.setForecastUriByCords(location.latitude, location.longitude)
        return getWeatherForecastFromApi(apiManager, context)
    }

    fun getLocationForecastByCityName(cityName: String, context: Context): WeatherForecast? {
        if (cityName == "") {
            Toast.makeText(context, "Nie można uzyskać aktualnej lokalizacji", Toast.LENGTH_SHORT).show()
            return null
        }

        val apiManager = ApiManager()
        apiManager.setForecastUri(cityName)
        return getWeatherForecastFromApi(apiManager, context)
    }

    private fun getWeatherDataFromApi(
        apiManager: ApiManager,
        context: Context,
    ): WeatherData? {
        var weatherData: WeatherData
        runBlocking {
            val body = Fuel.get(apiManager.getApiUri()).body
            weatherData = Gson().fromJson(body, WeatherData::class.java)
        }
        if (weatherData.name == "") {
            Toast.makeText(context, "Brak danych pogodowych dla podanej lokalizacji!", Toast.LENGTH_SHORT).show()
            return null
        }
        return weatherData
    }

    private fun getWeatherForecastFromApi(
        apiManager: ApiManager,
        context: Context
    ): WeatherForecast? {
        var weatherForecast: WeatherForecast
        runBlocking {
            val body = Fuel.get(apiManager.getForecastUri()).body
            weatherForecast = Gson().fromJson(body, WeatherForecast::class.java)
        }
        if (weatherForecast.city.name == "") {
            Toast.makeText(context, "Brak danych pogodowych dla podanej lokalizacji!", Toast.LENGTH_SHORT).show()
            return null
        }
        return weatherForecast
    }
}