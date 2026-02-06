package com.xyoye.common_component.storage.usecase

import com.xyoye.common_component.config.AppConfig
import com.xyoye.common_component.log.privacy.SensitiveDataSanitizer
import com.xyoye.common_component.network.config.Api
import com.xyoye.common_component.network.repository.OtherRepository
import com.xyoye.common_component.network.repository.ResourceRepository
import com.xyoye.common_component.utils.ErrorReportHelper
import com.xyoye.common_component.utils.danmu.DanmuFinder
import com.xyoye.data_component.bean.LocalDanmuBean
import com.xyoye.data_component.data.AnimeCidData
import com.xyoye.data_component.data.DanmuEpisodeData
import com.xyoye.data_component.data.EpisodeCidData
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.regex.Pattern

object BilibiliDanmuUseCase {
    data class QueryResult<T>(
        val data: T? = null,
        val errorMessage: String? = null
    )

    private const val TAG = "BilibiliDanmuUseCase"

    private val gzipHeader = mapOf(Pair("Accept-Encoding", "gzip,deflate"))

    suspend fun findEpisodeCidByCode(
        code: String,
        isAvCode: Boolean
    ): EpisodeCidData? =
        OtherRepository
            .getCidInfo(isAvCode, code)
            .getOrNull()
            ?.data

    suspend fun findVideoEpisodeCid(url: String): QueryResult<EpisodeCidData> {
        val htmlResult = fetchHtml(url, "findVideoEpisodeCid")
        val htmlElement = htmlResult.data ?: return QueryResult(errorMessage = htmlResult.errorMessage)

        val header = "__INITIAL_STATE__="
        val footer = ";(function"
        val footerRegex = footer.replace("(", "\\(")

        val pattern = Pattern.compile("($header).*($footerRegex)")
        val matcher = pattern.matcher(htmlElement)
        if (matcher.find().not()) {
            return QueryResult()
        }

        val jsonObject =
            try {
                JSONObject(matcher.group(0)?.removeSurrounding(header, footer).orEmpty())
            } catch (error: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    error,
                    TAG,
                    "findVideoEpisodeCid",
                    "JSON parsing failed, url=${SensitiveDataSanitizer.sanitizeUrl(url)}"
                )
                return QueryResult()
            }

        val videoJson = jsonObject.optJSONObject("videoData") ?: return QueryResult()

        val cid = videoJson.optString("cid").takeIf { it.isNotBlank() } ?: return QueryResult()
        val episodeTitle = videoJson.optString("title").orEmpty()

        return QueryResult(data = EpisodeCidData(episodeTitle, cid))
    }

    suspend fun findBangumiEpisodes(url: String): QueryResult<AnimeCidData> {
        val htmlResult = fetchHtml(url, "findBangumiEpisodes")
        val htmlElement = htmlResult.data ?: return QueryResult(errorMessage = htmlResult.errorMessage)

        val header = "window.__INITIAL_STATE__="
        val footer = "};"

        val jsonObject =
            try {
                val headerStart = htmlElement.indexOf(header)
                if (headerStart == -1) {
                    return QueryResult()
                }

                var content = htmlElement.substring(headerStart + header.length)
                val footerStart = content.indexOf(footer)
                if (footerStart == -1) {
                    return QueryResult()
                }
                content = content.substring(0, footerStart + 1)

                JSONObject(content)
            } catch (error: Exception) {
                ErrorReportHelper.postCatchedExceptionWithContext(
                    error,
                    TAG,
                    "findBangumiEpisodes",
                    "JSON parsing failed, url=${SensitiveDataSanitizer.sanitizeUrl(url)}"
                )
                return QueryResult()
            }

        val seasonTitle = jsonObject.optJSONObject("mediaInfo")?.optString("season_title").orEmpty()
        val episodesJson = jsonObject.optJSONArray("epList") ?: return QueryResult()

        val episodes = mutableListOf<EpisodeCidData>()
        for (index in 0 until episodesJson.length()) {
            val episodeJson = episodesJson.optJSONObject(index) ?: continue
            val episodeName = episodeJson.optString("long_title").orEmpty()
            val episodeIndex = episodeJson.optString("title").orEmpty()
            val episodeTitle =
                if (episodeName.isEmpty()) {
                    "第${episodeIndex}话"
                } else {
                    "第${episodeIndex}话 $episodeName"
                }

            val cid = episodeJson.optString("cid").takeIf { it.isNotBlank() } ?: continue
            episodes.add(EpisodeCidData(episodeTitle, cid))
        }

        return QueryResult(data = AnimeCidData(seasonTitle, episodes))
    }

    suspend fun downloadEpisodeDanmu(
        episodeCid: EpisodeCidData,
        animeTitle: String = ""
    ): LocalDanmuBean? {
        val url = "${Api.BILI_BILI_COMMENT}${episodeCid.cid}.xml"
        val inputStream =
            ResourceRepository
                .getResourceResponseBody(url, gzipHeader)
                .getOrNull()
                ?.byteStream()
                ?: return null

        val episodeData = DanmuEpisodeData(animeTitle = animeTitle, episodeTitle = episodeCid.title)
        return DanmuFinder.instance.saveStream(episodeData, inputStream)
    }

    private fun fetchHtml(
        url: String,
        action: String
    ): QueryResult<String> =
        try {
            val html =
                Jsoup
                    .connect(url)
                    .userAgent(AppConfig.getJsoupUserAgent().orEmpty())
                    .timeout(10 * 1000)
                    .get()
                    .toString()
            QueryResult(data = html)
        } catch (throwable: Throwable) {
            ErrorReportHelper.postCatchedExceptionWithContext(
                throwable,
                TAG,
                action,
                "url=${SensitiveDataSanitizer.sanitizeUrl(url)}"
            )
            QueryResult(errorMessage = throwable.message)
        }
}
