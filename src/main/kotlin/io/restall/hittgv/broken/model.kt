package io.restall.hittgv.broken

data class Country(val name: String, val code: String)

data class Island(val name: String, val country: Country)
