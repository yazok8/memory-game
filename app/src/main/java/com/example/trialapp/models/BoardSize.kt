package com.example.trialapp.models

enum class BoardSize(val numCards: Int) {

    EASY(8),
    Medium(18),
    Hard(24);

    fun getWidth(): Int{
    return when (this){
        EASY -> 2
        Medium ->  3
        Hard -> 4
        }
    }

    fun getHeight(): Int{
        return numCards / getWidth()
    }

    fun getNumPairs(): Int{
        return numCards / 2
    }
}