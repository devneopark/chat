plugins {
    alias(libs.plugins.protobuf)
}

version = "0.0.1"

val protobufVersion = libs.versions.protobufRuntime.get()

dependencies {
    api(libs.protobuf.java)
    implementation("com.devneopark.chat.backend:domain-room-event:0.0.1-SNAPSHOT")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
}
