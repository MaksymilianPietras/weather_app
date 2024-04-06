package com.example.weather_app

class ApiManager {
    private val API_BASE = "https://api.openweathermap.org/data/2.5/weather"
    private val API_KEY = "41b05a0d1a410e43fb0d797d6c800b88"
    private var apiUri: String

    fun getApiUri(): String{
        return apiUri
    }

    constructor(lat: Double, lon:Double){
        apiUri = API_BASE + "?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY
    }

    constructor(cityName: String){
        apiUri = API_BASE + "?q=" + cityName + "&appid=" + API_KEY
    }
}