pluginManagement {
    repositories {
        // 阿里云镜像 - 加速下载
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像 - 加速下载
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        google()
        mavenCentral()
        ivy {
            name = "SherpaOnnxGitHubReleases"
            url = uri("https://github.com/k2-fsa/sherpa-onnx/releases/download")
            patternLayout {
                artifact("v[revision]/sherpa-onnx-[revision].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("com.k2fsa", "sherpa-onnx")
            }
        }
        ivy {
            name = "SherpaOnnxJavaGitHubReleases"
            url = uri("https://github.com/k2-fsa/sherpa-onnx/releases/download")
            patternLayout {
                artifact("v[revision]/sherpa-onnx-v[revision].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("com.k2fsa", "sherpa-onnx-java")
            }
        }
        ivy {
            name = "SherpaOnnxWindowsNativeGitHubReleases"
            url = uri("https://github.com/k2-fsa/sherpa-onnx/releases/download")
            patternLayout {
                artifact("v[revision]/sherpa-onnx-native-lib-win-x64-v[revision].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("com.k2fsa", "sherpa-onnx-native-win-x64")
            }
        }
    }
}

rootProject.name = "InterviewAssistant"
include(":androidApp")
include(":shared")
include(":core:design")
include(":core:test")
include(":desktopApp")
