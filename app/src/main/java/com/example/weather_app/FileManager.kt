package com.example.weather_app

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class FileManager {
    companion object {
        fun setCityDataFromFileLines(
            citiesData: List<WeatherData>,
            cityName: String,
            adapter: MainActivity.ViewPagerAdapter,
            context: Context,
            activity: FragmentActivity
        ) {
            for (cityData in citiesData) {
                if (cityData.name == cityName) {
                    CitiesFragment.updateCityDataForCityBtn(cityName, adapter, context, activity)
                    break
                }
            }
        }

        fun removeCityFromInternalStorage(city: String, activity: FragmentActivity) {
            var fileContent = readCitiesDataFromInternalStorage(activity)
            fileContent = fileContent.filter { it.name.uppercase() != city.uppercase() }
            val parsedFileContent = Gson().toJson(fileContent)
            val internalStorage = "weather_data.txt"
            val fileOutputStream: FileOutputStream =
                activity.openFileOutput(internalStorage, AppCompatActivity.MODE_PRIVATE)
            fileOutputStream.bufferedWriter().use { it.write(parsedFileContent) }
            fileOutputStream.close()

            val weatherForecastData = readCitiesForecastFromInternalStorage(activity)
            val parsedForecastContent = Gson().toJson(weatherForecastData)
            val forecastInternalStorage = "weather_forecast.txt"
            val forecastFileOutputStream: FileOutputStream =
                activity.openFileOutput(forecastInternalStorage, AppCompatActivity.MODE_PRIVATE)
            forecastFileOutputStream.bufferedWriter().use { it.write(parsedForecastContent) }
            forecastFileOutputStream.close()
        }

        fun getCitiesNamesFromFileContent(weathersData: List<WeatherData>): List<String> {
            val splittedCities = ArrayList<String>()
            for (weatherData in weathersData){
                splittedCities.add(weatherData.name)
            }
            return splittedCities
        }


        fun readCitiesDataFromInternalStorage(activity: FragmentActivity): List<WeatherData> {
            val internalStorage = "weather_data.txt"
            val fileContent: String
            var citiesData: List<WeatherData> = ArrayList()
            try {

                val inputStream: FileInputStream = activity.openFileInput(internalStorage)
                fileContent = inputStream.bufferedReader().use { it.readText() }
                val typeToken = object : TypeToken<List<WeatherData>>() {}.type
                if (fileContent != ""){
                    citiesData = Gson().fromJson(fileContent, typeToken)

                }

                inputStream.close()
            } catch (_: FileNotFoundException) {}

            return citiesData
        }

        fun saveCityDataToInternalStorage(
            weatherData: WeatherData,
            activity: FragmentActivity
        ) {

            val citiesData = readCitiesDataFromInternalStorage(activity)
            val internalStorage = "weather_data.txt"
            val outputStream: FileOutputStream = activity.openFileOutput(internalStorage,
                AppCompatActivity.MODE_PRIVATE
            )

            val newFileContent = citiesData.toMutableList()

            for (data in newFileContent){
                if (data.name == weatherData.name){
                    newFileContent.remove(data)
                    newFileContent.add(weatherData)
                    val contentToSave = Gson().toJson(newFileContent)
                    outputStream.bufferedWriter().use { it.write(contentToSave) }
                    outputStream.close()
                    return
                }
            }

            newFileContent.add(weatherData)
            val contentToSave = Gson().toJson(newFileContent)

            outputStream.bufferedWriter().use { it.write(contentToSave) }

            outputStream.close()
        }

        fun saveCityForecastToInternalStorage(weatherForecast: WeatherForecast, activity: FragmentActivity) {
            val citiesForecast = readCitiesForecastFromInternalStorage(activity)
            val internalStorage = "weather_forecast.txt"
            val outputStream: FileOutputStream = activity.openFileOutput(internalStorage,
                AppCompatActivity.MODE_PRIVATE
            )

            val newFileContent = citiesForecast.toMutableList()

            for (forecast in newFileContent){
                if (forecast.city.name == weatherForecast.city.name){
                    newFileContent.remove(forecast)
                    newFileContent.add(weatherForecast)
                    val contentToSave = Gson().toJson(newFileContent)
                    outputStream.bufferedWriter().use { it.write(contentToSave) }
                    outputStream.close()
                    return
                }
            }

            newFileContent.add(weatherForecast)
            val contentToSave = Gson().toJson(newFileContent)

            outputStream.bufferedWriter().use { it.write(contentToSave) }

            outputStream.close()
        }

        fun readCitiesForecastFromInternalStorage(activity: FragmentActivity): List<WeatherForecast> {
            val internalStorage = "weather_forecast.txt"
            val fileContent: String
            var citiesForecast: List<WeatherForecast> = ArrayList()
            try {

                val inputStream: FileInputStream = activity.openFileInput(internalStorage)
                fileContent = inputStream.bufferedReader().use { it.readText() }
                val typeToken = object : TypeToken<List<WeatherForecast>>() {}.type
                if (fileContent != ""){
                    citiesForecast = Gson().fromJson(fileContent, typeToken)

                }

                inputStream.close()
            } catch (_: FileNotFoundException) {}

            return citiesForecast
        }
    }
}