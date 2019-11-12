package github.explorer

import arrow.core.Option
import com.beust.klaxon.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.LocalDateTime

// https://jorgecastillo.dev/please-try-to-use-io

sealed class AppError {
    data class UserNotFound(val errorInfo: String) : AppError()
    data class GitHubConnectionFailed(val errorInfo: String) : AppError()
    data class UserDataJsonParseFailed(val errorInfo: String) : AppError()
}

@Target(AnnotationTarget.FIELD)
annotation class KlaxonDate

data class UserInfo(
    @Json(name = "login")
    var username: String,

    @Json(name = "public_repos")
    val publicReposCount: Int,

    @Json(name = "id")
    val gitHubId: Int,

    @Json(name = "created_at")
    @KlaxonDate
    val memberSince: LocalDateTime?
) {
    companion object {
        fun deserializeFromJson(userInfoData: String): UserInfo? =
            createKlaxon().parse<UserInfo>(userInfoData)
    }
}

fun extractUserInfo(userInfoData: String): Option<UserInfo> =
    Option.fromNullable(UserInfo.deserializeFromJson(userInfoData))

fun saveUserInfo(maybeUserInfo: UserInfo): UserInfo =
    saveRecord(maybeUserInfo)

fun addStarRating(userInfo: UserInfo): UserInfo {
    if (userInfo.publicReposCount > 20) {
        userInfo.username = userInfo.username + " ⭐"
    }
    return userInfo
}

fun getUserInfo(username: String): Option<UserInfo> {
    val apiData = callApi(username)
    return extractUserInfo(apiData).map {
        addStarRating(it)
    }.map {
        saveUserInfo(it)
    }
}

fun callApi(username: String): String {
    val client = HttpClient.newBuilder().build()
    val request =
        HttpRequest
            .newBuilder()
            .uri(URI.create("https://api.github.com/users/$username"))
            .build()

    val response = client.send(request, BodyHandlers.ofString())

    return response.body()
}

fun run(args: Array<String>) {
    val username = args.firstOrNull()

    try {
        println(getUserInfo(username ?: "adomokos"))
    } catch (ex: Exception) {
        println("Error occurred: $ex")
    }
}
