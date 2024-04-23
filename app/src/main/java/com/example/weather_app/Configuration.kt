package com.example.weather_app

import java.util.Locale

object Configuration {
    private lateinit var temperatureUnit: TemperatureUnit

    fun setTemperatureUnit(temperatureUnit: TemperatureUnit){
        this.temperatureUnit = temperatureUnit
    }

    fun getTemperatureUnit(): TemperatureUnit{
        return temperatureUnit
    }

    fun convertTemperatureByLetter(tempValue: Double, oldUnit: String, newUnit: String): Double {
        return when {
            oldUnit.equals("K", ignoreCase = true) && newUnit.equals("C", ignoreCase = true) -> kelvinToCelsius(tempValue)
            oldUnit.equals("K", ignoreCase = true) && newUnit.equals("F", ignoreCase = true) -> kelvinToFahrenheit(tempValue)
            oldUnit.equals("C", ignoreCase = true) && newUnit.equals("K", ignoreCase = true) -> celsiusToKelvin(tempValue)
            oldUnit.equals("C", ignoreCase = true) && newUnit.equals("F", ignoreCase = true) -> celsiusToFahrenheit(tempValue)
            oldUnit.equals("F", ignoreCase = true) && newUnit.equals("C", ignoreCase = true) -> fahrenheitToCelsius(tempValue)
            oldUnit.equals("F", ignoreCase = true) && newUnit.equals("K", ignoreCase = true) -> fahrenheitToKelvin(tempValue)
            else -> tempValue
        }
    }

    private fun celsiusToFahrenheit(celsius: Double): Double {
        return "%.2f".format(Locale.US, celsius * 9 / 5 + 32).toDouble()
    }

    private fun fahrenheitToCelsius(fahrenheit: Double): Double {
        return "%.2f".format(Locale.US, (fahrenheit - 32) * 5 / 9).toDouble()
    }

    private fun celsiusToKelvin(celsius: Double): Double {
        return "%.2f".format(Locale.US, celsius + 273.15).toDouble()
    }

    private fun kelvinToCelsius(kelvin: Double): Double {
        return "%.2f".format(Locale.US, kelvin - 273.15).toDouble()
    }

    private fun fahrenheitToKelvin(fahrenheit: Double): Double {
        return "%.2f".format(Locale.US, (fahrenheit + 459.67) * 5 / 9).toDouble()
    }

    private fun kelvinToFahrenheit(kelvin: Double): Double {
        return "%.2f".format(Locale.US, kelvin * 9 / 5 - 459.67).toDouble()
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