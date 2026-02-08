import setup.moduleSetup

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

moduleSetup()

dependencies {
    // Keep core_log_component low-level (no dependency on :core_system_component); runtime wiring happens in :core_system_component.
    // Avoid leaking :data_component transitively; consumers should declare it explicitly when used.
    implementation(project(":data_component"))

    implementation(Dependencies.AndroidX.core)
    implementation(Dependencies.Tencent.mmkv)
    implementation(Dependencies.Tencent.bugly)

    // MMKV 配置表注解处理器：jar 统一放在 repository/mmkv
    implementation(files("../repository/mmkv/mmkv-annotation.jar"))
    kapt(files("../repository/mmkv/mmkv-compiler.jar"))

    testImplementation(Dependencies.Testing.androidx_test_core)
    testImplementation(Dependencies.Testing.robolectric)
    testImplementation(Dependencies.Kotlin.coroutines_test)
}

android {
    namespace = "com.xyoye.core_log_component"
}
