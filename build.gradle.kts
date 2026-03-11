plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    // Remove the Kotlin plugin line completely
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}