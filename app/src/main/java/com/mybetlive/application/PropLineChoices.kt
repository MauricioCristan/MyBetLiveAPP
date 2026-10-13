package com.mybetlive.application

import com.mybetlive.data.LineRule
import java.math.BigDecimal
import java.math.RoundingMode

/** Enumerates the server's admissible range, without floating-point increments. */
fun propLineChoices(rule: LineRule?, halves: Boolean): List<String> {
    if(rule==null||!rule.min.isFinite()||!rule.max.isFinite()||!rule.step.isFinite()||
        rule.max<rule.min||rule.step<=0)return emptyList()
    val min=rule.min.toBigDecimal();val max=rule.max.toBigDecimal();val step=rule.step.toBigDecimal()
    val last=max.subtract(min).divide(step,0,RoundingMode.FLOOR)
    if(last>BigDecimal("10000"))return emptyList()
    return (0..last.toInt()).map {min.add(step.multiply(it.toBigDecimal()))}
        .filter { it.remainder(BigDecimal.ONE).abs().compareTo(if(halves)BigDecimal("0.5") else BigDecimal.ZERO)==0 }
        .map {it.stripTrailingZeros().toPlainString()}
}

fun propCondition(value: String,selection: String): String? {
    val line=value.toBigDecimalOrNull()?:return null
    val integer=line.stripTrailingZeros().scale()<=0
    val threshold=if(selection=="OVER")line.setScale(0,RoundingMode.FLOOR).add(BigDecimal.ONE)
        else line.setScale(0,RoundingMode.CEILING).subtract(BigDecimal.ONE)
    if(threshold.signum()<0)return "Con 0 hay push; esta estadística no puede quedar por debajo de 0."
    val criterion="Para cumplir: ${threshold.toPlainString()} o ${if(selection=="OVER")"más" else "menos"} al finalizar."
    return criterion+if(integer)" Con ${line.toPlainString()} hay push." else ""
}
