package com.example.weather_app

class ApiManager {
    private val API_BASE = "https://api.openweathermap.org/data/2.5/weather"
    private val API_KEY = "41b05a0d1a410e43fb0d797d6c800b88"
    private val WEATHER_ICON_BASE_URI = "https://openweathermap.org/img/wn/"
    private val WEATHER_ICON_SUFFIX_URI = "@2x.png"
    private val FORECAST_API_BASE = "https://api.openweathermap.org/data/2.5/forecast"
    private var forecastApiUrl: String = ""
    private var apiUri: String = ""
    private var iconUri: String = ""
    fun getApiUri(): String{
        return apiUri
    }

    fun setWeatherUriByCityName(code: String){
        iconUri = WEATHER_ICON_BASE_URI + code + WEATHER_ICON_SUFFIX_URI
    }

    fun setForecastUriByCords(lat: Double, lon: Double){
        forecastApiUrl = "$FORECAST_API_BASE?lat$lat&lon=$lon&appid=$API_KEY"
    }

    fun setForecastUri(cityName: String){
        forecastApiUrl = "$FORECAST_API_BASE?q=$cityName&appid=$API_KEY"
    }

    fun getForecastUri(): String{
        return forecastApiUrl
    }

    fun getWeatherUri(): String {
        return iconUri
    }

    constructor(lat: Double, lon:Double){
        apiUri = "$API_BASE?lat=$lat&lon=$lon&appid=$API_KEY"
    }

    constructor(cityName: String){
        apiUri = "$API_BASE?q=$cityName&appid=$API_KEY"
    }

    constructor(){}
}