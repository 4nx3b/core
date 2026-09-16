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
    // SimpMusic stream-resolution port (2026-09-05): BravePipe replaces
    // MetrolistExtractor (same org.schabi.newpipe.extractor namespace, so
    // NewPipe.kt imports keep working — see NewPipe.kt for the two API
    // adaptations). PipePipe carries the tier-1/2 extraction under its own
    // dev.maxrave.pipepipe.extractor namespace. quickjs-kt runs YouTube's
    // player JS for the local cipher solver (QuickJsEngine), same version
    // SimpMusic pins.
    implementation(libs.brave.extractor)
    implementation(libs.pipepipe.extractor)
    implementation(libs.quickjs.kt)
    // NOTE: no Timber here — Timber publishes only an Android AAR and Gradle
    // rejects it for this pure-JVM module ("No matching variant ... elements
    // 'aar' ... needed ... class files"). The simpstream package logs through
    // SimpStreamLog (println by default); :app bridges it into Timber from
    // YTPlayerUtils' initializer.
    // innertubex is a KMP library that only publishes android + common
    // variants on JitPack. Since :core is a JVM module, it can't resolve
    // the android variant. The dependency is declared in :app instead,
    // where the Android variant resolves correctly.
    // api(libs.innertubex)
    implementation(libs.re2j)
    testImplementation(libs.junit)
}

// Copied verbatim from SimpMusic's kotlinYtmusicScraper build.gradle.kts:
// PipePipe brings com.google.protobuf:protobuf-java (full) while Brave brings
// com.google.protobuf:protobuf-javalite. Both occupy the com.google.protobuf.*
// namespace and trigger DEX duplicate-class failures. Drop the full variant
// globally so Brave's javalite wins.
// (The root build.gradle.kts also forces nanojson to the commit both extractors
// need — same reason, same fix, same comment.)
configurations.all {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
}
