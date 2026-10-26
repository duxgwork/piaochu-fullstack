pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 大陆开发环境的备用 Central 镜像，不使用 HTTP 或关闭 TLS 验证。
        maven("https://maven.aliyun.com/repository/central")
    }
}
rootProject.name = "PiaochuAndroid"
include(":app")
