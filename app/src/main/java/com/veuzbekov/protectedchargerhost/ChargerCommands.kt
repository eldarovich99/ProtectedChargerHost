package com.veuzbekov.protectedchargerhost

object ChargerCommands {
/*    fun enable(uuid: String) = "enable_$uuid"
    fun disable(uuid: String) = "disable_$uuid"
    fun disableAlarm(uuid: String) = "disable_alarm_$uuid"
    fun enableChild(uuid: String) = "enable_child_$uuid"
    fun disableChild(uuid: String) = "disable_child_$uuid"*/

    // uuid  10 chars

    fun enableCharger(uuid: String) = "${uuid}2." // команда включения
    fun pingCharger(uuid: String) = "${uuid}1." // команда проверки
    fun disableCharger(uuid: String) = "${uuid}3." // команда отключения
    fun enableChildCharger(uuid: String) = "${uuid}2."  // команда включения
    // охраны стороннего устройства

    fun alarm(uuid: String) = "${uuid}5."

    val pingPattern = "1{uuid}."
    val enablePattern = "2{uuid}."
    val disablePattern = "3{uuid}."

    val disconnectedPattern = "4{uuid}" // сеть электропитания была отключена

    enum class Command{
        PING,
        ENABLED,
        CHILD_ENABLED,
        DISABLED,
        ALARM,
        LOST_ELECTRICITY,
        UNKNOWN
    }

    fun checkCommand(command: String, uuid: String): Command{
        return if (command.contains(uuid)){
            when(command.substring(0, 1).toIntOrNull()){
                1 -> Command.PING
                2 -> Command.ENABLED
                3 -> Command.CHILD_ENABLED
                4 -> Command.DISABLED
                5 -> Command.ALARM
                6 -> Command.LOST_ELECTRICITY
                else -> Command.UNKNOWN
            }
        }
        else{
            Command.UNKNOWN
        }
    }
}