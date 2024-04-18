package com.example.weather_app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class WindFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_wind, container, false)
    }

    fun setLocationAdditionalInfo(weatherData: WeatherData){
        requireView().findViewById<TextView>(R.id.windPower).text = "Wind speed: ${weatherData.wind.speed} m/sec"
        requireView().findViewById<TextView>(R.id.windDir).text = "Wind deg: ${weatherData.wind.deg}°"

        requireView().findViewById<TextView>(R.id.humidity).text = "Humidity: ${weatherData.main.humidity}%"
        requireView().findViewById<TextView>(R.id.visibility).text = "Visibility: ${weatherData.visibility} m"
        requireView().findViewById<TextView>(R.id.pressure).text = "Pressure: ${weatherData.main.pressure} hPa"
    }

}