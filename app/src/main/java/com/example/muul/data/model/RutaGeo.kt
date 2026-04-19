package com.example.muul.data.model

data class RutaGeo(
    val indice: Int,
    val geometryJson: String,
    val distanciaTexto: String,
    val duracionTexto: String,
    val distanciaMetros: Double,
    val duracionSegundos: Double,
    val pasos: List<PasoRuta>,
    val perfil: String = "caminando"
)