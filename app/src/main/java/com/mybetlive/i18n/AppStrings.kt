package com.mybetlive.i18n
import android.content.res.Resources
import com.mybetlive.R

/** Localizes legacy presentation templates at the UI boundary. Provider identities remain untouched. */
object AppStrings {
    @Volatile var resources:Resources?=null
    private data class Entry(val source:String,val target:String,val pattern:Regex?,val indices:List<Int>,val hasLiteral:Boolean)
    @Volatile private var catalog:List<Entry>?=null
    private fun entries(r:Resources):List<Entry> = catalog ?: run {
        val sources=r.getStringArray(R.array.ui_source)
        val config=android.content.res.Configuration(r.configuration).apply{setLocale(java.util.Locale.ENGLISH)}
        val english=Resources(r.assets,r.displayMetrics,config).getStringArray(R.array.ui_translation)
        val marker=Regex("\\{([0-9]+)\\}")
        sources.mapIndexed { i,s ->
            val matches=marker.findAll(s).toList();var last=0
            val expression=buildString {for(m in matches){append(Regex.escape(s.substring(last,m.range.first)));append("(.*?)");last=m.range.last+1};append(Regex.escape(s.substring(last)))}
            Entry(s,english[i],if(matches.isEmpty())null else Regex(expression,RegexOption.DOT_MATCHES_ALL),matches.map{it.groupValues[1].toInt()},marker.replace(s,"").any(Char::isLetter))
        }.sortedByDescending{it.source.length}.also{catalog=it}
    }
    fun language(r:Resources?=resources):String = if((r?.configuration?.locales?.get(0)?:java.util.Locale.getDefault()).language=="en")"en" else "es"
    fun translate(text:String,r:Resources?=resources,depth:Int=0):String {
        if(r==null||language(r)!="en"||depth>6||text.isEmpty())return text
        val entries=entries(r)
        entries.firstOrNull{it.source==text}?.let{return it.target}
        for(e in entries){if(!e.hasLiteral)continue;val match=e.pattern?.matchEntire(text)?:continue
            val args=e.indices.mapIndexed { i,index -> index to translate(match.groupValues[i+1],r,depth+1) }.toMap()
            return Regex("\\{([0-9]+)\\}").replace(e.target){args[it.groupValues[1].toInt()]?:it.value}
        }
        // Titles and captions are composed from independently localized pieces.
        for(separator in listOf("\n"," · ",": "))if(text.contains(separator))return text.split(separator).joinToString(separator){translate(it,r,depth+1)}
        for(e in entries){if(e.hasLiteral)continue;val match=e.pattern?.matchEntire(text)?:continue
            val args=e.indices.mapIndexed { i,index -> index to translate(match.groupValues[i+1],r,depth+1) }.toMap()
            return Regex("\\{([0-9]+)}").replace(e.target){args[it.groupValues[1].toInt()]?:it.value}
        }
        return text
    }
}
