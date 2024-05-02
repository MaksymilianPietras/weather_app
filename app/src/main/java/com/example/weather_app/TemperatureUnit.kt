package com.example.weather_app

enum class TemperatureUnit {
    K,
    C,
    F;

    fun next(): TemperatureUnit {
        return when (this) {
            K -> C
            C -> F
            F -> K
        }
    }

    fun prev(): TemperatureUnit {
        return when (this) {
            K -> F
            F -> C
            C -> K
        }
    }
}