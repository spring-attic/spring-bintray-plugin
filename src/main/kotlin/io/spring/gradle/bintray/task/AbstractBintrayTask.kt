package io.spring.gradle.bintray.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.spring.gradle.bintray.BintrayClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input


abstract class AbstractBintrayTask: DefaultTask() {
    companion object {
        val BINTRAY_API_URL = "https://api.bintray.com"
    }

    @Input var bintrayUser: String? = null
    @Input var bintrayKey: String? = null

    open fun postConfigure() {
        onlyIf { bintrayUser != null && bintrayKey != null }
    }

    protected val bintrayClient by lazy { BintrayClient(bintrayUser!!, bintrayKey!!) }
    protected val mapper = ObjectMapper().registerModule(KotlinModule())

    override fun getGroup(): String {
        return "bintray"
    }
}