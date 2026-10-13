package com.mybetlive.data
import java.time.Duration
import java.time.Instant

fun EventSnapshot.scheduledLabel(now:Instant=Instant.now()):String {
    val start=runCatching{Instant.parse(startsAt)}.getOrNull() ?: return "Programado"
    if(!now.isBefore(start))return "Esperando inicio"
    return if(Duration.between(now,start)<=Duration.ofMinutes(15))"Por comenzar" else "Programado"
}
