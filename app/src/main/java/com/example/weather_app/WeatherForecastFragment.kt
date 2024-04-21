package com.example.weather_app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


class WeatherForecastFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_weather_forecast, container, false)
    }

    companion object {
        fun newInstance(weatherForecast: WeatherForecast): WeatherForecastFragment {
            val fragment = WeatherForecastFragment()
            val args = Bundle().apply {
                putParcelable("WeatherForecast", weatherForecast)
            }
            fragment.arguments = args
            return fragment
        }

        fun setForecastInfo(weatherForecast: WeatherForecast, view: View){

        }
    }
}