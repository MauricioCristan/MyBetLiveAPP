package com.mybetlive.application
import com.mybetlive.data.*
interface BankOperations {
 fun owner():String?
 fun cached():BankCache
 suspend fun <T> call(action:suspend(BankApi)->T):T
 fun remember(cache:BankCache)
}
