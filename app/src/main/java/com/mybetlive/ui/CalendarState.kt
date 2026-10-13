package com.mybetlive.ui

import com.mybetlive.data.CalendarInfo
import java.time.LocalDate

internal fun AppState.withCalendar(calendar: CalendarInfo): AppState {
    val localToday=LocalDate.parse(calendar.date)
    return copy(today=localToday,timeZone=calendar.timeZone,date=if(followingToday)localToday else date)
}
