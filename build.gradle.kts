plugins {
    id("com.android.application") version "8.13.2" apply false
    // Remove the Kotlin plugin line completely
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}