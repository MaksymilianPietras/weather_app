package com.example.weather_app

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
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
            activity: FragmentActivity
        ) {
            for (cityData in citiesData) {
                if (cityData.name == cityName) {
                    CitiesFragment.updateCityDataForCityBtnFromFile(cityName, adapter, activity)
                    break
                }
            }
        }

        fun removeCityFromInternalStorage(city: String, activity: FragmentActivity) {
            Configuration.fileLock.lock()
            var fileContent = readCitiesDataFromInternalStorage(activity)
            fileContent = fileContent.filter { it.name.uppercase() != city.uppercase() }
            val parsedFileContent = Gson().toJson(fileContent)
            val internalStorage = "weather_data.txt"
            val fileOutputStream: FileOutputStream =
                activity.openFileOutput(internalStorage, AppCompatActivity.MODE_PRIVATE)
            fileOutputStream.bufferedWriter().use { it.write(parsedFileContent) }
            fileOutputStream.close()

            var weatherForecastData = readCitiesForecastFromInternalStorage(activity)
            weatherForecastData = weatherForecastData.filter { it.city.name.uppercase() != city.uppercase() }
            val parsedForecastContent = Gson().toJson(weatherForecastData)
            val forecastInternalStorage = "weather_forecast.txt"
            val forecastFileOutputStream: FileOutputStream =
                activity.openFileOutput(forecastInternalStorage, AppCompatActivity.MODE_PRIVATE)
            forecastFileOutputStream.bufferedWriter().use { it.write(parsedForecastContent) }
            forecastFileOutputStream.close()
            Configuration.fileLock.unlock()
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
                Configuration.fileLock.lock()
                val inputStream: FileInputStream = activity.openFileInput(internalStorage)
                fileContent = inputStream.bufferedReader().use { it.readText() }
                val typeToken = object : TypeToken<List<WeatherData>>() {}.type
                if (fileContent != ""){
                    citiesData = Gson().fromJson(fileContent, typeToken)

                }

                inputStream.close()
                Configuration.fileLock.unlock()
            } catch (_: FileNotFoundException) {}

            return citiesData
        }

        fun saveCityDataToInternalStorage(
            weatherData: WeatherData,
            activity: FragmentActivity
        ) {
            Configuration.fileLock.lock()
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
                    Configuration.fileLock.unlock()
                    return
                }
            }

            newFileContent.add(weatherData)
            val contentToSave = Gson().toJson(newFileContent)

            outputStream.bufferedWriter().use { it.write(contentToSave) }

            outputStream.close()
            Configuration.fileLock.unlock()


        }

         fun saveCityForecastToInternalStorage(weatherForecast: WeatherForecast, activity: FragmentActivity) {
            Configuration.fileLock.lock()
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
                    Configuration.fileLock.unlock()
                    return
                }
            }

            newFileContent.add(weatherForecast)
            val contentToSave = Gson().toJson(newFileContent)

            outputStream.bufferedWriter().use { it.write(contentToSave) }

            outputStream.close()
            Configuration.fileLock.unlock()
        }

        fun readCitiesForecastFromInternalStorage(activity: FragmentActivity): List<WeatherForecast> {

            val internalStorage = "weather_forecast.txt"
            val fileContent: String
            var citiesForecast: List<WeatherForecast> = ArrayList()
            try {
                Configuration.fileLock.lock()
                val inputStream: FileInputStream = activity.openFileInput(internalStorage)
                fileContent = inputStream.bufferedReader().use { it.readText() }
                val typeToken = object : TypeToken<List<WeatherForecast>>() {}.type
                if (fileContent != ""){
                    citiesForecast = Gson().fromJson(fileContent, typeToken)

                }

                inputStream.close()
                Configuration.fileLock.unlock()
            } catch (_: FileNotFoundException) {}

            return citiesForecast
        }
        fun readCitiesForecastFromInternalStorageByCityName(activity: FragmentActivity, cityName: String): WeatherForecast {

            val internalStorage = "weather_forecast.txt"
            val fileContent: String
            var citiesForecast: List<WeatherForecast> = ArrayList()
            try {
                Configuration.fileLock.lock()
                val inputStream: FileInputStream = activity.openFileInput(internalStorage)
                fileContent = inputStream.bufferedReader().use { it.readText() }
                val typeToken = object : TypeToken<List<WeatherForecast>>() {}.type
                if (fileContent != ""){
                    citiesForecast = Gson().fromJson(fileContent, typeToken)
                    citiesForecast = citiesForecast.filter { it.city.name.uppercase() == cityName.uppercase() }

                }

                inputStream.close()
                Configuration.fileLock.unlock()
            } catch (_: FileNotFoundException) {}

            return citiesForecast[0]
        }

        fun readCitiesDataFromInternalStorageByCityName(activity: FragmentActivity, cityName: String): WeatherData {

            val internalStorage = "weather_data.txt"
            val fileContent: String
            var citiesData: List<WeatherData> = ArrayList()
            try {
                Configuration.fileLock.lock()
                val inputStream: FileInputStream = activity.openFileInput(internalStorage)
                fileContent = inputStream.bufferedReader().use { it.readText() }
                val typeToken = object : TypeToken<List<WeatherData>>() {}.type
                if (fileContent != ""){
                    citiesData = Gson().fromJson(fileContent, typeToken)
                    citiesData = citiesData.filter { it.name.uppercase() == cityName.uppercase() }

                }

                inputStream.close()
                Configuration.fileLock.unlock()
            } catch (_: FileNotFoundException) {}

            return citiesData[0]
        }
    }


}

