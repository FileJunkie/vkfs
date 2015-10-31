package name.filejunkie.vkfs.vk

import scalaj.http._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import name.filejunkie.vkfs.common.images._

class VkApi(userId: String) {
  val apiPrefix = "http://api.vk.com/method/"
  implicit val formats = DefaultFormats
  
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
}