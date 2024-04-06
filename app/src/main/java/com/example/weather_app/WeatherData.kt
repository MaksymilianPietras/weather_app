package com.example.weather_app

import java.time.LocalTime

class WeatherData{
    var name: String = ""
    var coord: Coords = Coords()
    var weather: List<Weather> = listOf()
    var main: Main = Main()
    var timezone: Int = 0

    inner class Coords{
        var lat: Double = 0.0
        var lon: Double = 0.0
    }

    inner class Weather{
        var main: String = ""
        var icon: String = ""
    }

    inner class Main{
        var temp: Double = 0.0
        var temp_min: Double = 0.0
        var temp_max: Double = 0.0
        var pressure: Double = 0.0
    }
}

