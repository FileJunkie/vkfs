package name.filejunkie.vkfs.vk

import scalaj.http._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import name.filejunkie.vkfs.common.images._
import java.net.URL
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.util.Scanner

class VkApi(userId: String, token: Option[String]) {
  val apiPrefix = "https://api.vk.com/method/"
  val clientId = 5129436
  implicit val formats = DefaultFormats
  val photos = scala.collection.mutable.Map[String,Array[Byte]]()
  
  def authorize = {
    val url = "https://oauth.vk.com/authorize?client_id=5129436&display=page&redirect_uri=https://oauth.vk.com/blank.html&scope=photos&response_type=token&v=5.37"
    println("Please go to " + url + " and get your access token there")
  }

  def getAlbums = {
    val response: HttpResponse[String] = Http(apiPrefix + "photos.getAlbums").param("owner_id",userId).param("v","5.37").asString
    val responseString = response.body
    val json = parse(responseString)
    val albumsJson = json \ "response" \ "items"
    
    albumsJson.extract[List[Album]]
  }

  def getPhotos(albumTitle: String) : Option[List[Photo]] = {
    getAlbums.find { album => albumTitle == album.title } match {
      case Some(album) => {
        val albumId = album.id.toString()

        val response: HttpResponse[String] = Http(apiPrefix + "photos.get").param("owner_id",userId).param("album_id",albumId).param("v","5.37").asString
        val responseString = response.body
        val json = parse(responseString)
        val photosJson = json \ "response" \ "items"

        Some(photosJson.extract[List[Photo]])
      }
      case None => None
    }
  }

  def getPhotoSize(photoId: String) = {
    val urlStr = getPhotoUrlById(photoId)
    val response: HttpResponse[String] = Http(urlStr).method("HEAD").asString

    val headers = response.headers

    headers.getOrElse("Content-Length", getPhoto(photoId).length.toString()).toLong
  }

  def getPhoto(photoId: String) = {
    photos.synchronized {
      if(photos.size > 10) photos.clear()

      photos.getOrElseUpdate(photoId, {
        val urlStr = getPhotoUrlById(photoId)

        val url = new URL(urlStr)

        val is = new BufferedInputStream(url.openStream())

        Stream.continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray
      })
    }
  }

  def getPhotoUrlById(photoId: String) = {
    val response: HttpResponse[String] = Http(apiPrefix + "photos.getById").param("owner_id", userId).param("photos",userId + "_" + photoId).param("v","5.37").asString

    val responseString = response.body
    val json = parse(responseString)
    val photosJson = json \ "response"

    val photo = photosJson.extract[List[Photo]].last

    photo.photo_2560.getOrElse(photo.photo_1280.getOrElse(photo.photo_807.getOrElse(photo.photo_604.get)))
  }

  def renameAlbum(oldTitle: String, newTitle: String) = {
    token match {
      case Some(s) => {
        getAlbums.find { album => album.title == oldTitle } match {
          case Some(album) => {
            val response: HttpResponse[String] = Http(apiPrefix + "photos.editAlbum").param("access_token", token.get).param("album_id", album.id.toString()).param("title", newTitle).param("v","5.37").asString
            true
          }
          case _ => false
        }
      }
      case _ => false
    }
  }
}
