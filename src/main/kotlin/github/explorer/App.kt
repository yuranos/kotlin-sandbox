package github.explorer

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

fun extractUserInfo(userInfoData: String): UserInfo? =
    UserInfo.deserializeFromJson(userInfoData)

fun saveUserInfo(userInfo: UserInfo?): UserInfo? {
    if (userInfo == null) {
        return userInfo
    }

    return saveRecord(userInfo)
}

fun addStarRating(userInfo: UserInfo?): UserInfo? {
    if (userInfo == null) {
        return userInfo
    }

    if (userInfo.publicReposCount > 20) {
        userInfo.username = userInfo.username + " ⭐"
    }
    return userInfo
}

fun getUserInfo(username: String): UserInfo? {
    val apiData = callApi(username)
    val userInfo = extractUserInfo(apiData)
    val ratedUserInfo = addStarRating(userInfo)
    return saveUserInfo(ratedUserInfo)
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
        println(getUserInfo(username ?: "dryblaze"))
    } catch (ex: Exception) {
        println("Error occurred: $ex")
    }
}
