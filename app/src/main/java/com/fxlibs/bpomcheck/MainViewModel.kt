package com.fxlibs.bpomcheck

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel : ViewModel() {

    data class Result(val status:Status, val plainHtml:String? = null)
    enum class Status{SUCCESS, ERROR_CONNECTION, ERROR_DATA, ERROR_NOT_FOUND}

    data class Session(val cookieId:String, val sessionId:String)

    data class Info(val field:String,var value:String)

    var userSession : Session? = null
    val resultData = MutableLiveData<Result>()


    fun getInfo(bpomId:String) {
        if (userSession == null) {
            startSession(onSuccess = {
                getInfo(bpomId)
            },
            onFailed = {
                resultData.postValue(Result(Status.ERROR_CONNECTION))
            })
            return
        }
        APIService.get().getInfo(userSession!!.cookieId, userSession!!.sessionId, bpomId)
            .enqueue(object : Callback<String>{

                override fun onResponse(call: Call<String>?, response: Response<String>?) {
                    try {
                        response?.body().let {
                            if (it.isNullOrBlank()) {
                                resultData.postValue(Result(Status.ERROR_NOT_FOUND))
                            }
                            else {
                                val idx = it.indexOf("urldetil")
                                val ids = it.indexOf("/", idx) + 1
                                val ide = it.indexOf("\"", ids)
                                it.substring(ids, ide).let { id ->
                                    Log.d(javaClass.name, "ID_DETAIL >> $id")
                                    getDetail(id)
                                }
                            }
                        }
                    } catch (e:Exception) {
                        resultData.postValue(Result(Status.ERROR_NOT_FOUND))
                    }
                }

                override fun onFailure(call: Call<String>?, t: Throwable?) {
                    Log.e(javaClass.name, t?.message, t)
                    resultData.postValue(Result(Status.ERROR_CONNECTION))
                }

            })
    }

    private fun startSession(onSuccess:() -> Unit, onFailed:() -> Unit) {
        APIService.get().getSession()
            .enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>?, response: Response<String>?) {
                    response?.headers()?.get("Set-Cookie").let {
                        if (it.isNullOrBlank()) {
                            resultData.postValue(Result(Status.ERROR_DATA))
                            onFailed.invoke()
                        }
                        else {
                            userSession = Session(
                                it.split(";")[0],
                                it.split(";")[0].split("=", limit = 2)[1])
                            onSuccess.invoke()
                        }
                    }
                }

                override fun onFailure(call: Call<String>?, t: Throwable?) {
                    Log.e(javaClass.name, t?.message, t)
                    resultData.postValue(Result(Status.ERROR_CONNECTION))
                }

            })
    }

    private fun getDetail(urldetilId:String) {
        APIService.get().getDetail(userSession!!.cookieId, userSession!!.sessionId, urldetilId)
            .enqueue(object : Callback<String>{

                override fun onResponse(call: Call<String>?, response: Response<String>?) {
                    try {
                        response?.body().let { it ->
                            if (it.isNullOrBlank()) {
                                resultData.postValue(Result(Status.ERROR_NOT_FOUND))
                            }
                            else {
                                try {
                                    val html = Jsoup.parse(it).apply {
                                        outputSettings().charset("ASCII")
                                        outputSettings().escapeMode(Entities.EscapeMode.base)
                                    }

                                    val outHtml = StringBuilder("<table>")

                                    val tables = html.getElementsByTag("table")
                                    for (i in 1 until tables.size) {
                                        tables[i].getElementsByTag("td").forEach { td ->
                                            outHtml.append("<tr>")
                                            if (td.html() == "&nbsp;") {
                                                outHtml.append("</br>")
                                            }
                                            else {
                                                outHtml.append("<td>")
                                                if (td.hasClass("subs")) {
                                                    outHtml.append("<b>")
                                                    outHtml.append(td.html())
                                                    outHtml.append("</b>")
                                                }
                                                else {
                                                    outHtml.append(td.html())
                                                }
                                                outHtml.append("</td>")
                                            }
                                            outHtml.append("</tr>")
                                        }
                                    }

                                    resultData.postValue(Result(Status.SUCCESS, outHtml.toString()))
                                } catch (e:Exception) {
                                    resultData.postValue(Result(Status.SUCCESS, it))
                                }
                            }
                        }
                    } catch (e:Exception) {
                        resultData.postValue(Result(Status.ERROR_NOT_FOUND))
                    }
                }

                override fun onFailure(call: Call<String>?, t: Throwable?) {
                    Log.e(javaClass.name, t?.message, t)
                    resultData.postValue(Result(Status.ERROR_CONNECTION))
                }

            })

    }

}