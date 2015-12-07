package name.filejunkie.vkfs.vk

import java.io.BufferedInputStream
import java.net.URL
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jvalue2extractable
import org.json4s.jvalue2monadic
import org.json4s.JValue
import org.json4s.string2JsonInput
import com.twitter.conversions.time.intToTimeableNumber
import com.twitter.util.ScheduledThreadPoolTimer
import com.twitter.util.SynchronizedLruMap
import name.filejunkie.vkfs.common.images.Album
import name.filejunkie.vkfs.common.images.Photo
import scalaj.http.Http
import scalaj.http.HttpResponse
import scala.reflect.ClassTag

object VkApi {
  val ApiVersion = "5.40"
  
  def authorize = {
    val url = "https://oauth.vk.com/authorize?client_id=5129436&display=page&redirect_uri=https://oauth.vk.com/blank.html&scope=photos&response_type=token&v=" + ApiVersion
    println("Please go to " + url + " and get your access token there")
  }
}

class VkApi(userId: String, token: Option[String]) {
  val FilesToStore = 10  

  val apiPrefix = "https://api.vk.com/method/"
  val clientId = 5129436
  implicit val formats = DefaultFormats
  val photos = new SynchronizedLruMap[String,Array[Byte]](FilesToStore)
  val albums = scala.collection.mutable.ListBuffer[Album]()

  new ScheduledThreadPoolTimer().schedule(0.second, 10.seconds)({
    albums synchronized {
      albums.clear()
    }
  })

  def getAlbums : List[Album] = {
    albums synchronized {
      if(albums.isEmpty){
        albums ++= callMethod("photos.getAlbums", 
                              {_ \ "response" \ "items"},
                              ("owner_id", userId)
                             ).extract[List[Album]]
      }
      albums.toList
    }
  }

  def getPhotos(albumTitle: String) : Option[List[Photo]] = {
    val album = getAlbums.find { album => albumTitle == album.title }

    for {
      alb <- album
      photos <- callMethod("photos.get",
                           {_ \ "response" \ "items"},
                           ("owner_id", userId),
                           ("album_id",alb.id.toString())
                           ).extractOpt[List[Photo]]
    } yield photos
  }

  def getPhotoSize(photoId: String) : Long = {
    val urlStr = getPhotoUrlById(photoId)
    val response: HttpResponse[String] = Http(urlStr).method("HEAD").asString

    val headers = response.headers

    headers.getOrElse("Content-Length", getPhoto(photoId).length.toString()).toLong
  }

  def getPhoto(photoId: String) : Array[Byte] = {
    photos.getOrElseUpdate(photoId, {
      val urlStr = getPhotoUrlById(photoId)

      val url = new URL(urlStr)

      val is = new BufferedInputStream(url.openStream())

      Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
    })
  }

  def getPhotoUrlById(photoId: String) : String = {
    val photo = callMethod("photos.getById",
                           {_ \ "response"},
                           ("owner_id", userId),
                           ("photos",userId + "_" + photoId)
                          ).extract[List[Photo]].last

    photo.photo_2560.getOrElse(photo.photo_1280.getOrElse(photo.photo_807.getOrElse(photo.photo_604.get)))
  }

  def renameAlbum(oldTitle: String, newTitle: String) : Boolean = {
    val album = for {
      s <- token
      album <- getAlbums.find { album => album.title == oldTitle }
    } yield album

    album match {
      case Some(x) => {
        albums synchronized {
          callMethod("photos.editAlbum", ("album_id", x.id.toString()), ("title", newTitle))
          albums.clear()
          true
        }
      }
      case None => false
    }
  }

  def createAlbum(name: String) : Boolean = {
    if(albums.exists { album => album.title == name }){
      return false
    }

    token match {
      case Some(s) => {
        albums synchronized {
          callMethod("photos.createAlbum", ("title", name), ("privacy_view", "only_me"), ("privacy_comment", "only_me"))
          albums.clear()
        }
        true
      }
      case _ => false
    }
  }

  private def callMethod(method: String, params: (String, String)*) : HttpResponse[String] = {
    val allParams = ("v", VkApi.ApiVersion) +: params.toList ::: (token match {
      case Some(x: String) => List(("access_token", x))
      case None => Nil
    })

    allParams.foldLeft(Http(apiPrefix + method))((a, b) => a.param(b._1, b._2)).asString
  }

  private def callMethod(method: String, f: JValue => JValue, params: (String, String)*) : JValue = {
    val response = callMethod(method, params:_*)
    val responseString = response.body
    val json = parse(responseString)
    f(json)
  }
}
