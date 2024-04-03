package com.example.weather_app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity


class CitiesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cities)


        findViewById<ImageButton>(R.id.addCityBtn).setOnClickListener {
            val newCity = findViewById<EditText>(R.id.cityNameText).text
            val favouriteCityFragment = FavouriteCityFragment.newInstance(newCity.toString())
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.favouriteCitiesLabel, favouriteCityFragment)
            transaction.commit()
        }

    }
}