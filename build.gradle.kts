plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    implementation(libs.brotli)
    implementation(libs.metrolist.extractor)
    // innertubex is a KMP library that only publishes android + common
    // variants on JitPack. Since :core is a JVM module, it can't resolve
    // the android variant. The dependency is declared in :app instead,
    // where the Android variant resolves correctly.
    // api(libs.innertubex)
    implementation(libs.re2j)
    testImplementation(libs.junit)
}
