package com.example.weather_app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class BasicDataFragment : Fragment() {
    companion object{
        lateinit var timeCounterScheduler: ScheduledExecutorService
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.temperature).setOnClickListener {
            Configuration.setTemperatureUnit(Configuration.getTemperatureUnit().next())
            val currentTemp = view.findViewById<Button>(R.id.temperature).text.toString()

            requireView().findViewById<TextView>(R.id.temperature).text = Configuration.getTemperature(currentTemp.substring(0, currentTemp.indexOf("°")).toDouble(),
                Configuration.getTemperatureUnit().prev())

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_basic_data, container, false)
    }

    fun setWeatherData(weatherData: WeatherData){
        requireView().findViewById<TextView>(R.id.city).text = weatherData.name
        requireView().findViewById<TextView>(R.id.geoCords).text =
            "${weatherData?.coord?.lat}° szer. geogr. ${weatherData?.coord?.lon}° dł. geogr."
        requireView().findViewById<TextView>(R.id.temperature).text =
            weatherData.main.temp.let { Configuration.getTemperature(it, Configuration.getTemperatureUnit()) }

        var zonedDateTime = getTimeForPlace(weatherData)
        requireView().findViewById<TextView>(R.id.time).text = "${zonedDateTime?.hour}:${zonedDateTime?.minute}"

        timeCounterScheduler = Executors.newScheduledThreadPool(1)

        val timeCounter = Runnable {
            zonedDateTime = getTimeForPlace(weatherData)
            zonedDateTime = zonedDateTime?.plusSeconds(1)
            requireView().findViewById<TextView>(R.id.time).text = String.format("%02d:%02d:%02d", zonedDateTime?.hour, zonedDateTime?.minute, zonedDateTime?.second)
        }

        timeCounterScheduler.scheduleAtFixedRate(timeCounter, 0, 1, TimeUnit.SECONDS)

        requireView().findViewById<TextView>(R.id.pressure).text = "${weatherData?.main?.pressure} hPa"
    }

    private fun getTimeForPlace(weatherData: WeatherData): ZonedDateTime? {
        val date = Instant.now()
        var zonedDateTime = ZonedDateTime.ofInstant(date, ZoneOffset.UTC)
        zonedDateTime = zonedDateTime.plusSeconds(weatherData.timezone.toLong())
        return zonedDateTime
    }


}