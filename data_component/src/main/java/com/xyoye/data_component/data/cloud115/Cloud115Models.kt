package com.xyoye.data_component.data.cloud115

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

interface Cloud115ApiResponse {
    val state: Boolean
    val errno: String?
    val errNo: Int?
    val error: String?
    val errtype: String?
    val msg: String?
}

@JsonClass(generateAdapter = true)
data class Cloud115QRCodeSession(
    val uid: String? = null,
    val time: Long = 0L,
    val sign: String? = null,
    val qrcode: String? = null
)

@JsonClass(generateAdapter = true)
data class Cloud115QRCodeTokenResp(
    val code: Int = 0,
    val message: String? = null,
    val state: Int = 0,
    val errno: Int = 0,
    val error: String? = null,
    val data: Cloud115QRCodeSession? = null
)

@JsonClass(generateAdapter = true)
data class Cloud115QRCodeStatus(
    val msg: String? = null,
    val status: Int = 0,
    val version: String? = null
)

@JsonClass(generateAdapter = true)
data class Cloud115QRCodeStatusResp(
    val code: Int = 0,
    val message: String? = null,
    val state: Int = 0,
    val errno: Int = 0,
    val error: String? = null,
    val data: Cloud115QRCodeStatus? = null
)

@JsonClass(generateAdapter = true)
data class Cloud115Credential(
    @Json(name = "UID")
    val uid: String? = null,
    @Json(name = "CID")
    val cid: String? = null,
    @Json(name = "SEID")
    val seid: String? = null,
    @Json(name = "KID")
    val kid: String? = null
)

@JsonClass(generateAdapter = true)
data class Cloud115Face(
    @Json(name = "face_s")
    val faceSmall: String? = null,
    @Json(name = "face_m")
    val faceMedium: String? = null,
    @Json(name = "face_l")
    val faceLarge: String? = null
)

@JsonClass(generateAdapter = true)
data class Cloud115QRCodeLoginData(
    val cookie: Cloud115Credential? = null,
    @Json(name = "user_id")
    val userId: Long? = null,
    @Json(name = "user_name")
    val userName: String? = null,
    val mobile: String? = null,
    val email: String? = null,
    @Json(name = "is_vip")
    val isVip: Long? = null,
    val face: Cloud115Face? = null
)

@JsonClass(generateAdapter = true)
data class Cloud115QRCodeLoginResp(
    val code: Int = 0,
    val message: String? = null,
    val state: Int = 0,
    val errno: Int = 0,
    val error: String? = null,
    val data: Cloud115QRCodeLoginData? = null
)

@JsonClass(generateAdapter = true)
data class Cloud115CookieStatusResp(
    val state: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Cloud115FileInfo(
    val aid: String? = null,
    val cid: String? = null,
    val fid: String? = null,
    val pid: String? = null,
    val n: String? = null,
    val ico: String? = null,
    val s: Long? = null,
    val sha: String? = null,
    val pc: String? = null,
    val m: Int? = null,
    val t: String? = null,
    val tp: Long? = null,
    val fl: List<Cloud115LabelInfo>? = null
)

@JsonClass(generateAdapter = true)
data class Cloud115LabelInfo(
    val id: String? = null,
    val name: String? = null,
    val color: String? = null
)

@JsonClass(generateAdapter = true)
data class Cloud115FileListResp(
    override val state: Boolean = false,
    override val errno: String? = null,
    override val errNo: Int? = null,
    override val error: String? = null,
    override val errtype: String? = null,
    override val msg: String? = null,
    val aid: String? = null,
    val cid: String? = null,
    val count: Int? = null,
    val order: String? = null,
    @Json(name = "is_asc")
    val isAsc: Int? = null,
    val offset: Int? = null,
    val limit: Int? = null,
    @Json(name = "page_size")
    val pageSize: Int? = null,
    val data: List<Cloud115FileInfo>? = null
) : Cloud115ApiResponse

@JsonClass(generateAdapter = true)
data class Cloud115FileParentInfo(
    @Json(name = "file_id")
    val fileId: Long? = null,
    @Json(name = "file_name")
    val fileName: String? = null
)

@JsonClass(generateAdapter = true)
data class Cloud115FileStatResponse(
    val count: Int? = null,
    val size: String? = null,
    @Json(name = "folder_count")
    val folderCount: Int? = null,
    val ptime: Long? = null,
    val utime: Long? = null,
    @Json(name = "file_name")
    val fileName: String? = null,
    @Json(name = "pick_code")
    val pickCode: String? = null,
    val sha1: String? = null,
    @Json(name = "file_category")
    val fileCategory: Int? = null,
    val paths: List<Cloud115FileParentInfo>? = null
)

@JsonClass(generateAdapter = true)
data class Cloud115DownloadResp(
    override val state: Boolean = false,
    override val errno: String? = null,
    override val errNo: Int? = null,
    override val error: String? = null,
    override val errtype: String? = null,
    override val msg: String? = null,
    val data: String? = null
) : Cloud115ApiResponse
