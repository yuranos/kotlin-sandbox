package github.explorer

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
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
    data class UserInfoSaveFailed(val errorInfo: String) : AppError()
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

fun extractUserInfo(userInfoData: String): Either<AppError, UserInfo> {
    val userInfo = UserInfo.deserializeFromJson(userInfoData)
    return userInfo?.right() ?: AppError.UserDataJsonParseFailed("userInfoData").left()
}

fun saveUserInfo(userInfo: UserInfo): Either<AppError, UserInfo> =
    if (saveRecord(userInfo)) {
        AppError.UserInfoSaveFailed("Saving the user record failed").left()
    } else {
        userInfo.right()
    }

fun addStarRating(userInfo: UserInfo): UserInfo {
    if (userInfo.publicReposCount > 20) {
        userInfo.username = userInfo.username + " ⭐"
    }
    return userInfo
}

fun getUserInfo(username: String): Either<AppError, UserInfo> =
    callApi(username)
        .flatMap(::extractUserInfo)
        .map(::addStarRating)
        .flatMap(::saveUserInfo)

fun callApi(username: String): Either<AppError, String> {
    val client = HttpClient.newBuilder().build()
    val request =
        HttpRequest
            .newBuilder()
            .uri(URI.create("https://api.github.com/users/$username"))
            .build()

    return try {
        val response = client.send(request, BodyHandlers.ofString())

        if (response.statusCode() == 404) {
            AppError.UserNotFound(username).left()
        } else {
            response.body().right()
        }
    } catch (ex: Exception) {
        AppError.GitHubConnectionFailed(ex.toString()).left()
    }
}

fun run(args: Array<String>) {
    val username = args.firstOrNull()

    try {
        println(getUserInfo(username ?: "adomokos1"))
    } catch (ex: Exception) {
        // This should be a fatal exception - something unexpected
        println("Error occurred: $ex")
    }
}
