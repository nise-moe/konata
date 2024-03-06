package com.nisemoe.konata

import com.nisemoe.konata.tools.getEvents
import com.nisemoe.konata.tools.processReplayData
import org.jetbrains.bio.viktor.F64Array

class Replay(string: String, id: Long? = null, mods: Int = 0) {

    private var events: ArrayList<ReplayEvent>

    var vector: F64Array
    var axis: DoubleArray
    var id: Long? = null
    var mods: Int = 0

    init {
        this.id = id
        this.mods = mods

        this.events = getEvents(string)
        this.vector = processReplayData(this.events)
        this.axis = this.vector.view(2, axis = 1).toDoubleArray()
    }

    fun hasHR(): Boolean {
        return (mods and (1 shl 4)) == 1 shl 4
    }

}

