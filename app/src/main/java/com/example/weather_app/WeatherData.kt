package com.example.weather_app


class WeatherData{
    constructor(name: String, lat: Double, lon: Double, temp: Double, timezone: Int, pressure: Double, weather: String, icon: String){
        this.name = name
        this.coord.lat = lat
        this.coord.lon = lon
        this.main.temp = temp
        this.timezone = timezone
        this.main.pressure = pressure
        this.weather = mutableListOf(Weather())
        this.weather[0].main = weather
        this.weather[0].icon = icon
    }

    constructor()

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

