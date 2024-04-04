package com.example.weather_app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream

class CitiesFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_cities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fileContent = readCitiesFromInternalStorage()
        if (fileContent != ""){
            var splittedCities = fileContent.split('|')
            splittedCities = splittedCities.filter { it.isNotEmpty() }
            splittedCities.forEach {cityName -> addFavouriteCityFragmentByName(cityName) }
        }

        requireView().findViewById<ImageButton>(R.id.addCityBtn).setOnClickListener {
            val newCity = requireView().findViewById<EditText>(R.id.cityNameText).text
            saveCityNameToInternalStorage(newCity.toString())
            addFavouriteCityFragmentByName(newCity.toString())
        }
    }


    private fun addFavouriteCityFragmentByName(newCity: String) {

        val favouriteCityFragment = FavouriteCityFragment.newInstance(newCity)
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.add(R.id.favouriteCitiesLabel, favouriteCityFragment)
        transaction.commit()
    }

    private fun readCitiesFromInternalStorage(): String {
        val internalStorage = "weather_data.txt"
        var fileContent = ""
        try {
            val inputStream: FileInputStream = requireActivity().openFileInput(internalStorage)
            fileContent = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()
        } catch (_: FileNotFoundException) {}

        return fileContent
    }

    private fun saveCityNameToInternalStorage(newCity: String) {
        val internalStorage = "weather_data.txt"
        val outputStream: FileOutputStream = requireActivity().openFileOutput(internalStorage,
            AppCompatActivity.MODE_APPEND
        )
        outputStream.bufferedWriter().use { it.write("$newCity|") }

        outputStream.close()
    }

}

