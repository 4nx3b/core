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
    // innertubex is a KMP library — the JVM variant doesn't exist on JitPack
    // (only android + common). Since :core is a JVM module, we can't depend
    // on the Android variant. The dependency is declared in :app instead,
    // where the Android variant resolves correctly.
    // api(libs.innertubex)  // TODO: move facade here once JVM target is available
    implementation(libs.re2j)
    implementation(libs.rhino)
    testImplementation(libs.junit)
}
