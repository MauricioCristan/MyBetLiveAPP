package com.mybetlive.application

import com.mybetlive.data.*
import org.junit.Test
import org.junit.Assert.*

class FloatingPickRegistryTest {
    private fun bet(id:String,status:String="LIVE_WINNING")=BetView(id,"event",EventSnapshot("NBA","fake","game","",Team("h","Home"),Team("a","Away"),20,18,"LIVE","Q1",null,""),
        "MONEYLINE","HOME",null,null,null,null,null,null,null,status,2.0,null,"",false)
    @Test fun everyActivePickHasItsOwnIdentityWithoutDuplicatesOrHistory() {
        val r=FloatingPickRegistry();val rows=listOf(bet("a"),bet("b"),bet("history","WON"))
        r.activate("a",rows);r.activate("b",rows)
        assertEquals(listOf("a","b"),r.ids)
    }
    @Test fun carouselPreservesSelectionAcrossUpdatesAndRestoresFinals(){
        val r=FloatingPickRegistry();val rows=listOf(bet("a"),bet("b"),bet("c"))
        r.activate("a",rows);r.navigate(1);assertEquals("b",r.selectedId)
        r.sync(rows+bet("d"));assertEquals("b",r.selectedId)
        r.navigate(-1);r.navigate(-1);assertEquals("d",r.selectedId)
        val restored=FloatingPickRegistry();restored.restore(r.ids,"b",listOf(bet("a"),bet("b","WON"),bet("c"),bet("d")))
        assertEquals("b",restored.selectedId);assertEquals(listOf("a","b","c","d"),restored.ids)
        restored.sync(listOf(bet("a"),bet("c")));assertEquals("a",restored.selectedId)
    }
    @Test fun closingOneDoesNotAffectOthersOrReopenOnUpdates() {
        val r=FloatingPickRegistry();val rows=listOf(bet("a"),bet("b"))
        r.activate("a",rows);r.close("a");r.sync(rows+bet("c"))
        assertEquals(listOf("b","c"),r.ids)
        r.activate("a",rows+bet("c"));assertEquals(setOf("a","b","c"),r.ids.toSet())
    }
    @Test fun simultaneousFinalResultsStayUntilClosedAndDeletedPickIsRemoved() {
        val r=FloatingPickRegistry();r.activate("a",listOf(bet("a"),bet("b")))
        r.sync(listOf(bet("a","LOST"),bet("b","WON")))
        assertEquals(listOf("a","b"),r.ids)
        r.sync(listOf(bet("b","WON")));assertEquals(listOf("b"),r.ids)
        r.close("b");assertTrue(r.ids.isEmpty())
    }
}
