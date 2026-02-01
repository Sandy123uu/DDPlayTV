package com.xyoye.common_component.network.service

import com.xyoye.common_component.network.config.HeaderKey
import com.xyoye.data_component.data.cloud115.Cloud115CookieStatusResp
import com.xyoye.data_component.data.cloud115.Cloud115DownloadResp
import com.xyoye.data_component.data.cloud115.Cloud115FileListResp
import com.xyoye.data_component.data.cloud115.Cloud115FileStatResponse
import com.xyoye.data_component.data.cloud115.Cloud115QRCodeLoginResp
import com.xyoye.data_component.data.cloud115.Cloud115QRCodeStatusResp
import com.xyoye.data_component.data.cloud115.Cloud115QRCodeTokenResp
import okhttp3.ResponseBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface Cloud115Service {
    @GET("/api/1.0/web/1.0/token")
    suspend fun qrcodeToken(
        @Header(HeaderKey.BASE_URL) baseUrl: String
    ): Cloud115QRCodeTokenResp

    @Streaming
    @GET("/api/1.0/mac/1.0/qrcode")
    suspend fun qrcodeImage(
        @Header(HeaderKey.BASE_URL) baseUrl: String,
        @Query("uid") uid: String
    ): ResponseBody

    @GET("/get/status/")
    suspend fun qrcodeStatus(
        @Header(HeaderKey.BASE_URL) baseUrl: String,
        @Query("uid") uid: String,
        @Query("time") time: Long,
        @Query("sign") sign: String,
        @Query("_") timestamp: String? = null
    ): Cloud115QRCodeStatusResp

    @FormUrlEncoded
    @POST("/app/1.0/{app}/1.0/login/qrcode")
    suspend fun qrcodeLogin(
        @Header(HeaderKey.BASE_URL) baseUrl: String,
        @Path("app") app: String,
        @Field("account") account: String,
        @Field("app") appInForm: String? = null
    ): Cloud115QRCodeLoginResp

    @GET("/")
    suspend fun cookieStatus(
        @Header(HeaderKey.BASE_URL) baseUrl: String,
        @Header("Cookie") cookie: String,
        @Query("ct") ct: String,
        @Query("ac") ac: String,
        @Query("_") timestamp: String? = null
    ): Cloud115CookieStatusResp

    @GET("/files")
    suspend fun listFiles(
        @Header(HeaderKey.BASE_URL) baseUrl: String,
        @Header("Cookie") cookie: String,
        @Header("User-Agent") userAgent: String,
        @Query("aid") aid: String,
        @Query("cid") cid: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("show_dir") showDir: Int,
        @Query("o") order: String? = null,
        @Query("asc") asc: Int? = null,
        @Query("format") format: String? = "json"
    ): Cloud115FileListResp

    @GET("/files/search")
    suspend fun searchFiles(
        @Header(HeaderKey.BASE_URL) baseUrl: String,
        @Header("Cookie") cookie: String,
        @Header("User-Agent") userAgent: String,
        @Query("search_value") searchValue: String,
        @Query("cid") cid: String,
        @Query("type") type: Int? = null,
        @Query("count_folders") countFolders: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("o") order: String? = null,
        @Query("asc") asc: Int? = null,
        @Query("format") format: String? = "json"
    ): Cloud115FileListResp

    @GET("/category/get")
    suspend fun stat(
        @Header(HeaderKey.BASE_URL) baseUrl: String,
        @Header("Cookie") cookie: String,
        @Header("User-Agent") userAgent: String,
        @Query("cid") cid: String
    ): Cloud115FileStatResponse

    @FormUrlEncoded
    @POST("/android/2.0/ufile/download")
    suspend fun downloadUrl(
        @Header(HeaderKey.BASE_URL) baseUrl: String,
        @Header("Cookie") cookie: String,
        @Header("User-Agent") userAgent: String,
        @Query("t") t: String,
        @Field("data") data: String
    ): Cloud115DownloadResp
}

