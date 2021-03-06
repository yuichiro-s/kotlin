/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.xhr

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*

public external abstract class XMLHttpRequestEventTarget : EventTarget {
    open var onloadstart: ((Event) -> dynamic)?
    open var onprogress: ((Event) -> dynamic)?
    open var onabort: ((Event) -> dynamic)?
    open var onerror: ((Event) -> dynamic)?
    open var onload: ((Event) -> dynamic)?
    open var ontimeout: ((Event) -> dynamic)?
    open var onloadend: ((Event) -> dynamic)?
}

public external abstract class XMLHttpRequestUpload : XMLHttpRequestEventTarget {
}

public external open class XMLHttpRequest : XMLHttpRequestEventTarget {
    var onreadystatechange: ((Event) -> dynamic)?
    open val readyState: Short
    var timeout: Int
    var withCredentials: Boolean
    open val upload: XMLHttpRequestUpload
    open val responseURL: String
    open val status: Short
    open val statusText: String
    var responseType: String
    open val response: Any?
    open val responseText: String
    open val responseXML: Document?
    fun open(method: String, url: String): Unit
    fun open(method: String, url: String, async: Boolean, username: String? = definedExternally, password: String? = definedExternally): Unit
    fun setRequestHeader(name: String, value: String): Unit
    fun send(body: dynamic = definedExternally): Unit
    fun abort(): Unit
    fun getResponseHeader(name: String): String?
    fun getAllResponseHeaders(): String
    fun overrideMimeType(mime: String): Unit

    companion object {
        val UNSENT: Short
        val OPENED: Short
        val HEADERS_RECEIVED: Short
        val LOADING: Short
        val DONE: Short
    }
}

public external open class FormData(form: HTMLFormElement = definedExternally) {
    fun append(name: String, value: String): Unit
    fun append(name: String, value: Blob, filename: String = definedExternally): Unit
    fun delete(name: String): Unit
    fun get(name: String): dynamic
    fun getAll(name: String): Array<dynamic>
    fun has(name: String): Boolean
    fun set(name: String, value: String): Unit
    fun set(name: String, value: Blob, filename: String = definedExternally): Unit
}

public external open class ProgressEvent(type: String, eventInitDict: ProgressEventInit = definedExternally) : Event {
    open val lengthComputable: Boolean
    open val loaded: Int
    open val total: Int
}

public external interface ProgressEventInit : EventInit {
    var lengthComputable: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var loaded: Int? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
    var total: Int? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
}

public inline fun ProgressEventInit(lengthComputable: Boolean? = false, loaded: Int? = 0, total: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): ProgressEventInit {
    val o = js("({})")

    o["lengthComputable"] = lengthComputable
    o["loaded"] = loaded
    o["total"] = total
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

