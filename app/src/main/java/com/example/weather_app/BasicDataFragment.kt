package com.example.weather_app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class BasicDataFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_basic_data, container, false)
    }

    fun setWeatherData(weatherData: WeatherData){
        requireView().findViewById<TextView>(R.id.city).text = weatherData.name
        requireView().findViewById<TextView>(R.id.geoCords).text =
            "${weatherData?.coord?.lat}° szer. geogr. ${weatherData?.coord?.lon}° dł. geogr."
        requireView().findViewById<TextView>(R.id.temperature).text = weatherData?.main?.temp.toString() + "°"
        requireView().findViewById<TextView>(R.id.time).text = weatherData?.time.toString()
        requireView().findViewById<TextView>(R.id.pressure).text = "${weatherData?.main?.pressure} hPa"
    }


}