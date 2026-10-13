package com.mybetlive.application

import com.mybetlive.data.LineRule

fun propLineError(input: String,rule: LineRule?): String? {
    if(rule==null||!rule.step.isFinite()||rule.step<=0||!rule.min.isFinite()||!rule.max.isFinite())
        return "Actualiza el catálogo para cargar el rango permitido de esta línea."
    val normalized=input.trim().replace(',','.')
    if(normalized.length>32||!Regex("-?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)").matches(normalized))
        return "Ingresa una línea numérica válida."
    val value=normalized.toBigDecimalOrNull() ?: return "Ingresa una línea numérica válida."
    if(value<rule.min.toBigDecimal()||value>rule.max.toBigDecimal())
        return "La línea de ${rule.unit} debe estar entre ${lineNumber(rule.min)} y ${lineNumber(rule.max)}."
    if(value.remainder(rule.step.toBigDecimal()).signum()!=0)
        return "Usa una línea entera o media: por ejemplo, ${lineNumber(rule.example)}."
    return null
}
fun lineNumber(value: Double): String=java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
