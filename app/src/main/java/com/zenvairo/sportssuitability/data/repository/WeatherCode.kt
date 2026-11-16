package com.zenvairo.sportssuitability.data.repository

/** WMO weather interpretation codes as published by Open-Meteo, in Spanish. */
fun weatherCodeText(code: Int?): String? = when (code) {
    null -> null
    0 -> "Despejado"
    1 -> "Mayormente despejado"
    2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45 -> "Niebla"
    48 -> "Niebla helada"
    51 -> "Llovizna débil"
    53 -> "Llovizna moderada"
    55 -> "Llovizna intensa"
    56 -> "Llovizna helada débil"
    57 -> "Llovizna helada intensa"
    61 -> "Lluvia débil"
    63 -> "Lluvia moderada"
    65 -> "Lluvia fuerte"
    66 -> "Lluvia helada débil"
    67 -> "Lluvia helada fuerte"
    71 -> "Nevada débil"
    73 -> "Nevada moderada"
    75 -> "Nevada fuerte"
    77 -> "Cinarra"
    80 -> "Chubascos débiles"
    81 -> "Chubascos moderados"
    82 -> "Chubascos torrenciales"
    85 -> "Chubascos de nieve débiles"
    86 -> "Chubascos de nieve fuertes"
    95 -> "Tormenta"
    96 -> "Tormenta con granizo"
    99 -> "Tormenta con granizo fuerte"
    else -> "Código $code"
}

/** A compact emoji for the code, used in the day strip and notifications. */
fun weatherCodeEmoji(code: Int?, isDay: Boolean = true): String = when (code) {
    null -> "•"
    0 -> if (isDay) "☀️" else "🌙"
    1, 2 -> if (isDay) "🌤️" else "☁️"
    3 -> "☁️"
    45, 48 -> "🌫️"
    51, 53, 55, 56, 57 -> "🌦️"
    61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
    71, 73, 75, 77, 85, 86 -> "❄️"
    95, 96, 99 -> "⛈️"
    else -> "•"
}
