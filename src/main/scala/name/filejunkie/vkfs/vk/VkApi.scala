package name.filejunkie.vkfs.vk

import java.io.BufferedInputStream
import java.net.URL

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jvalue2extractable
import org.json4s.jvalue2monadic
import org.json4s.string2JsonInput

import com.twitter.conversions.time.intToTimeableNumber
import com.twitter.util.ScheduledThreadPoolTimer
import com.twitter.util.SynchronizedLruMap

import name.filejunkie.vkfs.common.images.Album
import name.filejunkie.vkfs.common.images.Photo
import scalaj.http.Http
import scalaj.http.HttpResponse

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
        val response = callMethod("photos.getAlbums", ("owner_id", userId))
        val responseString = response.body
        val json = parse(responseString)
        val albumsJson = json \ "response" \ "items"

        albums ++= albumsJson.extract[List[Album]]
      }
      albums.toList
    }
  }

  def getPhotos(albumTitle: String) : Option[List[Photo]] = {
    getAlbums.find { album => albumTitle == album.title } match {
      case Some(album) => {
        val albumId = album.id.toString()

        val response = callMethod("photos.get", ("owner_id", userId), ("album_id",albumId))
        val responseString = response.body
        val json = parse(responseString)
        val photosJson = json \ "response" \ "items"

        Some(photosJson.extract[List[Photo]])
      }
      case None => None
    }
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
    val response = callMethod("photos.getById", ("owner_id", userId), ("photos",userId + "_" + photoId))

    val responseString = response.body
    val json = parse(responseString)
    val photosJson = json \ "response"

    val photo = photosJson.extract[List[Photo]].last

    photo.photo_2560.getOrElse(photo.photo_1280.getOrElse(photo.photo_807.getOrElse(photo.photo_604.get)))
  }

  def renameAlbum(oldTitle: String, newTitle: String) : Boolean = {
    token match {
      case Some(s) => {
        getAlbums.find { album => album.title == oldTitle } match {
          case Some(album) => {
            callMethod("photos.editAlbum", ("album_id", album.id.toString()), ("title", newTitle))
            true
          }
          case _ => false
        }
      }
      case _ => false
    }
  }

  private def callMethod(method: String, params: (String, String)*) : HttpResponse[String] = {
    val allParams : List[(String, String)] = ("v", VkApi.ApiVersion) :: (token match {
      case Some(x: String) => ("access_token", x) :: params.toList
      case None => params.toList
    })

    allParams.foldLeft(Http(apiPrefix + method))((a, b) => a.param(b._1, b._2)).asString
  }
}
