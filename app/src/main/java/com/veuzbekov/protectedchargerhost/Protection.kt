package com.veuzbekov.protectedchargerhost

sealed class Protection(name: String) {
    class Waiting(): Protection(Waiting::class.java.toString())
    class ProtectingPhone(): Protection(ProtectingPhone::class.java.toString())
    class ProtectingChild(): Protection(ProtectingChild::class.java.toString())
    class ElectricityLost(): Protection(ElectricityLost::class.java.toString())
    class Alarm(): Protection(Protection::class.java.toString())
}