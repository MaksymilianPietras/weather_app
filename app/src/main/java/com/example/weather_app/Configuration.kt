package com.example.weather_app

import java.util.Locale
import kotlin.math.round

object Configuration {
    private lateinit var temperatureUnit: TemperatureUnit

    fun setTemperatureUnit(temperatureUnit: TemperatureUnit){
        this.temperatureUnit = temperatureUnit
    }

    fun getTemperatureUnit(): TemperatureUnit{
        return temperatureUnit
    }

    fun getTemperature(tempValue: Double, temperatureUnit: TemperatureUnit): String{
        if (temperatureUnit == this.temperatureUnit){
            return "%.2f".format(Locale.US, tempValue) + "°" + temperatureUnit.name
        } else if (temperatureUnit == TemperatureUnit.K && this.temperatureUnit == TemperatureUnit.C){
            return "%.2f".format(Locale.US, tempValue - 273.15) + "°" + this.temperatureUnit.name
        } else if (temperatureUnit == TemperatureUnit.C && this.temperatureUnit == TemperatureUnit.F){
            return "%.2f".format(Locale.US,tempValue * (9.0/5.0) + 32) + "°" + this.temperatureUnit.name
        }
        return "%.2f".format(Locale.US,(tempValue - 32) * (5.0/9.0) + 273.15) + "°" + this.temperatureUnit.name
    }
}