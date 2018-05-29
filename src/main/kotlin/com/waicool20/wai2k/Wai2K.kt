package com.waicool20.wai2k

import javafx.application.Application
import javafx.stage.Stage

class Wai2K : Application() {

    companion object Stages {
        lateinit var ROOT: Stage
            private set
    }

    override fun start(stage: Stage) {
        ROOT = stage
    }
}

