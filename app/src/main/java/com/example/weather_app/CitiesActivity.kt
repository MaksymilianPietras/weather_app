package com.example.weather_app

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream


class CitiesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cities)

        val fileContent = readCitiesFromInternalStorage()
        if (fileContent != ""){
            var splittedCities = fileContent.split('|')
            splittedCities = splittedCities.filter { it.isNotEmpty() }
            splittedCities.forEach {cityName -> addFavouriteCityFragmentByName(cityName) }
        }

        findViewById<ImageButton>(R.id.addCityBtn).setOnClickListener {
            val newCity = findViewById<EditText>(R.id.cityNameText).text
            saveCityNameToInternalStorage(newCity.toString())
            addFavouriteCityFragmentByName(newCity.toString())
        }
    }

    private fun addFavouriteCityFragmentByName(newCity: String) {

        val favouriteCityFragment = FavouriteCityFragment.newInstance(newCity)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.favouriteCitiesLabel, favouriteCityFragment)
        transaction.commit()
    }

    private fun readCitiesFromInternalStorage(): String {
        val internalStorage = "weather_data.txt"
        var fileContent = ""
        try {
            val inputStream: FileInputStream = this.openFileInput(internalStorage)
            fileContent = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()
        } catch (_: FileNotFoundException) {}

        return fileContent
    }

    private fun saveCityNameToInternalStorage(newCity: String) {
        val internalStorage = "weather_data.txt"
        val outputStream: FileOutputStream = this.openFileOutput(internalStorage, MODE_APPEND)
        outputStream.bufferedWriter().use { it.write("$newCity|") }

        outputStream.close()
    }

    override fun onStop() {
        super.onStop()
    }
}