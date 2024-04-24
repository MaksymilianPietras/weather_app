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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let { args ->
            val windSpeed = args.getDouble("windSpeed", 0.0)
            val windDeg = args.getInt("windDeg", 0)
            val humidity = args.getInt("humidity", 0)
            val visibility = args.getInt("visibility", 0)
            val pressure = args.getDouble("pressure", 0.0)

            val weatherData = WeatherData(windSpeed, windDeg, humidity, visibility, pressure)
            setLocationAdditionalInfo(weatherData, requireView())
        }
    }

    companion object {
        fun newInstance(weatherData: WeatherData?): WindFragment {
            val fragment = WindFragment()
            if (weatherData == null){
                return fragment
            }
            val args = Bundle().apply {
                putDouble("windSpeed", weatherData.wind.speed)
                putInt("windDeg", weatherData.wind.deg)
                putInt("humidity", weatherData.main.humidity)
                putInt("visibility", weatherData.visibility)
                putDouble("pressure", weatherData.main.pressure)
            }
            fragment.arguments = args
            return fragment
        }

        fun setLocationAdditionalInfo(weatherData: WeatherData, view: View){
            view.findViewById<TextView>(R.id.windPower).text = "Wind speed: ${weatherData.wind.speed} m/sec"
            view.findViewById<TextView>(R.id.windDir).text = "Wind deg: ${weatherData.wind.deg}°"

            view.findViewById<TextView>(R.id.humidity).text = "Humidity: ${weatherData.main.humidity}%"
            view.findViewById<TextView>(R.id.visibility).text = "Visibility: ${weatherData.visibility} m"
            view.findViewById<TextView>(R.id.pressure).text = "Pressure: ${weatherData.main.pressure} hPa"
        }
    }


}